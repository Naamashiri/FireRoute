package fireroute.domain.risk;

import fireroute.domain.graph.RoadSegment;

/**
 * How dangerous is it to walk a given segment right now.
 *
 * The service covers a single Home Front Command alert area, so there is one
 * risk profile and every segment inside the graph shares it: an alert applies to
 * the whole area at once, not to individual streets. The segment is still a
 * parameter because that is the question the routing code asks — "how risky is
 * this edge" — and the day the service covers more than one area, only this
 * class has to learn the difference.
 */
public class RiskEvaluator {

    private final RiskProfile areaProfile;

    public RiskEvaluator(RiskProfile areaProfile) {
        if (areaProfile == null) {
            throw new IllegalArgumentException("areaProfile must be non-null");
        }
        this.areaProfile = areaProfile;
    }

    /**
     * Current risk score for the segment.
     *
     * Read live rather than cached: the profile is updated when an alert starts
     * or ends, and a route calculated a second later must reflect that.
     */
    public double getSegmentRisk(RoadSegment segment) {
        if (segment == null) {
            throw new IllegalArgumentException("segment must be non-null");
        }
        return areaProfile.calculateCurrentRisk();
    }
}
