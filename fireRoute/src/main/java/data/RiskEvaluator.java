package data;

import model.Junction;

/**
 * RiskEvaluator provides the logic for quantifying danger levels
 * across the map based on spatial indexing and alert statistics.
 */
public class RiskEvaluator {

    private final ZoneIndex zoneIndex;

    public RiskEvaluator(ZoneIndex zoneIndex) {
        if (zoneIndex == null) {
            throw new IllegalArgumentException("zoneIndex must be non-null");
        }
        this.zoneIndex = zoneIndex;
    }

    /**
     * Calculates the risk score for a single junction (0.0 to 1.0).
     */
    public double getJunctionRisk(Junction junction) {
        RiskZone zone = zoneIndex.getZone(junction);
        return (zone == null) ? 0.0 : zone.getRiskScore();
    }

    /**
     * Estimates the risk for the segment connecting two junctions.
     * Uses Math.max to ensure the most dangerous part of the segment
     * dictates the overall risk.
     */
    public double getSegmentRisk(Junction source, Junction target) {
        return Math.max(getJunctionRisk(source), getJunctionRisk(target));
    }
}