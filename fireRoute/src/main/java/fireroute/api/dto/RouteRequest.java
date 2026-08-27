package fireroute.api.dto;

/**
 * A request to route between two chosen points.
 *
 * Resolving the omitted values against the routing defaults is RouteMapper's
 * job, so those defaults keep living in exactly one place.
 */
public record RouteRequest(
        String sourceId,
        String destinationId,
        WalkingPaceDto walkingPace,
        Double fearFactor,
        Double maxShelterMinutes
) implements RouteOptions {

    public RouteRequest {
        RouteOptions.validate(fearFactor, maxShelterMinutes);
    }
}
