package risk;

import graph.Junction;
import graph.RoadSegment;

/**
 בהינתן שני צמתים מה רמת הסכנה של הקטע המחבר בינהם
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
    public double getSegmentRisk(RoadSegment segment) {
        return Math.max(getJunctionRisk(segment.sourceJunction), getJunctionRisk(segment.targetJunction));
    }
}