package fireroute.domain.graph;

/**
 * A one-way stretch of road between two junctions, and how long it takes to walk.
 *
 * It carries no risk of its own. Danger in this service is a property of the
 * area — an alert covers all of it at once — and how safe a route is comes from
 * how close its junctions are to a shelter, which ShelterMap answers. A
 * per-segment risk field would be a second, unused definition of the same idea.
 */
public class RoadSegment {

    private final Junction targetJunction;
    private final Junction sourceJunction;

    /** Time in minutes to walk this road at an average pace. */
    private final double travelTime;

    public RoadSegment(Junction targetJunction, Junction sourceJunction, double travelTime) {
        if (targetJunction == null || sourceJunction == null) {
            throw new IllegalArgumentException("Junctions must not be null");
        }
        if (travelTime <= 0) {
            throw new IllegalArgumentException("Time must be positive value");
        }

        this.targetJunction = targetJunction;
        this.sourceJunction = sourceJunction;
        this.travelTime = travelTime;
    }

    public Junction getTargetJunction() {
        return targetJunction;
    }

    public Junction getSourceJunction() {
        return sourceJunction;
    }

    public double getTravelTime() {
        return travelTime;
    }

    @Override
    public String toString() {
        return "RoadSegment{" + sourceJunction.getId() + " -> " + targetJunction.getId()
                + ", " + travelTime + " min}";
    }
}
