package fireroute.api.mapper;

import fireroute.api.dto.RoutePoint;
import fireroute.api.dto.RouteOptions;
import fireroute.api.dto.RouteResponse;
import fireroute.api.dto.WalkingPaceDto;
import fireroute.routing.PathResult;
import fireroute.routing.RouteParams;
import fireroute.routing.WalkingPace;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates between the API layer and the routing domain, in both directions:
 * a request becomes routing parameters, a result becomes a response.
 *
 * Stateless on purpose — what is being converted always arrives as a parameter,
 * never as constructor state — so one instance serves every request. It lives in
 * the api layer because that layer already depends on the domain; putting it
 * here keeps the dependency arrow pointing one way.
 *
 * It carries @Component rather than being built in AppConfig because it belongs
 * to the API layer, not the domain. Domain classes stay free of Spring.
 */
@Component
public class RouteMapper {

    /**
     * Builds routing parameters from a request, filling in whatever the caller
     * omitted from the routing defaults rather than from constants duplicated in
     * the API layer.
     *
     * The null checks are the point of this method: the request records box
     * their numbers so that "field omitted" stays distinguishable from "sent as
     * zero", and this is the single place where that distinction gets resolved.
     * Taking RouteOptions rather than a concrete record means both endpoints
     * resolve their defaults through the same code.
     */
    public RouteParams toRouteParams(RouteOptions request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        RouteParams defaults = new RouteParams();

        double fearFactor = request.fearFactor() != null
                ? request.fearFactor()
                : defaults.getFearFactor();

        double maxShelterMinutes = request.maxShelterMinutes() != null
                ? request.maxShelterMinutes()
                : defaults.getMaxShelterMinutes();

        return new RouteParams(
                toDomainPace(request.walkingPace()),
                fearFactor,
                maxShelterMinutes
        );
    }

    public RouteResponse toRouteResponse(PathResult pathResult) {
        if (pathResult == null) {
            throw new IllegalArgumentException("pathResult must not be null");
        }

        if (!pathResult.hasPath()) {
            return new RouteResponse(false, 0.0, List.of(), 0.0);
        }

        List<RoutePoint> points = pathResult.getPath().stream()
                // Junction stores x as longitude and y as latitude (see GeoPoint,
                // and the coordinates in map.json), so y is the latitude and has
                // to come first here.
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
     * Private because the wire enum is an implementation detail of this
     * translation; nothing outside the mapper should be converting DTOs by hand.
     */
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
