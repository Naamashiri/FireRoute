package fireroute.routing;

import fireroute.domain.graph.Junction;
import fireroute.domain.graph.RoadSegment;

/**
 * What one road segment costs to walk.
 *
 * <pre>
 *   cost = walkingMinutes + fearFactor x minutesExposed
 * </pre>
 *
 * Two terms, both in minutes, both meaning something a person could state:
 * how long this segment takes, and how far from cover it leaves you. The
 * fearFactor is the exchange rate between them — how many extra minutes of
 * walking the user will pay to avoid one minute of exposure. At zero it is the
 * fastest route; raise it and the route hugs the shelters.
 *
 * Exposure is measured with ShelterMap — walking time through the graph to the
 * nearest shelter — and not as a straight line to the nearest shelter in the
 * repository. Two reasons: it is the same measurement the shelter constraint
 * uses, so "near a shelter" means one thing everywhere; and it is precomputed
 * once at startup, so this runs in constant time inside Dijkstra's inner loop
 * rather than scanning every shelter on every edge.
 */
public class RouteCostCalculator {

    /**
     * Being within this many minutes of a shelter is treated as safe and costs
     * nothing. Ninety seconds is roughly the warning time in central Israel.
     */
    private static final double SAFE_MINUTES_TO_SHELTER = 1.5;

    private final ShelterMap shelterMap;

    public RouteCostCalculator(ShelterMap shelterMap) {
        if (shelterMap == null) {
            throw new IllegalArgumentException("shelterMap must be non-null");
        }
        this.shelterMap = shelterMap;
    }

    public double calculateCost(RoadSegment segment, RouteParams userParams) {
        if (segment == null || userParams == null) {
            throw new IllegalArgumentException("segment and userParams must be non-null");
        }

        double walkingMinutes = segment.getTravelTime() * userParams.getPaceMultiplier();
        double excessExposure = Math.max(
                0.0,
                minutesToShelter(segment.getTargetJunction(), userParams) - SAFE_MINUTES_TO_SHELTER
        );

        return walkingMinutes + userParams.getFearFactor() * excessExposure;
    }

    /**
     * Exposure is measured at the junction the segment leads to — where the
     * walker actually ends up.
     *
     * A junction with no shelter reachable at all would otherwise cost infinity,
     * which makes every route through it incomparable rather than merely bad. The
     * caller's own limit stands in for it: by definition that is the worst
     * exposure they were willing to consider.
     */
    private double minutesToShelter(Junction junction, RouteParams params) {
        double minutes = params.getPaceMultiplier() * shelterMap.getDistanceToShelter(junction);

        return Double.isInfinite(minutes) ? params.getMaxShelterMinutes() : minutes;
    }
}
