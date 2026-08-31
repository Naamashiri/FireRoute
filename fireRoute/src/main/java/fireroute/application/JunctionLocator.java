package fireroute.application;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;

/**
 * Turns a position on the map into a junction the router can start from.
 *
 * Without this the API cannot be used by anything real. A phone knows where it
 * is in latitude and longitude; it has never heard of an OpenStreetMap node id,
 * and neither has a person choosing a destination. This is the translation
 * between the two, and every client needs it before it can ask for a route at
 * all.
 */
public class JunctionLocator {

    private final Graph graph;
    private final double maxSnapDistanceMeters;

    public JunctionLocator(Graph graph) {
        this(graph, 250.0);
    }

    public JunctionLocator(Graph graph, double maxSnapDistanceMeters) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        if (!Double.isFinite(maxSnapDistanceMeters) || maxSnapDistanceMeters <= 0) {
            throw new IllegalArgumentException("maxSnapDistanceMeters must be finite and positive");
        }
        this.graph = graph;
        this.maxSnapDistanceMeters = maxSnapDistanceMeters;
    }

    /**
     * The junction closest to the given position.
     *
     * A linear sweep of the graph. At a few thousand junctions this is well
     * under a millisecond and runs once per request rather than per edge, so a
     * spatial index would be complexity bought for nothing measurable — the
     * lesson of the last profiling round.
     *
     * Distances are compared in an equirectangular approximation: over a city
     * the error is centimetres, and only the ORDER matters here, not the value.
     *
     * @throws IllegalArgumentException if the graph holds no located junction
     */
    public Junction nearestTo(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new IllegalArgumentException("latitude and longitude must be finite");
        }
        double metresPerDegreeLat = 111_320.0;
        double metresPerDegreeLon = metresPerDegreeLat * Math.cos(Math.toRadians(latitude));

        Junction nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;

        for (Junction junction : graph.getJunctions()) {
            if (!junction.hasCoordinates()) {
                continue;
            }

            double dx = (junction.getX() - longitude) * metresPerDegreeLon;
            double dy = (junction.getY() - latitude) * metresPerDegreeLat;
            double distanceSquared = dx * dx + dy * dy;

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = junction;
            }
        }

        if (nearest == null) {
            throw new IllegalArgumentException("the graph contains no junction with coordinates");
        }

        double nearestDistanceMeters = Math.sqrt(nearestDistanceSquared);
        if (nearestDistanceMeters > maxSnapDistanceMeters) {
            throw new LocationOutsideCoverageException(nearestDistanceMeters, maxSnapDistanceMeters);
        }

        return nearest;
    }
}
