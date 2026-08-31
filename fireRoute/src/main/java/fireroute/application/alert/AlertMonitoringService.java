package fireroute.application.alert;

import fireroute.domain.alert.AlertSnapshot;
import fireroute.domain.alert.AlertState;
import fireroute.domain.alert.AlertStatus;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executes one alert check and applies the UNKNOWN/last-known-state policy. */
public class AlertMonitoringService {
    private static final Logger log = LoggerFactory.getLogger(AlertMonitoringService.class);
    private final AlertSource source;
    private final AlertState state;
    private final Clock clock;

    public AlertMonitoringService(AlertSource source, AlertState state, Clock clock) {
        if (source == null || state == null || clock == null) {
            throw new IllegalArgumentException("source, state and clock must not be null");
        }
        this.source = source;
        this.state = state;
        this.clock = clock;
    }

    public AlertSnapshot checkNow() {
        AlertStatus status;
        try {
            status = source.read();
        } catch (Exception exception) {
            log.warn("Alert source failed; retaining the last known status", exception);
            status = AlertStatus.UNKNOWN;
        }
        if (status == null) status = AlertStatus.UNKNOWN;
        AlertSnapshot before = state.snapshot();
        AlertSnapshot after = state.record(status, clock.instant());
        if (after.lastKnownStatus() != before.lastKnownStatus()) {
            log.info("Alert status for area {} changed to {}", state.getAreaId(), after.lastKnownStatus());
        }
        return after;
    }
}
