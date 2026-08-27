package fireroute.domain.alert;

/**
 * Whether a Home Front Command alert is currently live for the area this service
 * covers.
 *
 * A single flag, deliberately. The service covers one alert area, so there is
 * nothing to distinguish between streets: an alert applies to all of them at
 * once. And an alert does not make one walking route preferable to another — it
 * makes walking to your errand the wrong activity altogether. Its job is to tell
 * the client to switch to emergency routing, not to re-weight the cost function.
 *
 * The field is volatile because this object is the one piece of shared state
 * that changes while the application runs: an alert source writes to it from its
 * own thread while request threads read it. Volatile guarantees the write
 * becomes visible instead of sitting in a core's cache, and with a single field
 * there is no half-updated state to guard against — which is why no lock is
 * needed here.
 */
public class AlertState {

    private final String areaId;

    private volatile boolean alertActive;

    public AlertState(String areaId) {
        if (areaId == null || areaId.isBlank()) {
            throw new IllegalArgumentException("areaId cannot be null or blank");
        }
        this.areaId = areaId;
    }

    public void raiseAlert() {
        this.alertActive = true;
    }

    public void clearAlert() {
        this.alertActive = false;
    }

    public boolean isAlertActive() {
        return alertActive;
    }

    public String getAreaId() {
        return areaId;
    }

    @Override
    public String toString() {
        return "AlertState{areaId='" + areaId + "', alertActive=" + alertActive + "}";
    }
}
