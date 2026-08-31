package fireroute.api.mapper;

import fireroute.api.dto.RoutePoint;
import fireroute.api.dto.RouteOptions;
import fireroute.api.dto.RouteFailureReason;
import fireroute.api.dto.RouteResponse;
import fireroute.api.dto.WalkingPaceDto;
import fireroute.api.dto.RouteType;
import fireroute.domain.routing.PathResult;
import fireroute.domain.routing.RouteParams;
import fireroute.domain.routing.WalkingPace;
import fireroute.domain.graph.GeoPoint;
import fireroute.domain.shelter.ShelterRepository;

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
    private final ShelterRepository shelterRepository;
    private final ShelterMapper shelterMapper;

    public RouteMapper(ShelterRepository shelterRepository, ShelterMapper shelterMapper) {
        this.shelterRepository = shelterRepository;
        this.shelterMapper = shelterMapper;
    }

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
            return new RouteResponse(
                    false,
                    RouteType.NORMAL,
                    RouteFailureReason.NO_ROUTE_EXISTS,
                    0.0,
                    List.of(),
                    0.0,
                    false,
                    null,
                    false
            );
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
                RouteType.NORMAL,
                RouteFailureReason.NONE,
                pathResult.getTotalTime(),
                points,
                pathResult.getMaxMinutesToShelter(),
                pathResult.isShelterConstraintSatisfied(),
                null,
                false
        );
    }

    public RouteResponse toEmergencyRouteResponse(PathResult pathResult) {
        RouteResponse route = toRouteResponse(pathResult);
        if (!route.found()) {
            return new RouteResponse(false, RouteType.EMERGENCY,
                    RouteFailureReason.NO_REACHABLE_SHELTER, 0.0, List.of(),
                    0.0, false, null, false);
        }

        RoutePoint destination = route.pathPoints().get(route.pathPoints().size() - 1);
        var shelter = shelterRepository.findNearestTo(
                new GeoPoint(destination.longitude(), destination.latitude())
        ).map(shelterMapper::toResponse).orElse(null);

        return new RouteResponse(route.found(), RouteType.EMERGENCY, route.failureReason(), route.totalTravelTime(),
                route.pathPoints(), route.maxMinutesToShelter(),
                route.shelterConstraintSatisfied(), shelter, route.pathPoints().size() == 1);
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
