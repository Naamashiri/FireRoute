(function (global) {
  'use strict';

  const AVERAGE_WALKING_SPEED = 5.0;
  const WALKING_SPEEDS = { SLOW: 3.5, AVERAGE: 5.0, FAST: 6.0 };
  const SAFE_MINUTES_TO_SHELTER = 1.5;
  const MAX_SNAP_DISTANCE_METERS = 250;

  class MinHeap {
    constructor() { this.items = []; }

    push(value) {
      this.items.push(value);
      let index = this.items.length - 1;
      while (index > 0) {
        const parent = Math.floor((index - 1) / 2);
        if (this.items[parent].cost <= value.cost) break;
        this.items[index] = this.items[parent];
        index = parent;
      }
      this.items[index] = value;
    }

    pop() {
      if (!this.items.length) return null;
      const root = this.items[0];
      const last = this.items.pop();
      if (!this.items.length) return root;

      let index = 0;
      while (true) {
        const left = index * 2 + 1;
        const right = left + 1;
        if (left >= this.items.length) break;
        const child = right < this.items.length &&
          this.items[right].cost < this.items[left].cost ? right : left;
        if (this.items[child].cost >= last.cost) break;
        this.items[index] = this.items[child];
        index = child;
      }
      this.items[index] = last;
      return root;
    }

    get size() { return this.items.length; }
  }

  class OfflineRouter {
    constructor(mapUrl, sheltersUrl) {
      this.mapUrl = mapUrl;
      this.sheltersUrl = sheltersUrl;
      this.nodes = new Map();
      this.outgoing = new Map();
      this.shelterDistances = new Map();
      this.shelters = [];
      this.ready = this.load();
    }

    async load() {
      const [mapResponse, sheltersResponse] = await Promise.all([
        fetch(this.mapUrl),
        fetch(this.sheltersUrl)
      ]);
      if (!mapResponse.ok || !sheltersResponse.ok) {
        throw new Error('Offline emergency data could not be loaded');
      }

      const [graph, shelters] = await Promise.all([
        mapResponse.json(),
        sheltersResponse.json()
      ]);

      for (const junction of graph.junctions || []) {
        this.nodes.set(String(junction.id), junction);
        this.outgoing.set(String(junction.id), []);
      }
      for (const road of graph.roads || []) {
        const from = String(road.from);
        const to = String(road.to);
        if (this.outgoing.has(from) && this.nodes.has(to)) {
          this.outgoing.get(from).push({ to, travelTime: Number(road.travelTime) });
        }
      }

      this.shelters = (shelters.features || []).map(feature => ({
        id: String(feature.id ?? feature.properties?.oid_mitkan ?? ''),
        address: String(feature.properties?.Full_Address || '').trim(),
        longitude: Number(feature.geometry?.coordinates?.[0] ?? feature.properties?.lon),
        latitude: Number(feature.geometry?.coordinates?.[1] ?? feature.properties?.lat)
      })).filter(shelter => Number.isFinite(shelter.latitude) && Number.isFinite(shelter.longitude));

      this.computeShelterDistances();
      return this;
    }

    async handle(urlValue) {
      await this.ready;
      const url = new URL(urlValue, global.location?.origin || 'http://localhost');

      if (url.pathname === '/api/junctions/nearest') {
        return this.nearest(Number(url.searchParams.get('lat')), Number(url.searchParams.get('lon')));
      }

      const options = this.options(url.searchParams);
      if (url.pathname === '/api/routes/emergency') {
        const result = this.routeToNearestShelter(String(url.searchParams.get('sourceId')), options);
        result.routeType = 'EMERGENCY';
        result.alreadyAtShelter = result.found && result.pathPoints.length === 1;
        if (!result.found) result.failureReason = 'NO_REACHABLE_SHELTER';
        return result;
      }
      if (url.pathname === '/api/routes') {
        const result = this.route(
          String(url.searchParams.get('sourceId')),
          String(url.searchParams.get('destinationId')),
          options
        );
        result.routeType = 'NORMAL';
        result.alreadyAtShelter = false;
        return result;
      }

      throw new Error('No offline handler for ' + url.pathname);
    }

    options(searchParams) {
      const paceName = searchParams.get('walkingPace') || 'AVERAGE';
      const speed = WALKING_SPEEDS[paceName] || WALKING_SPEEDS.AVERAGE;
      return {
        paceMultiplier: AVERAGE_WALKING_SPEED / speed,
        fearFactor: this.numberOrDefault(searchParams.get('fearFactor'), 1),
        maxShelterMinutes: this.numberOrDefault(searchParams.get('maxShelterMinutes'), 7)
      };
    }

    numberOrDefault(value, fallback) {
      const parsed = Number(value);
      return value !== null && Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
    }

    nearest(latitude, longitude) {
      if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
        throw new Error('Invalid location');
      }

      const metresPerDegreeLat = 111320;
      const metresPerDegreeLon = metresPerDegreeLat * Math.cos(latitude * Math.PI / 180);
      let nearest = null;
      let bestDistance = Infinity;

      for (const junction of this.nodes.values()) {
        const dx = (Number(junction.x) - longitude) * metresPerDegreeLon;
        const dy = (Number(junction.y) - latitude) * metresPerDegreeLat;
        const distance = dx * dx + dy * dy;
        if (distance < bestDistance) {
          bestDistance = distance;
          nearest = junction;
        }
      }

      if (!nearest) throw new Error('No offline junction data');
      if (Math.sqrt(bestDistance) > MAX_SNAP_DISTANCE_METERS) {
        throw new Error('Location is outside the supported map area');
      }
      return {
        id: String(nearest.id),
        latitude: Number(nearest.y),
        longitude: Number(nearest.x),
        shelter: Boolean(nearest.isShelter)
      };
    }

    route(startId, goalId, options) {
      if (!this.nodes.has(startId) || !this.nodes.has(goalId)) {
        throw new Error('Unknown junction in offline data');
      }
      if (startId === goalId) return this.result([startId], 0, options);

      const strict = this.search(startId, goalId, options, true);
      return strict.found ? strict : this.search(startId, goalId, options, false);
    }

    routeToNearestShelter(startId, options) {
      if (!this.nodes.has(startId)) throw new Error('Unknown source junction in offline data');
      const queue = new MinHeap();
      const distances = new Map([[startId, 0]]);
      const previous = new Map();
      queue.push({ id: startId, cost: 0 });

      while (queue.size) {
        const current = queue.pop();
        if (current.cost !== distances.get(current.id)) continue;
        if (this.nodes.get(current.id).isShelter) {
          return this.result(this.buildPath(startId, current.id, previous), current.cost, options);
        }
        for (const edge of this.outgoing.get(current.id) || []) {
          const nextCost = current.cost + edge.travelTime * options.paceMultiplier;
          if (nextCost < (distances.get(edge.to) ?? Infinity)) {
            distances.set(edge.to, nextCost);
            previous.set(edge.to, current.id);
            queue.push({ id: edge.to, cost: nextCost });
          }
        }
      }
      return this.noPath();
    }

    search(startId, goalId, options, enforceShelterConstraint) {
      const queue = new MinHeap();
      const costs = new Map([[startId, 0]]);
      const times = new Map([[startId, 0]]);
      const previous = new Map();
      queue.push({ id: startId, cost: 0 });

      while (queue.size) {
        const current = queue.pop();
        if (current.cost !== costs.get(current.id)) continue;
        if (current.id === goalId) {
          return this.result(
            this.buildPath(startId, goalId, previous),
            times.get(goalId),
            options
          );
        }

        for (const edge of this.outgoing.get(current.id) || []) {
          const rawShelterDistance = this.shelterDistances.get(edge.to) ?? Infinity;
          const minutesToShelter = options.paceMultiplier * rawShelterDistance;
          if (enforceShelterConstraint && edge.to !== goalId &&
              minutesToShelter > options.maxShelterMinutes) continue;

          const walkingMinutes = edge.travelTime * options.paceMultiplier;
          // In the relaxed pass, disconnected shelter-distance data must remain
          // routable. Penalise it at the configured limit instead of producing
          // an infinite edge cost.
          const exposureMinutes = Number.isFinite(minutesToShelter)
            ? minutesToShelter : options.maxShelterMinutes;
          const exposure = Math.max(0, exposureMinutes - SAFE_MINUTES_TO_SHELTER);
          const edgeCost = walkingMinutes + options.fearFactor * exposure;
          const nextCost = current.cost + edgeCost;

          if (nextCost < (costs.get(edge.to) ?? Infinity)) {
            costs.set(edge.to, nextCost);
            times.set(edge.to, times.get(current.id) + walkingMinutes);
            previous.set(edge.to, current.id);
            queue.push({ id: edge.to, cost: nextCost });
          }
        }
      }
      return this.noPath();
    }

    buildPath(startId, goalId, previous) {
      const path = [];
      let current = goalId;
      while (current !== startId) {
        path.push(current);
        current = previous.get(current);
        if (current === undefined) return [];
      }
      path.push(startId);
      path.reverse();
      return path;
    }

    result(pathIds, totalTravelTime, options) {
      if (!pathIds.length) return this.noPath();
      let maximumExposure = 0;
      for (const id of pathIds) {
        maximumExposure = Math.max(
          maximumExposure,
          options.paceMultiplier * (this.shelterDistances.get(id) ?? Infinity)
        );
      }
      return {
        found: true,
        failureReason: 'NONE',
        totalTravelTime,
        pathPoints: pathIds.map(id => {
          const node = this.nodes.get(id);
          return { id, latitude: Number(node.y), longitude: Number(node.x) };
        }),
        maxMinutesToShelter: maximumExposure,
        shelterConstraintSatisfied: maximumExposure <= options.maxShelterMinutes,
        offline: true
      };
    }

    noPath() {
      return {
        found: false,
        failureReason: 'NO_ROUTE_EXISTS',
        totalTravelTime: 0,
        pathPoints: [],
        maxMinutesToShelter: 0,
        shelterConstraintSatisfied: false,
        offline: true
      };
    }

    computeShelterDistances() {
      const reversed = new Map();
      for (const id of this.nodes.keys()) reversed.set(id, []);
      for (const [from, edges] of this.outgoing.entries()) {
        for (const edge of edges) reversed.get(edge.to).push({ to: from, travelTime: edge.travelTime });
      }

      const queue = new MinHeap();
      for (const [id, node] of this.nodes.entries()) {
        const distance = node.isShelter ? 0 : Infinity;
        this.shelterDistances.set(id, distance);
        if (node.isShelter) queue.push({ id, cost: 0 });
      }

      while (queue.size) {
        const current = queue.pop();
        if (current.cost !== this.shelterDistances.get(current.id)) continue;
        for (const edge of reversed.get(current.id) || []) {
          const nextDistance = current.cost + edge.travelTime;
          if (nextDistance < this.shelterDistances.get(edge.to)) {
            this.shelterDistances.set(edge.to, nextDistance);
            queue.push({ id: edge.to, cost: nextDistance });
          }
        }
      }
    }
  }

  global.FireRouteOfflineRouter = OfflineRouter;
})(typeof window !== 'undefined' ? window : globalThis);
