package geom;

/**
 * Represents a geographic point (coordinates).
 * x = longitude (קו אורך, e.g. 34.78)
 * y = latitude  (קו רוחב, e.g. 32.08)
 */
public record GeoPoint(double x, double y) {

    // רדיוס כדור הארץ במטרים
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    /**
     * מחזיר את קו הרוחב (Latitude)
     */
    public double lat() {
        return y;
    }

    /**
     * מחזיר את קו האורך (Longitude)
     */
    public double lon() {
        return x;
    }

    /**
     * מחשב מרחק אווירי במטרים לנקודה אחרת באמצעות נוסחת Haversine.
     */
    public double distanceTo(GeoPoint other) {
        if (other == null) {
            throw new IllegalArgumentException("Target GeoPoint cannot be null");
        }

        double lat1 = Math.toRadians(this.lat());
        double lon1 = Math.toRadians(this.lon());
        double lat2 = Math.toRadians(other.lat());
        double lon2 = Math.toRadians(other.lon());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * מרחק אוקלידי פשוט (שימושי לבדיקות יחידה / גרף פשוט במישור).
     */
    public double euclideanDistanceTo(GeoPoint other) {
        if (other == null) {
            throw new IllegalArgumentException("Target GeoPoint cannot be null");
        }
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}