package engine;

import graph.Graph;
import graph.Junction;
import routing.DijkstraPathFinder;
import routing.PathResult;
import routing.RouteParams;

public class FireRouteEngine {

    private final Graph graph;
    private final PathFinder pathFinder;

    public FireRouteEngine(
            Graph graph,
            PathFinder pathFinder
    ) {
        this.graph = graph;
        this.pathFinder = pathFinder;
    }

    public PathResult calculateRoute(
            String sourceId,
            String destinationId,
            RouteParams params
    ) {
        Junction source = graph.getJunction(sourceId);
        Junction destination = graph.getJunction(destinationId);

        return pathFinder.findPath(
                source,
                destination,
                params
        );
    }
}
