package fireroute.api.dto;

import java.util.List;

/**
 * A calculated route as the client sees it.
 *
 * totalTravelTime says how long the walk is; maxMinutesToShelter says how
 * exposed it ever gets. They are different measures, and the second is the one
 * that makes a route safe or not — a long route that never strays far from
 * cover is a good answer, a short one that does is not.
 *
 * shelterConstraintSatisfied is false when a route could only be found by
 * relaxing the requested shelter limit; the client should present it as a
 * fallback with a warning, not as a safe route. It carries no meaning when
 * found is false.
 *
 * totalCost is deliberately absent: it is the internal weighting the algorithm
 * optimises, and publishing it would freeze the cost function into the API.
 */
public record RouteResponse(
        boolean found,
        RouteType routeType,
        RouteFailureReason failureReason,
        double totalTravelTime,
        List<RoutePoint> pathPoints,
        double maxMinutesToShelter,
        boolean shelterConstraintSatisfied,
        ShelterResponse destinationShelter,
        boolean alreadyAtShelter
) {
}
