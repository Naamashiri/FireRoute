package fireroute.application;

public class LocationOutsideCoverageException extends RuntimeException {
    private final double distanceMeters;
    private final double maximumMeters;

    public LocationOutsideCoverageException(double distanceMeters, double maximumMeters) {
        super("Location is outside the supported map area");
        this.distanceMeters = distanceMeters;
        this.maximumMeters = maximumMeters;
    }

    public double getDistanceMeters() { return distanceMeters; }
    public double getMaximumMeters() { return maximumMeters; }
}
