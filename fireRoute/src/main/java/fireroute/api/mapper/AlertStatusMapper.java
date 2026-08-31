package fireroute.api.mapper;

import fireroute.api.dto.AlertStatusResponse;
import fireroute.domain.alert.AlertSnapshot;
import fireroute.domain.alert.AlertState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class AlertStatusMapper {
    private final Clock clock;
    private final Duration staleAfter;
    private final boolean simulated;

    public AlertStatusMapper(
            Clock alertClock,
            @Value("${fireroute.alerts.stale-after-seconds:10}") long staleAfterSeconds,
            @Value("${fireroute.alerts.source:simulated}") String alertSource
    ) {
        if (staleAfterSeconds < 0) throw new IllegalArgumentException("stale-after-seconds must not be negative");
        this.clock = alertClock;
        this.staleAfter = Duration.ofSeconds(staleAfterSeconds);
        this.simulated = "simulated".equalsIgnoreCase(alertSource);
    }

    public AlertStatusResponse toResponse(AlertState state) {
        AlertSnapshot snapshot = state.snapshot();
        return new AlertStatusResponse(
                state.getAreaId(), snapshot.status(), snapshot.lastKnownStatus(),
                snapshot.alertActive(), simulated, snapshot.isStale(clock.instant(), staleAfter),
                snapshot.lastAttemptAt(), snapshot.lastSuccessfulReadAt(), snapshot.lastChangedAt()
        );
    }
}
