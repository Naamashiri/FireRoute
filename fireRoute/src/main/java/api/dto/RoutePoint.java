package api.dto;

/**
 * One junction on a route, reduced to what a client needs in order to draw it.
 *
 * Coordinates are boxed on purpose: junctions built for tests and for abstract
 * graphs carry no coordinates at all, and null has to survive the mapping
 * instead of blowing up on unboxing.
 */
public record RoutePoint(
        String id,
        Double latitude,
        Double longitude
) {
}
