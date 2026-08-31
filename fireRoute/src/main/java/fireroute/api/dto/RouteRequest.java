package fireroute.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A request to route between two chosen points.
 *
 * Resolving the omitted values against the routing defaults is RouteMapper's
 * job, so those defaults keep living in exactly one place.
 */
public record RouteRequest(
        @NotBlank(message = "sourceId must not be blank")
        String sourceId,
        @NotBlank(message = "destinationId must not be blank")
        String destinationId,
        WalkingPaceDto walkingPace,
        @PositiveOrZero(message = "fearFactor must not be negative")
        Double fearFactor,
        @PositiveOrZero(message = "maxShelterMinutes must not be negative")
        Double maxShelterMinutes
) implements RouteOptions { }
