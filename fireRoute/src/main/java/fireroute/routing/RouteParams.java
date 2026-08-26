package fireroute.routing;

/**
 * The parameters a user brings to a route request: how fast they walk, how much
 * they weight risk against time, and how far from cover they are willing to be
 * at any point along the way.
 *
 * Immutable — a request's parameters are fixed for the duration of that request,
 * and nothing should be able to change them from under the algorithm while it
 * runs.
 */
public class RouteParams {

    private static final double DEFAULT_MAX_SHELTER_MINUTES = 7.0;
    private static final double DEFAULT_WALKING_SPEED_KMH = 5.0;
    private static final double DEFAULT_USER_FEAR_FACTOR = 1.0;

    private final double walkingSpeedKmh;
    private final double userFearFactor;
    private final double maxShelterMinutes;

    public RouteParams() {
        this.walkingSpeedKmh = DEFAULT_WALKING_SPEED_KMH;
        this.userFearFactor = DEFAULT_USER_FEAR_FACTOR;
        this.maxShelterMinutes = DEFAULT_MAX_SHELTER_MINUTES;
    }

    public RouteParams(WalkingPace walkingPace, double userFearFactor) {
        this(walkingPace, userFearFactor, DEFAULT_MAX_SHELTER_MINUTES);
    }

    /**
     * @param maxShelterMinutes the longest a user is willing to be from the
     *        nearest shelter at any junction on the route. Zero is a meaningful
     *        value, not an omission: it admits only junctions that are shelters
     *        themselves.
     */
    public RouteParams(WalkingPace walkingPace, double userFearFactor, double maxShelterMinutes) {
        if (walkingPace == null) {
            throw new IllegalArgumentException("walkingPace cannot be null");
        }
        if (userFearFactor < 0) {
            throw new IllegalArgumentException("userFearFactor must be non-negative");
        }
        if (maxShelterMinutes < 0) {
            throw new IllegalArgumentException("maxShelterMinutes must be non-negative");
        }

        this.walkingSpeedKmh = walkingPace.getSpeedKmh();
        this.userFearFactor = userFearFactor;
        this.maxShelterMinutes = maxShelterMinutes;
    }

    public double getMaxShelterMinutes() { return maxShelterMinutes; }
    public double getWalkingSpeedKmh() { return walkingSpeedKmh; }
    public double getFearFactor() { return userFearFactor; }

    /**
     * Slower-than-average pace yields a multiplier > 1 (more time exposed to risk per segment);
     * faster-than-average pace yields a multiplier < 1.
     */
    public double getPaceMultiplier() {
        return WalkingPace.AVERAGE.getSpeedKmh() / walkingSpeedKmh;
    }
}
