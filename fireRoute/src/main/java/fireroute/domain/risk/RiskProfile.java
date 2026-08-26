package fireroute.domain.risk;

import java.time.Instant;

/**
 * Represents the risk profile of a geographic area.
 *
 * The profile is based on:
 * - The total number of historical alerts.
 * - The time of the latest alert.
 * - Whether an alert is currently active.
 */
public class RiskProfile {

    private static final double ACTIVE_ALERT_PENALTY = 100.0;

    private final String areaId;

    private int alertsCounter;
    private Instant lastAlertTime;
    private boolean activeAlert;

    public RiskProfile(
            String areaId,
            int alertsCounter,
            Instant lastAlertTime,
            boolean activeAlert
    ) {
        if (areaId == null || areaId.isBlank()) {
            throw new IllegalArgumentException(
                    "areaId cannot be null or blank"
            );
        }

        if (alertsCounter < 0) {
            throw new IllegalArgumentException(
                    "alertsCounter cannot be negative"
            );
        }

        this.areaId = areaId;
        this.alertsCounter = alertsCounter;
        this.lastAlertTime = lastAlertTime;
        this.activeAlert = activeAlert;
    }

    /**
     * Records a new alert for this area.
     */
    public void recordAlert() {
        alertsCounter++;
        lastAlertTime = Instant.now();
        activeAlert = true;
    }

    /**
     * Marks the current alert as inactive.
     */
    public void clearActiveAlert() {
        activeAlert = false;
    }

    /**
     * Calculates the current risk score.
     *
     * For the MVP:
     * - Every historical alert contributes one risk point.
     * - An active alert adds a large immediate penalty.
     */
    public double calculateCurrentRisk() {
        double historicalRisk = alertsCounter;
        double currentAlertRisk =
                activeAlert ? ACTIVE_ALERT_PENALTY : 0.0;

        return historicalRisk + currentAlertRisk;
    }

    public String getAreaId() {
        return areaId;
    }

    public int getAlertsCounter() {
        return alertsCounter;
    }

    public Instant getLastAlertTime() {
        return lastAlertTime;
    }

    public boolean hasActiveAlert() {
        return activeAlert;
    }

    @Override
    public String toString() {
        return "RiskProfile{" +
                "areaId='" + areaId + '\'' +
                ", alertsCounter=" + alertsCounter +
                ", lastAlertTime=" + lastAlertTime +
                ", activeAlert=" + activeAlert +
                ", riskScore=" + calculateCurrentRisk() +
                '}';
    }
}