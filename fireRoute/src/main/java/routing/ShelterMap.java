package routing;

import graph.Graph;
import graph.Junction;
import graph.RoadSegment;

import java.util.*;

/**
 * Computes, for every junction v:
 * 1) the minimal travel time from v to the nearest shelter
 * 2) which shelter is nearest
 * 3) what is the next junction to move to from v on the way to that shelter
 *
 * The graph may be directed, so we run multi-source Dijkstra
 * on the reversed graph.
 */
public class ShelterMap {

    private final Graph graph;

    // distToShelter.get(v) = minimal minutes from v to nearest shelter
    private final Map<Junction, Double> distToShelter = new HashMap<>();

    // nearestShelter.get(v) = the nearest shelter of v
    private final Map<Junction, Junction> nearestShelter = new HashMap<>();

    // nextStepToShelter.get(v) = the next junction to go to from v toward nearest shelter
    private final Map<Junction, Junction> nextStepToShelter = new HashMap<>();

    // reversedEdges.get(v) = all reversed edges leaving v in the reversed graph
    private final Map<Junction, List<ReversedEdge>> reversedEdges = new HashMap<>();

    private boolean isComputed = false;

    private static class State {
        final Junction node;
        final double dist;

        State(Junction node, double dist) {
            this.node = node;
            this.dist = dist;
        }
    }

    /**
     * Original edge: u -> v
     * Reversed edge: v -> u
     */
    private static class ReversedEdge {
        final Junction to;       // in reversed graph: current -> to
        final double travelTime;

        ReversedEdge(Junction to, double travelTime) {
            this.to = to;
            this.travelTime = travelTime;
        }
    }

    public ShelterMap(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must be non-null");
        }
        this.graph = graph;
    }

    /**
     * Builds the reversed graph adjacency lists.
     */
    private void buildReversedEdges() {
        reversedEdges.clear();

        for (Junction j : graph.getJunctions()) {
            reversedEdges.put(j, new ArrayList<>());
        }

        // original: u -> v
        // reversed: v -> u
        for (Junction u : graph.getJunctions()) {
            for (RoadSegment seg : u.getOutGoingRoads()) {
                Junction v = seg.getTargetJunction();
                reversedEdges.get(v).add(new ReversedEdge(u, seg.getTravelTime()));
            }
        }
    }

    /**
     * Runs multi-source Dijkstra from all shelters on the reversed graph.
     *
     * After compute():
     * - distToShelter[v] = shortest time from v to nearest shelter
     * - nearestShelter[v] = nearest shelter of v
     * - nextStepToShelter[v] = next node on shortest path from v to that shelter
     */
    public void compute() {
        isComputed = false;

        distToShelter.clear();
        nearestShelter.clear();
        nextStepToShelter.clear();
        buildReversedEdges();

        for (Junction j : graph.getJunctions()) {
            distToShelter.put(j, Double.POSITIVE_INFINITY);
        }

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingDouble(s -> s.dist));

        // Start from all shelters
        for (Junction j : graph.getJunctions()) {
            if (j.isShelter()) {
                distToShelter.put(j, 0.0);
                nearestShelter.put(j, j);
                pq.add(new State(j, 0.0));
            }
        }

        // No shelters in graph
        if (pq.isEmpty()) {
            isComputed = true;
            return;
        }

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            Junction v = cur.node;

            // outdated queue entry
            if (cur.dist > distToShelter.get(v)) {
                continue;
            }

            for (ReversedEdge edge : reversedEdges.get(v)) {
                Junction u = edge.to;
                double newDist = cur.dist + edge.travelTime;

                if (newDist < distToShelter.get(u)) {
                    distToShelter.put(u, newDist);
                    nearestShelter.put(u, nearestShelter.get(v));

                    // In the original graph, from u the next step is v
                    nextStepToShelter.put(u, v);

                    pq.add(new State(u, newDist));
                }
            }
        }

        isComputed = true;
    }

    private void ensureComputed() {
        if (!isComputed) {
            throw new IllegalStateException("ShelterMap was not computed yet. Call compute() first.");
        }
    }

    /**
     * @return minimal time from junction to nearest shelter,
     * or +infinity if unreachable.
     */
    public double getDistanceToShelter(Junction junction) {
        if (junction == null) {
            throw new IllegalArgumentException("junction must be non-null");
        }
        ensureComputed();
        return distToShelter.getOrDefault(junction, Double.POSITIVE_INFINITY);
    }

    /**
     * @return nearest shelter of the given junction,
     * or null if unreachable.
     */
    public Junction getNearestShelter(Junction junction) {
        if (junction == null) {
            throw new IllegalArgumentException("junction must be non-null");
        }
        ensureComputed();
        return nearestShelter.get(junction);
    }

    /**
     * @return the next junction to move to from the given junction
     * on the shortest path to the nearest shelter,
     * or null if unreachable / already at shelter.
     */
    public Junction getNextStepToShelter(Junction junction) {
        if (junction == null) {
            throw new IllegalArgumentException("junction must be non-null");
        }
        ensureComputed();
        return nextStepToShelter.get(junction);
    }

    /**
     * @return read-only view of distances to nearest shelter.
     */
    public Map<Junction, Double> getDistToShelterMap() {
        ensureComputed();
        return Collections.unmodifiableMap(distToShelter);
    }
}