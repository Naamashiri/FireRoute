package geom;

import java.util.List;

/**
 * Represents a closed polygon on a 2D plane.
 * Provides spatial logic for point-in-polygon testing.
 */
public record GeoPolygon(List<GeoPoint> points) {

    /**
     * Constructor with validation to ensure the polygon is valid.
     */
    public GeoPolygon {
        if (points == null || points.size() < 3) {
            throw new IllegalArgumentException("A polygon must have at least 3 vertices to form an area.");
        }
        for (GeoPoint p : points) {
            if (p == null) {
                throw new IllegalArgumentException("Polygon points must be non-null");
            }
        }
        // Defensive copy to ensure the polygon remains immutable
        points = List.copyOf(points);
    }

    /**
     * Determines if a point (x, y) is inside the polygon using the Ray Casting algorithm.
     * Logic: Cast a ray from the point to the right. If it crosses the polygon's
     * boundaries an odd number of times, the point is inside.
     *
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     * @return true if the point is inside the polygon.
     */
    public boolean contains(double x, double y) {
        boolean inside = false;
        int n = points.size();

        // Iterate through each edge of the polygon
        for (int i = 0, j = n - 1; i < n; j = i++) {
            GeoPoint pi = points.get(i);
            GeoPoint pj = points.get(j);

            // 1. Check if the point's Y coordinate is between the Y coordinates of the edge's endpoints.
            // 2. Calculate the X coordinate where the ray intersects the edge.
            // 3. If the point's X is to the left of that intersection, the ray crosses the edge.
            boolean intersect = ((pi.y() > y) != (pj.y() > y))
                    && (x < (pj.x() - pi.x()) * (y - pi.y()) / (pj.y() - pi.y()) + pi.x());

            if (intersect) {
                inside = !inside; // Toggle status for every intersection (Even-Odd rule)
            }
        }

        return inside;
    }

    /**
     * Helper method to check containment using a GeoPoint object.
     */
    public boolean contains(GeoPoint point) {
        if (point == null) return false;
        return contains(point.x(), point.y());
    }
}