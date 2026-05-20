package routing;

/**
 * Holds user/configuration parameters for route planning.
 */
public class RouteParams {

    /** User "fear" factor: higher means avoiding risk more strongly. Must be >= 0. */
    private final double fearFactor;

    /** Maximum allowed time (minutes) from any visited junction to the nearest shelter. Must be > 0. */
    private final double maxShelterMinutes;

    /**
     * Creates routing parameters.
     *
     * @param fearFactor        fear factor (must be >= 0)
     * @param maxShelterMinutes max allowed minutes to a shelter (must be > 0)
     *
     * Preconditions:
     * - fearFactor >= 0
     * - maxShelterMinutes > 0
     *
     * Postconditions:
     * - this.fearFactor == fearFactor
     * - this.maxShelterMinutes == maxShelterMinutes
     *
     * @throws IllegalArgumentException if parameters are out of range
     */
    public RouteParams(double fearFactor, double maxShelterMinutes) {
        if (fearFactor < 0) {
            throw new IllegalArgumentException("fearFactor must be >= 0");
        }
        if (maxShelterMinutes <= 0) {
            throw new IllegalArgumentException("maxShelterMinutes must be > 0");
        }
        this.fearFactor = fearFactor;
        this.maxShelterMinutes = maxShelterMinutes;
    }

    /** Default "maxShelterMinutes" is 7 minutes */
    public RouteParams(double fearFactor) {
        if (fearFactor < 0) {
            throw new IllegalArgumentException("fearFactor must be >= 0");
        }
        this.fearFactor = fearFactor;
        this.maxShelterMinutes = 7.0;
    }

    /**
     * Convenience factory: typical defaults for your app.
     * (Fear 1.0, shelter constraint 7 minutes).
     *
     * @return RouteParams with common defaults
     */
    public static RouteParams defaultParams() {
        return new RouteParams(1.0, 7.0);
    }

    /** @return user fear factor (>= 0). */
    public double getFearFactor() {
        return fearFactor;
    }

    /** @return maximum allowed minutes to a shelter (> 0). */
    public double getMaxShelterMinutes() {
        return maxShelterMinutes;
    }

    @Override
    public String toString() {
        return "RouteParams{fearFactor=" + fearFactor +
                ", maxShelterMinutes=" + maxShelterMinutes + "}";
    }
}