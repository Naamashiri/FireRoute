package fireroute.api.dto;

import fireroute.domain.alert.AlertStatus;
import java.time.Instant;

/**
 * The current alert state, as the client polls for it.
 *
 * `alertActive` is the whole answer: when it turns true the client should stop
 * asking for ordinary routes and call the emergency endpoint instead. An alert
 * does not make one walking route better than another — it makes walking to
 * your errand the wrong thing to be doing.
 */
public record AlertStatusResponse(
        String areaId,
        AlertStatus status,
        AlertStatus lastKnownStatus,
        boolean alertActive,
        boolean simulated,
        boolean stale,
        Instant lastAttemptAt,
        Instant lastSuccessfulReadAt,
        Instant lastChangedAt
) {
}
