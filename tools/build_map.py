"""
Builds map.json for FireRoute from OpenStreetMap.

An offline preprocessing step, not part of the service: it runs on a developer
machine, writes a JSON file, and the file is what gets committed. The Java side
only ever reads that file, so the server carries no OSM parsing at runtime.

Usage:
    pip install osmnx
    python tools/build_map.py
"""

import json
import math
from pathlib import Path

import networkx as nx
import numpy as np
import osmnx as ox

# --- what to build -----------------------------------------------------------

# "תל אביב - מרכז העיר" — the Home Front Command alert area this service covers.
# Centred between Rothschild, Nahalat Binyamin and Habima, which is the heart of
# that area, so the graph and the municipal shelter data cover the same ground.
CENTER = (32.0680, 34.7740)   # (latitude, longitude)
RADIUS_METERS = 1800

# Average walking speed, matching WalkingPace.AVERAGE on the Java side.
# travelTime is stored at this pace; RouteParams.getPaceMultiplier() adjusts it
# per user, so it must NOT be baked in per-pace here.
WALKING_SPEED_KMH = 5.0

# A shelter further than this from any junction is reported: it usually means the
# radius is too small and the shelter sits outside the graph entirely.
MAX_SNAP_METERS = 250

REPO_ROOT = Path(__file__).resolve().parent.parent
RESOURCES = REPO_ROOT / "fireRoute" / "src" / "main" / "resources"
SHELTERS_IN = RESOURCES / "shelters.json"
MAP_OUT = RESOURCES / "map.json"


def load_shelter_points(path):
    """Longitude/latitude of every shelter, read the same way ShelterLoader does."""
    raw = json.loads(path.read_text(encoding="utf-8"))
    items = raw["features"] if isinstance(raw, dict) and "features" in raw else raw

    points = []
    for item in items:
        props = item.get("properties", item)
        geometry = item.get("geometry") or {}
        coords = geometry.get("coordinates")

        if coords:
            lon, lat = float(coords[0]), float(coords[1])
        elif props.get("lat") is not None and props.get("lon") is not None:
            lon, lat = float(props["lon"]), float(props["lat"])
        else:
            continue

        points.append((lon, lat))

    return points


def nearest_node_indices(node_lons, node_lats, shelter_points):
    """
    Index of the closest junction for each shelter, plus the distance in metres.

    An equirectangular approximation rather than haversine: over a few kilometres
    the error is centimetres, and this stays a single vectorised pass instead of
    374 x N trigonometric calls.
    """
    mean_lat_rad = math.radians(float(np.mean(node_lats)))
    metres_per_deg_lat = 111_320.0
    metres_per_deg_lon = metres_per_deg_lat * math.cos(mean_lat_rad)

    node_x = node_lons * metres_per_deg_lon
    node_y = node_lats * metres_per_deg_lat

    indices, distances = [], []
    for lon, lat in shelter_points:
        dx = node_x - lon * metres_per_deg_lon
        dy = node_y - lat * metres_per_deg_lat
        squared = dx * dx + dy * dy

        best = int(np.argmin(squared))
        indices.append(best)
        distances.append(math.sqrt(float(squared[best])))

    return indices, distances


def main():
    print(f"Downloading walk network: {RADIUS_METERS} m around {CENTER} ...")

    # network_type="walk" gives footways, crossings and pedestrian paths, and
    # ignores one-way restrictions, which apply to vehicles rather than people.
    graph = ox.graph_from_point(CENTER, dist=RADIUS_METERS, network_type="walk")

    # Keep only the largest connected component. Stray fragments would produce
    # junctions that no route can ever reach, which reads as a routing bug.
    largest = max(nx.weakly_connected_components(graph), key=len)
    graph = graph.subgraph(largest).copy()

    node_ids = list(graph.nodes)
    node_lons = np.array([graph.nodes[n]["x"] for n in node_ids], dtype=float)
    node_lats = np.array([graph.nodes[n]["y"] for n in node_ids], dtype=float)
    print(f"  {len(node_ids)} junctions, {graph.number_of_edges()} raw edges")

    # --- mark shelters -------------------------------------------------------
    # isShelter is derived from shelters.json rather than declared in map.json, so
    # the shelter repository stays the single source of truth and the two notions
    # of "shelter" in the codebase cannot drift apart.
    shelter_points = load_shelter_points(SHELTERS_IN)
    indices, distances = nearest_node_indices(node_lons, node_lats, shelter_points)

    shelter_node_ids = set()
    out_of_range = 0
    for index, distance in zip(indices, distances):
        if distance <= MAX_SNAP_METERS:
            shelter_node_ids.add(node_ids[index])
        else:
            out_of_range += 1

    print(f"  {len(shelter_points)} shelters -> {len(shelter_node_ids)} junctions marked"
          f" ({out_of_range} outside the radius, skipped)")

    # --- build the payload ---------------------------------------------------
    junctions = [
        {
            "id": str(node_id),
            "x": round(float(graph.nodes[node_id]["x"]), 7),   # longitude
            "y": round(float(graph.nodes[node_id]["y"]), 7),   # latitude
            "isShelter": node_id in shelter_node_ids,
        }
        for node_id in node_ids
    ]

    metres_per_minute = WALKING_SPEED_KMH * 1000.0 / 60.0

    # Both directions: Graph.addRoadSegment is directed, but a pedestrian can walk
    # either way down any of these. Parallel edges collapse to the shortest.
    shortest = {}
    for u, v, data in graph.edges(data=True):
        length = float(data.get("length", 0.0))
        if length <= 0:
            continue
        for a, b in ((str(u), str(v)), (str(v), str(u))):
            if length < shortest.get((a, b), math.inf):
                shortest[(a, b)] = length

    roads = [
        {
            "from": a,
            "to": b,
            "travelTime": round(length / metres_per_minute, 4),
        }
        for (a, b), length in shortest.items()
    ]

    MAP_OUT.write_text(
        json.dumps({"junctions": junctions, "roads": roads}, ensure_ascii=False),
        encoding="utf-8",
    )

    size_mb = MAP_OUT.stat().st_size / (1024 * 1024)
    print(f"Wrote {MAP_OUT}")
    print(f"  {len(junctions)} junctions, {len(roads)} directed roads, {size_mb:.1f} MB")


if __name__ == "__main__":
    main()