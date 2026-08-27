package fireroute.routing;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import fireroute.domain.graph.RoadSegment;

import java.util.*;

/**
 * Dijkstra-based implementation of PathFinder.
 * Finds legal paths under shelter constraints with edge cost calculated by RouteCostCalculator.
 */
public class DijkstraPathFinder implements PathFinder {

    private final Graph graph;
    private final ShelterMap shelterMap;
    private final RouteCostCalculator routeCostCalculator;

    /**
     * ShelterMap is injected rather than built here: the cost calculator measures
     * exposure with the same map, and computing it twice would both waste the
     * startup work and risk the two drifting apart.
     */
    public DijkstraPathFinder(
            Graph graph,
            ShelterMap shelterMap,
            RouteCostCalculator routeCostCalculator
    ) {
        if (graph == null || shelterMap == null || routeCostCalculator == null) {
            throw new IllegalArgumentException("graph, shelterMap and routeCostCalculator must be non-null");
        }

        this.graph = graph;
        this.shelterMap = shelterMap;
        this.routeCostCalculator = routeCostCalculator;
    }

    private static class State {
        final Junction junction;
        final double costFromStart;

        State(Junction junction, double costFromStart) {
            this.junction = junction;
            this.costFromStart = costFromStart;
        }
    }

    @Override
    public PathResult findPath(Junction start, Junction goal, RouteParams params) {
        if (start == null || goal == null || params == null) {
            throw new IllegalArgumentException("start, goal and params must be non-null");
        }

        if (start.equals(goal)) {
            // The user does not move, but is still some distance away from a shelter.
            double minutes = minutesToShelter(start, params);
            return new PathResult(
                    List.of(start),
                    0.0,
                    0.0,
                    minutes,
                    minutes <= params.getMaxShelterMinutes()
            );
        }

        /*
         * Two passes.
         *
         * The first refuses to route through any junction further from cover
         * than the request allows. That is the whole point of the constraint:
         * given a choice, take the sheltered way even if it is longer.
         *
         * The second drops the filter and searches again. It exists because
         * "no route" is the wrong answer to give someone standing in an exposed
         * street during an alert — they did not choose where they are, and a
         * walk they can judge for themselves beats no walk at all. The result
         * reports shelterConstraintSatisfied so the caller warns rather than
         * presenting an exposed route as a safe one.
         *
         * Endpoints are never filtered in either pass. Which junctions a route
         * may pass through is a routing decision; where the user happens to be
         * standing is not.
         */
        PathResult strict = search(start, goal, params, true);
        if (strict.hasPath()) {
            return strict;
        }

        return search(start, goal, params, false);
    }

    private PathResult search(
            Junction start,
            Junction goal,
            RouteParams params,
            boolean enforceShelterConstraint
    ) {
        Map<Junction, Double> dist = new HashMap<>();
        Map<Junction, Junction> previous = new HashMap<>();
        Map<Junction, RoadSegment> previousSegment = new HashMap<>();
        Set<Junction> settledNodes = new HashSet<>();

        PriorityQueue<State> pq = new PriorityQueue<>(
                Comparator.comparingDouble(state -> state.costFromStart)
        );

        for (Junction junction : graph.getJunctions()) {
            dist.put(junction, Double.POSITIVE_INFINITY);
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

            if (u.equals(goal)) {
                break;
            }

            for (RoadSegment segment : u.getOutGoingRoads()) {
                Junction v = segment.getTargetJunction();

                if (settledNodes.contains(v)) {
                    continue;
                }

                if (enforceShelterConstraint && !isShelterReachableInTime(v, params)) {
                    continue;
                }

                double edgeCost = routeCostCalculator.calculateCost(segment, params);
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

    @Override
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
        double maxMinutesToShelter = 0.0;

        Junction current = start;
        path.add(current);

        while (!current.equals(shelter)) {
            Junction next = shelterMap.getNextStepToShelter(current);
            if (next == null) {
                return PathResult.noPath();
            }

            RoadSegment segment = findSegment(current, next);
            if (segment == null) {
                return PathResult.noPath();
            }

            totalTime += params.getPaceMultiplier() * segment.getTravelTime();
            totalCost += routeCostCalculator.calculateCost(segment, params);
            maxMinutesToShelter = Math.max(maxMinutesToShelter, minutesToShelter(current, params));

            current = next;
            path.add(current);
        }

        // The loop stops at the shelter itself; measure it too, so every node on the path is covered.
        maxMinutesToShelter = Math.max(maxMinutesToShelter, minutesToShelter(current, params));

        return new PathResult(
                path,
                totalTime,
                totalCost,
                maxMinutesToShelter,
                maxMinutesToShelter <= params.getMaxShelterMinutes()
        );
    }

    /**
     * Minutes from the given junction to its nearest shelter,
     * adjusted for the user's walking pace.
     *
     * Single definition on purpose: the constraint that filters junctions and
     * the figure reported to the user must be the exact same calculation.
     */
    private double minutesToShelter(Junction junction, RouteParams params) {
        return params.getPaceMultiplier() * shelterMap.getDistanceToShelter(junction);
    }

    private boolean isShelterReachableInTime(Junction junction, RouteParams params) {
        return minutesToShelter(junction, params) <= params.getMaxShelterMinutes();
    }

    private RoadSegment findSegment(Junction from, Junction to) {
        for (RoadSegment segment : from.getOutGoingRoads()) {
            if (segment.getTargetJunction().equals(to)) {
                return segment;
            }
        }
        return null;
    }

    private PathResult buildPathResult(
            Junction start,
            Junction goal,
            Map<Junction, Junction> previous,
            Map<Junction, RoadSegment> previousSegment,
            RouteParams params
    ) {
        List<Junction> reversedPath = new ArrayList<>();
        double totalTime = 0.0;
        double totalCost = 0.0;
        double maxMinutesToShelter = 0.0;

        Junction current = goal;

        while (!current.equals(start)) {
            reversedPath.add(current);

            RoadSegment segment = previousSegment.get(current);
            Junction prev = previous.get(current);

            if (segment == null || prev == null) {
                return PathResult.noPath();
            }

            totalTime += params.getPaceMultiplier() * segment.getTravelTime();
            totalCost += routeCostCalculator.calculateCost(segment, params);
            maxMinutesToShelter = Math.max(maxMinutesToShelter, minutesToShelter(current, params));

            current = prev;
        }

        reversedPath.add(start);

        // The loop stops before start; measure it too, so every node on the path is covered.
        maxMinutesToShelter = Math.max(maxMinutesToShelter, minutesToShelter(start, params));

        Collections.reverse(reversedPath);

        return new PathResult(
                reversedPath,
                totalTime,
                totalCost,
                maxMinutesToShelter,
                maxMinutesToShelter <= params.getMaxShelterMinutes()
        );
    }

    public ShelterMap getShelterMap() {
        return shelterMap;
    }
}
