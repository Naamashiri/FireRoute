package api.dto;

/**
 * Incoming route calculation request (JSON -> RouteRequest).
 *
 * The numeric parameters are boxed so that "field omitted" (null) stays
 * distinguishable from "field sent as zero". That distinction matters:
 * maxShelterMinutes = 0 is a meaningful request, meaning only junctions that
 * are shelters themselves qualify. Resolving the missing values against the
 * routing defaults is RouteMapper's job, so those defaults keep living in
 * exactly one place.
 */
public record RouteRequest(
        String sourceId,
        String destinationId,
        Double maxShelterMinutes,
        WalkingPaceDto walkingPace,
        Double fearFactor
) {
    public RouteRequest {
        if (maxShelterMinutes != null && maxShelterMinutes < 0) {
            throw new IllegalArgumentException("maxShelterMinutes must not be negative");
        }
        if (fearFactor != null && fearFactor < 0) {
            throw new IllegalArgumentException("fearFactor must not be negative");
        }
    }
}
