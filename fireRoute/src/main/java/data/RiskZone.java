package data;

import geom.GeoPolygon;
import model.Junction;

/**
 * Represents a geographic risk zone:
 * a named area on the map (polygon) together with its risk statistics.
 */
public record RiskZone(
        String id,
        String name,
        GeoPolygon polygon,
        RiskProfile riskProfile
) {
    /**
     * Compact constructor for validation.
     */
    public RiskZone {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must be non-null and non-empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-null and non-empty");
        }
        if (polygon == null) {
            throw new IllegalArgumentException("polygon must be non-null");
        }
        if (riskProfile == null) {
            throw new IllegalArgumentException("riskProfile must be non-null");
        }
    }

    /**
     * Checks whether the given junction is inside this zone.
     */
    public boolean contains(Junction j) {
        if (j == null || !j.hasCoordinates()) {
            return false;
        }
        return polygon.contains(j.getX(), j.getY());
    }

    /**
     * @return the dynamic risk score calculated by the profile.
     */
    public double getRiskScore() {
        return riskProfile.calculateRiskScore();
    }
}