package fireroute.domain.routing;

import fireroute.domain.graph.Junction;

public interface PathFinder {

    PathResult findPath(
            Junction start,
            Junction goal,
            RouteParams params
    );

    PathResult findPathToNearestShelter(
            Junction start,
            RouteParams params
    );
}