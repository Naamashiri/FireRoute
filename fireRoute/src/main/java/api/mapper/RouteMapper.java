package api.mapper;

import api.dto.RoutePoint;
import api.dto.RouteRequest;
import api.dto.RouteResponse;
import api.dto.WalkingPaceDto;
import routing.PathResult;
import routing.RouteParams;
import routing.WalkingPace;

import java.util.List;

/**
 * Translates between the API layer and the routing domain.
 *
 * Stateless on purpose: what is being converted arrives as a parameter, never
 * as constructor state, so a single instance can serve every request. The
 * constructor stays free for real dependencies, which is where a
 * ShelterRepository will go once shelter identity is available.
 *
 * It lives in the api layer because that layer already depends on the domain;
 * putting it here keeps the dependency arrow pointing one way.
 */
public class RouteMapper {

    public RouteResponse toRouteResponse(PathResult pathResult) {
        if (pathResult == null) {
            throw new IllegalArgumentException("pathResult must not be null");
        }

        if (!pathResult.hasPath()) {
            return new RouteResponse(false, 0.0, List.of(), 0.0);
        }

        List<RoutePoint> points = pathResult.getPath().stream()
                // Junction stores x as longitude and y as latitude (see GeoPoint),
                // so y comes first here.
                .map(junction -> new RoutePoint(
                        junction.getId(),
                        junction.getY(),
                        junction.getX()
                ))
                .toList();

        return new RouteResponse(
                true,
                pathResult.getTotalTime(),
                points,
                pathResult.getMaxMinutesToShelter()
        );
    }

    /**
     * Builds routing parameters from a request, filling in whatever the caller
     * omitted from the routing defaults rather than from constants duplicated
     * in the API layer.
     */
    public RouteParams toRouteParams(RouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        RouteParams defaults = new RouteParams();

        double maxShelterMinutes = request.maxShelterMinutes() != null
                ? request.maxShelterMinutes()
                : defaults.getMaxShelterMinutes();

        double fearFactor = request.fearFactor() != null
                ? request.fearFactor()
                : defaults.getFearFactor();

        return new RouteParams(
                maxShelterMinutes,
                toDomainPace(request.walkingPace()),
                fearFactor
        );
    }

    private WalkingPace toDomainPace(WalkingPaceDto pace) {
        if (pace == null) {
            return WalkingPace.AVERAGE;
        }

        return switch (pace) {
            case SLOW -> WalkingPace.SLOW;
            case AVERAGE -> WalkingPace.AVERAGE;
            case FAST -> WalkingPace.FAST;
        };
    }
}
