package fireroute.api.dto;

/**
 * A junction as the client sees it: the id it must quote when asking for a
 * route, and the point on the map it actually sits at — which is rarely the
 * exact spot the user tapped, so the client can show where it snapped to.
 */
public record JunctionResponse(
        String id,
        double latitude,
        double longitude,
        boolean shelter
) {
}
