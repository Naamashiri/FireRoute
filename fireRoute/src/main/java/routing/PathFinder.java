package routing;

import risk.RiskEvaluator;
import graph.Graph;
import graph.Junction;
import graph.RoadSegment;

import java.util.*;

/**
 * Finds legal paths under the shelter constraint and dynamic war risks.
 *
 * A path is legal only if every visited junction has a shelter reachable
 * within params.getMaxShelterMinutes().
 *
 * Edge cost:
 * travelTime + fearFactor * dynamicRisk
 */
public class PathFinder {

    private final Graph graph;
    private final ShelterMap shelterMap;
    private final RiskEvaluator riskEvaluator;

    public PathFinder(Graph graph, RiskEvaluator riskEvaluator) {
        if (graph == null || riskEvaluator == null) {
            throw new IllegalArgumentException("graph and riskEvaluator must be non-null");
        }
        this.graph = graph;
        this.riskEvaluator = riskEvaluator;
        this.shelterMap = new ShelterMap(graph);
        this.shelterMap.compute();
    }

    private static class State {
        final Junction junction;
        final double costFromStart;

        State(Junction junction, double costFromStart) {
            this.junction = junction;
            this.costFromStart = costFromStart;
        }
    }

    public PathResult findPath(Junction start, Junction goal, RouteParams params) {
        if (start == null || goal == null || params == null) {
            throw new IllegalArgumentException("start, goal and params must be non-null");
        }

        if (!isShelterReachableInTime(start, params) || !isShelterReachableInTime(goal, params)) {
            return PathResult.noPath();
        }

        Map<Junction, Double> dist = new HashMap<>();
        Map<Junction, Junction> previous = new HashMap<>();
        Map<Junction, RoadSegment> previousSegment = new HashMap<>();
        Set<Junction> settledNodes = new HashSet<>();

        PriorityQueue<State> pq =
                new PriorityQueue<>(Comparator.comparingDouble(s -> s.costFromStart));

        for (Junction j : graph.getJunctions()) {
            dist.put(j, Double.POSITIVE_INFINITY);
        }

        dist.put(start, 0.0);
        pq.add(new State(start, 0.0));

        while (!pq.isEmpty()) {
            State current = pq.poll();
            Junction u = current.junction;

            if (settledNodes.contains(u)) {
                continue;
            }
            settledNodes.add(u);

            if (u == goal) {
                break;
            }

            for (RoadSegment segment : u.getOutGoingRoads()) {
                Junction v = segment.getTargetJunction();

                if (settledNodes.contains(v)) {
                    continue;
                }

                if (!isShelterReachableInTime(v, params)) {
                    continue;
                }

                double segmentRisk = riskEvaluator.getSegmentRisk(u, v);
                double edgeCost = params.getPaceMultiplier()
                        * (segment.getTravelTime() + params.getFearFactor() * segmentRisk);
                double newCost = dist.get(u) + edgeCost;

                if (newCost < dist.get(v)) {
                    dist.put(v, newCost);
                    previous.put(v, u);
                    previousSegment.put(v, segment);
                    pq.add(new State(v, newCost));
                }
            }
        }

        if (dist.get(goal) == Double.POSITIVE_INFINITY) {
            return PathResult.noPath();
        }

        return buildPathResult(start, goal, previous, previousSegment, params);
    }

    /**
     * Emergency mode: shortest path to nearest shelter (precomputed by ShelterMap).
     * This favors speed to shelter, not a new global optimization.
     *
     * Note: the chosen route itself does not depend on fearFactor.
     * fearFactor only affects the reported totalCost.
     */
    public PathResult findPathToNearestShelter(Junction start, RouteParams params) {
        if (start == null || params == null) {
            throw new IllegalArgumentException("start and params must be non-null");
        }

        Junction shelter = shelterMap.getNearestShelter(start);
        if (shelter == null) {
            return PathResult.noPath();
        }

        List<Junction> path = new ArrayList<>();
        double totalTime = 0.0;
        double totalCost = 0.0;
        double maxRisk = 0.0;

        Junction current = start;
        path.add(current);

        while (current != shelter) {
            Junction next = shelterMap.getNextStepToShelter(current);
            if (next == null) {
                return PathResult.noPath();
            }

            RoadSegment segment = findSegment(current, next);
            if (segment == null) {
                return PathResult.noPath();
            }

            double segmentRisk = riskEvaluator.getSegmentRisk(current, next);

            totalTime += params.getPaceMultiplier() * segment.getTravelTime();
            totalCost += params.getPaceMultiplier()
                    * (segment.getTravelTime() + params.getFearFactor() * segmentRisk);
            maxRisk = Math.max(maxRisk, segmentRisk);

            current = next;
            path.add(current);
        }

        return new PathResult(path, totalTime, totalCost, maxRisk);
    }

    private boolean isShelterReachableInTime(Junction junction, RouteParams params) {
        double actualMinutes = params.getPaceMultiplier() * shelterMap.getDistanceToShelter(junction);
        return actualMinutes <= params.getMaxShelterMinutes();
    }

    private RoadSegment findSegment(Junction from, Junction to) {
        for (RoadSegment segment : from.getOutGoingRoads()) {
            if (segment.getTargetJunction().equals(to)) {
                return segment;
            }
        }
        return null;
    }

    private PathResult buildPathResult(Junction start,
                                       Junction goal,
                                       Map<Junction, Junction> previous,
                                       Map<Junction, RoadSegment> previousSegment,
                                       RouteParams params) {

        List<Junction> reversedPath = new ArrayList<>();
        double totalTime = 0.0;
        double totalCost = 0.0;
        double maxRisk = 0.0;

        Junction current = goal;

        while (current != null && current != start) {
            reversedPath.add(current);

            RoadSegment seg = previousSegment.get(current);
            if (seg == null) {
                return PathResult.noPath();
            }

            Junction prev = previous.get(current);
            if (prev == null) {
                return PathResult.noPath();
            }

            double segmentRisk = riskEvaluator.getSegmentRisk(prev, current);

            totalTime += params.getPaceMultiplier() * seg.getTravelTime();
            totalCost += params.getPaceMultiplier()
                    * (seg.getTravelTime() + params.getFearFactor() * segmentRisk);
            maxRisk = Math.max(maxRisk, segmentRisk);

            current = prev;
        }

        if (current != start) {
            return PathResult.noPath();
        }

        reversedPath.add(start);
        Collections.reverse(reversedPath);

        return new PathResult(reversedPath, totalTime, totalCost, maxRisk);
    }

    public ShelterMap getShelterMap() {
        return shelterMap;
    }
}