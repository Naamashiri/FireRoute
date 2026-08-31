package fireroute.application;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import fireroute.domain.routing.PathFinder;
import fireroute.domain.routing.PathResult;
import fireroute.domain.routing.RouteParams;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {
    private final Graph graph;
    private final PathFinder pathFinder;

    public RoutingService(Graph graph, PathFinder pathFinder) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        if (pathFinder == null) {
            throw new IllegalArgumentException("pathFinder must not be null");
        }
        this.graph = graph;
        this.pathFinder = pathFinder;
    }

    /** Calculates a normal route between two known junctions. */
    public PathResult calculateRoute(
            String sourceId,
            String destinationId,
            RouteParams params
    ) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must be non-null and non-blank");
        }

        if (destinationId == null || destinationId.isBlank()) {
            throw new IllegalArgumentException("destinationId must be non-null and non-blank");
        }

        if (params == null) {
            throw new IllegalArgumentException("params must be non-null");
        }

        Junction source = graph.getJunction(sourceId);
        Junction destination = graph.getJunction(destinationId);

        if (source == null) {
            throw new JunctionNotFoundException(sourceId);
        }

        if (destination == null) {
            throw new JunctionNotFoundException(destinationId);
        }

        return pathFinder.findPath(source, destination, params);
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
            throw new IllegalArgumentException("sourceId must be non-null and non-blank");
        }

        if (params == null) {
            throw new IllegalArgumentException("params must be non-null");
        }

        Junction source = graph.getJunction(sourceId);

        if (source == null) {
            throw new JunctionNotFoundException(sourceId);
        }

        return pathFinder.findPathToNearestShelter(source, params);
    }
}
