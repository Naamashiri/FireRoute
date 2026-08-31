package fireroute.domain.alert;

import java.time.Duration;
import java.time.Instant;

/** Immutable, internally consistent view of the alert monitor at one moment. */
public record AlertSnapshot(
        AlertStatus status,
        AlertStatus lastKnownStatus,
        Instant lastAttemptAt,
        Instant lastSuccessfulReadAt,
        Instant lastChangedAt
) {
    public AlertSnapshot {
        if (status == null || lastKnownStatus == null) {
            throw new IllegalArgumentException("alert statuses must not be null");
        }
    }

    public static AlertSnapshot initial() {
        return new AlertSnapshot(AlertStatus.UNKNOWN, AlertStatus.UNKNOWN, null, null, null);
    }

    public boolean alertActive() { return lastKnownStatus == AlertStatus.ACTIVE; }

    public boolean isStale(Instant now, Duration staleAfter) {
        if (now == null || staleAfter == null || staleAfter.isNegative()) {
            throw new IllegalArgumentException("now and a non-negative staleAfter are required");
        }
        return lastSuccessfulReadAt == null || !now.isBefore(lastSuccessfulReadAt.plus(staleAfter));
    }
}
