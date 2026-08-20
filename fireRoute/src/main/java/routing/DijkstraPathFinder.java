package routing;

import graph.Graph;
import graph.Junction;
import graph.RoadSegment;
import risk.RiskEvaluator;

import java.util.*;

/**
 * Dijkstra-based implementation of PathFinder.
 *
 * Finds legal paths under shelter constraints.
 * Edge cost is calculated by RouteCostCalculator.
 */
public class DijkstraPathFinder implements PathFinder {

    private final Graph graph;
    private final ShelterMap shelterMap;
    private final RiskEvaluator riskEvaluator;
    private final RouteCostCalculator routeCostCalculator;

    public DijkstraPathFinder(
            Graph graph,
            RiskEvaluator riskEvaluator,
            RouteCostCalculator routeCostCalculator
    ) {
        if (graph == null
                || riskEvaluator == null
                || routeCostCalculator == null) {

            throw new IllegalArgumentException(
                    "graph, riskEvaluator and routeCostCalculator must be non-null"
            );
        }

        this.graph = graph;
        this.riskEvaluator = riskEvaluator;
        this.routeCostCalculator = routeCostCalculator;

        this.shelterMap = new ShelterMap(graph);
        this.shelterMap.compute();
    }

    /**
     * Internal helper used by Dijkstra's priority queue.
     *
     * Represents:
     * junction + current best known cost from start.
     */
    private static class State {

        final Junction junction;
        final double costFromStart;

        State(
                Junction junction,
                double costFromStart
        ) {
            this.junction = junction;
            this.costFromStart = costFromStart;
        }
    }

    @Override
    public PathResult findPath(
            Junction start,
            Junction goal,
            RouteParams params
    ) {

        if (start == null
                || goal == null
                || params == null) {

            throw new IllegalArgumentException(
                    "start, goal and params must be non-null"
            );
        }

        /*
         * Start and goal themselves must satisfy
         * the shelter constraint.
         */
        if (!isShelterReachableInTime(start, params)
                || !isShelterReachableInTime(goal, params)) {

            return PathResult.noPath();
        }

        /*
         * dist[junction] =
         * best known cost from start to junction
         */
        Map<Junction, Double> dist =
                new HashMap<>();

        /*
         * Used later to reconstruct the path.
         */
        Map<Junction, Junction> previous =
                new HashMap<>();

        Map<Junction, RoadSegment> previousSegment =
                new HashMap<>();

        /*
         * Junctions whose minimum cost is already final.
         */
        Set<Junction> settledNodes =
                new HashSet<>();

        /*
         * Always returns the State with the smallest
         * costFromStart first.
         */
        PriorityQueue<State> pq =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                state -> state.costFromStart
                        )
                );

        /*
         * Initially every node is unreachable.
         */
        for (Junction junction : graph.getJunctions()) {
            dist.put(
                    junction,
                    Double.POSITIVE_INFINITY
            );
        }

        /*
         * Distance from start to itself = 0.
         */
        dist.put(start, 0.0);

        pq.add(
                new State(
                        start,
                        0.0
                )
        );

        /*
         * Dijkstra
         */
        while (!pq.isEmpty()) {

            State current =
                    pq.poll();

            Junction u =
                    current.junction;

            /*
             * A junction may appear in the queue multiple times.
             * Once settled, ignore older entries.
             */
            if (settledNodes.contains(u)) {
                continue;
            }

            settledNodes.add(u);

            /*
             * Goal reached with minimum cost.
             */
            if (u.equals(goal)) {
                break;
            }

            /*
             * Relax all outgoing edges.
             */
            for (RoadSegment segment : u.getOutGoingRoads()) {

                Junction v =
                        segment.getTargetJunction();

                if (settledNodes.contains(v)) {
                    continue;
                }

                /*
                 * Do not enter a junction that violates
                 * the shelter constraint.
                 */
                if (!isShelterReachableInTime(v, params)) {
                    continue;
                }

                /*
                 * RouteCostCalculator owns the cost formula.
                 */
                double edgeCost =
                        routeCostCalculator.calculateCost(
                                segment,
                                params
                        );

                double newCost =
                        dist.get(u) + edgeCost;

                /*
                 * Dijkstra relaxation.
                 */
                if (newCost < dist.get(v)) {

                    dist.put(
                            v,
                            newCost
                    );

                    previous.put(
                            v,
                            u
                    );

                    previousSegment.put(
                            v,
                            segment
                    );

                    pq.add(
                            new State(
                                    v,
                                    newCost
                            )
                    );
                }
            }
        }

        /*
         * Goal was never reached.
         */
        if (dist.get(goal)
                == Double.POSITIVE_INFINITY) {

            return PathResult.noPath();
        }

        return buildPathResult(
                start,
                goal,
                previous,
                previousSegment,
                params
        );
    }

    /**
     * Emergency routing:
     * follows the precomputed route to the nearest shelter.
     */
    @Override
    public PathResult findPathToNearestShelter(
            Junction start,
            RouteParams params
    ) {

        if (start == null || params == null) {

            throw new IllegalArgumentException(
                    "start and params must be non-null"
            );
        }

        Junction shelter =
                shelterMap.getNearestShelter(start);

        if (shelter == null) {
            return PathResult.noPath();
        }

        List<Junction> path =
                new ArrayList<>();

        double totalTime = 0.0;
        double totalCost = 0.0;
        double maxRisk = 0.0;

        Junction current = start;

        path.add(current);

        while (!current.equals(shelter)) {

            Junction next =
                    shelterMap.getNextStepToShelter(
                            current
                    );

            if (next == null) {
                return PathResult.noPath();
            }

            RoadSegment segment =
                    findSegment(
                            current,
                            next
                    );

            if (segment == null) {
                return PathResult.noPath();
            }

            /*
             * Actual walking time.
             */
            totalTime +=
                    params.getPaceMultiplier()
                            * segment.getTravelTime();

            /*
             * Full route cost.
             */
            totalCost +=
                    routeCostCalculator.calculateCost(
                            segment,
                            params
                    );

            /*
             * Risk is kept separately for PathResult.
             */
            double segmentRisk =
                    riskEvaluator.getSegmentRisk(
                            segment
                    );

            maxRisk =
                    Math.max(
                            maxRisk,
                            segmentRisk
                    );

            current = next;
            path.add(current);
        }

        return new PathResult(
                path,
                totalTime,
                totalCost,
                maxRisk
        );
    }

    /**
     * Checks whether a shelter can be reached from a junction
     * within the user's maximum allowed shelter time.
     */
    private boolean isShelterReachableInTime(
            Junction junction,
            RouteParams params
    ) {

        double baseMinutes =
                shelterMap.getDistanceToShelter(
                        junction
                );

        double actualMinutes =
                params.getPaceMultiplier()
                        * baseMinutes;

        return actualMinutes
                <= params.getMaxShelterMinutes();
    }

    /**
     * Finds a concrete RoadSegment between two adjacent junctions.
     */
    private RoadSegment findSegment(
            Junction from,
            Junction to
    ) {

        for (RoadSegment segment : from.getOutGoingRoads()) {

            if (segment
                    .getTargetJunction()
                    .equals(to)) {

                return segment;
            }
        }

        return null;
    }

    /**
     * Reconstructs the path after Dijkstra finishes.
     */
    private PathResult buildPathResult(
            Junction start,
            Junction goal,
            Map<Junction, Junction> previous,
            Map<Junction, RoadSegment> previousSegment,
            RouteParams params
    ) {

        List<Junction> reversedPath =
                new ArrayList<>();

        double totalTime = 0.0;
        double totalCost = 0.0;
        double maxRisk = 0.0;

        Junction current =
                goal;

        while (current != null
                && !current.equals(start)) {

            reversedPath.add(current);

            RoadSegment segment =
                    previousSegment.get(current);

            if (segment == null) {
                return PathResult.noPath();
            }

            Junction prev =
                    previous.get(current);

            if (prev == null) {
                return PathResult.noPath();
            }

            /*
             * Walking time of this segment.
             */
            totalTime +=
                    params.getPaceMultiplier()
                            * segment.getTravelTime();

            /*
             * Full weighted cost.
             */
            totalCost +=
                    routeCostCalculator.calculateCost(
                            segment,
                            params
                    );

            /*
             * Track maximum risk encountered.
             */
            double segmentRisk =
                    riskEvaluator.getSegmentRisk(
                            segment
                    );

            maxRisk =
                    Math.max(
                            maxRisk,
                            segmentRisk
                    );

            current =
                    prev;
        }

        if (current == null
                || !current.equals(start)) {

            return PathResult.noPath();
        }

        reversedPath.add(start);

        Collections.reverse(
                reversedPath
        );

        return new PathResult(
                reversedPath,
                totalTime,
                totalCost,
                maxRisk
        );
    }

    public ShelterMap getShelterMap() {
        return shelterMap;
    }
}