package fireroute.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

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
        @NotBlank(message = "sourceId must not be blank")
        String sourceId,
        WalkingPaceDto walkingPace,
        @PositiveOrZero(message = "fearFactor must not be negative")
        Double fearFactor,
        @PositiveOrZero(message = "maxShelterMinutes must not be negative")
        Double maxShelterMinutes
) implements RouteOptions { }
