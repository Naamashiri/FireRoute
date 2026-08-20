package engine;

import graph.Graph;
import graph.Junction;
import routing.PathFinder;
import routing.PathResult;
import routing.RouteParams;

/**
 * Core routing engine.
 *
 * Responsible for:
 * - resolving junctions from the graph
 * - validating route inputs
 * - delegating path finding to PathFinder
 *
 * It does NOT implement Dijkstra itself.
 */
public class FireRouteEngine {

    private final Graph graph;
    private final PathFinder pathFinder;

    public FireRouteEngine(
            Graph graph,
            PathFinder pathFinder
    ) {
        if (graph == null || pathFinder == null) {
            throw new IllegalArgumentException(
                    "graph and pathFinder must be non-null"
            );
        }

        this.graph = graph;
        this.pathFinder = pathFinder;
    }

    /**
     * Calculates a normal route from source to destination.
     */
    public PathResult calculateRoute(
            String sourceId,
            String destinationId,
            RouteParams params
    ) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceId must be non-null and non-blank"
            );
        }

        if (destinationId == null || destinationId.isBlank()) {
            throw new IllegalArgumentException(
                    "destinationId must be non-null and non-blank"
            );
        }

        if (params == null) {
            throw new IllegalArgumentException(
                    "params must be non-null"
            );
        }

        Junction source =
                graph.getJunction(sourceId);

        Junction destination =
                graph.getJunction(destinationId);

        if (source == null) {
            throw new IllegalArgumentException(
                    "Unknown source junction: " + sourceId
            );
        }

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Unknown destination junction: " + destinationId
            );
        }

        return pathFinder.findPath(
                source,
                destination,
                params
        );
    }

    /**
     * Emergency mode:
     * calculates a route from the source to the nearest shelter.
     */
    public PathResult calculateEmergencyRoute(
            String sourceId,
            RouteParams params
    ) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceId must be non-null and non-blank"
            );
        }

        if (params == null) {
            throw new IllegalArgumentException(
                    "params must be non-null"
            );
        }

        Junction source =
                graph.getJunction(sourceId);

        if (source == null) {
            throw new IllegalArgumentException(
                    "Unknown source junction: " + sourceId
            );
        }

        return pathFinder.findPathToNearestShelter(
                source,
                params
        );
    }
}