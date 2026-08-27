package fireroute.api.dto;

/**
 * A request to route to the nearest shelter.
 *
 * It carries no destination by design: under an alert the user does not choose
 * where to go, and offering the field would suggest otherwise. That single
 * missing field is the whole difference from RouteRequest, and it is the reason
 * this is a separate type rather than a shared one with a nullable destination —
 * a contract should not describe a field it will ignore.
 */
public record EmergencyRouteRequest(
        String sourceId,
        WalkingPaceDto walkingPace,
        Double fearFactor,
        Double maxShelterMinutes
) implements RouteOptions {

    public EmergencyRouteRequest {
        RouteOptions.validate(fearFactor, maxShelterMinutes);
    }
}
