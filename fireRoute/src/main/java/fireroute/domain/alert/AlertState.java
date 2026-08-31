package fireroute.domain.alert;

import java.time.Instant;

/**
 * Whether a Home Front Command alert is currently live for the area this service
 * covers.
 *
 * The state belongs to one alert area. Besides the latest reading, it retains
 * the last successful reading and its timestamps. This lets the API distinguish
 * "quiet" from "the alert source could not be reached" without losing the last
 * known safety information.
 *
 * The snapshot is immutable and the reference is volatile, so request threads
 * always observe one complete, consistent version of the state.
 */
public class AlertState {

    private final String areaId;

    private volatile AlertSnapshot snapshot;

    public AlertState(String areaId) {
        if (areaId == null || areaId.isBlank()) {
            throw new IllegalArgumentException("areaId cannot be null or blank");
        }
        this.areaId = areaId;
        this.snapshot = AlertSnapshot.initial();
    }

    public synchronized AlertSnapshot record(AlertStatus status, Instant observedAt) {
        if (status == null || observedAt == null) {
            throw new IllegalArgumentException("status and observedAt must not be null");
        }

        AlertSnapshot previous = snapshot;
        AlertStatus lastKnown = previous.lastKnownStatus();
        Instant lastSuccessfulReadAt = previous.lastSuccessfulReadAt();
        Instant lastChangedAt = previous.lastChangedAt();

        if (status != AlertStatus.UNKNOWN) {
            lastSuccessfulReadAt = observedAt;
            if (status != lastKnown) {
                lastKnown = status;
                lastChangedAt = observedAt;
            }
        }

        snapshot = new AlertSnapshot(
                status,
                lastKnown,
                observedAt,
                lastSuccessfulReadAt,
                lastChangedAt
        );
        return snapshot;
    }

    public AlertSnapshot snapshot() {
        return snapshot;
    }

    public AlertStatus getAlertStatus() {
        return snapshot.status();
    }

    public String getAreaId() {
        return areaId;
    }

    @Override
    public String toString() {
        return "AlertState{areaId='" + areaId + "', snapshot=" + snapshot + "}";
    }
}
