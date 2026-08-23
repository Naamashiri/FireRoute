package api.mapper;
import routing.PathResult;
import java.util.List;
import api.dto.RouteResponse;
import api.dto.RoutePoint;

// PathResult --> RouteResponse
public class RouteMapper {
    PathResult pathResult;
    public RouteMapper(PathResult pathResult) {
        this.pathResult = pathResult;
    }
    public RouteResponse toRouteResponse() {
        if(pathResult.hasPath() == false) {
            return new RouteResponse(false, 0.0, List.of(), 0.0);
        }
        List<RoutePoint> routePoints = pathResult.getPath().stream()
                .map(junction -> new RoutePoint(junction.getId(), junction.getX(), junction.getY()))
                .toList();

        return new RouteResponse(
                pathResult.hasPath(),
                pathResult.getTotalTime(),
                routePoints,
                pathResult.getMaxMinutesToShelter()
        );
    }


}
