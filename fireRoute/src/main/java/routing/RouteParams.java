package routing;

/**
 * Holds user/configuration parameters for route planning.
 */
public class RouteParams {
    private static final double DEFAULT_MAX_SHELTER_MINUTES = 7.0;
    private static final double DEFAULT_WALKING_SPEED_KMH = 5.0;
    private static final double DEFAULT_USER_FEAR_FACTOR = 1.0;
    private double maxShelterMinutes;
    private double walkingSpeedKmh;
    private double userFearFactor;

    public RouteParams() {
        this.maxShelterMinutes = DEFAULT_MAX_SHELTER_MINUTES;
        this.walkingSpeedKmh = DEFAULT_WALKING_SPEED_KMH;
        this.userFearFactor = DEFAULT_USER_FEAR_FACTOR;
    }

    public RouteParams(double maxShelterMinutes, WalkingPace walkingPace, double userFearFactor) {
        if (maxShelterMinutes < 0) {
            throw new IllegalArgumentException("maxShelterMinutes must be non-negative");
        }
        if (walkingPace == null) {
            throw new IllegalArgumentException("walkingPace cannot be null");
        }
        if (userFearFactor < 0) {
            throw new IllegalArgumentException("userFearFactor must be non-negative");
        }
        this.maxShelterMinutes = maxShelterMinutes;
        this.walkingSpeedKmh = walkingPace.getSpeedKmh();
        this.userFearFactor = userFearFactor;
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



