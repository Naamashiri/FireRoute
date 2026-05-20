package data;

/**
 * Stores statistical data about alerts in a specific area.
 */
public record RiskProfile(
        int totalAlerts,
        double severityIndex,   // must be in [0,1]
        long lastAlertTimestamp // milliseconds since epoch
) {

    public RiskProfile {
        if (totalAlerts < 0) {
            throw new IllegalArgumentException("totalAlerts must be >= 0");
        }
        if (severityIndex < 0.0 || severityIndex > 1.0) {
            throw new IllegalArgumentException("severityIndex must be in [0,1]");
        }
        if (lastAlertTimestamp > System.currentTimeMillis()) {
            throw new IllegalArgumentException("lastAlertTimestamp cannot be in the future");
        }
    }

    /**
     * Calculates a dynamic risk score in [0,1].
     * Higher score if there were many alerts, high severity,
     * and the most recent alert was recent.
     */
    public double calculateRiskScore() {
        double alertsScore = normalizeAlerts(totalAlerts);
        double recencyWeight = calculateRecencyWeight();

        double baseScore = 0.4 * alertsScore + 0.6 * severityIndex;
        return baseScore * recencyWeight;
    }

    /**
     * Maps alert count to [0,1] with saturation:
     * after some point, additional alerts increase score less dramatically.
     */
    private static double normalizeAlerts(int alerts) {
        return 1.0 - Math.exp(-0.1 * alerts);
    }

    /**
     * Exponential decay by hours since last alert.
     * More recent alerts => higher weight.
     */
    private double calculateRecencyWeight() {
        long millisSince = System.currentTimeMillis() - lastAlertTimestamp;
        double hoursSince = millisSince / (1000.0 * 60 * 60);
        return Math.exp(-0.05 * hoursSince);
    }
}