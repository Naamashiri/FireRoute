package fireroute.infrastructure.alert;

import fireroute.application.AlertReading;
import fireroute.application.AlertSource;
import fireroute.domain.alert.AlertState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Asks the alert source how things are, on a timer, and updates AlertState.
 *
 * fixedDelay, not fixedRate: the delay is measured from the end of one run, so a
 * slow source spaces the calls out instead of queueing catch-up requests at a
 * service that is already struggling. Rate is right for internal work that must
 * keep a cadence; delay is right for calling someone else.
 *
 * Everything here runs on Spring's scheduling thread, one instance at a time —
 * fixedDelay guarantees no overlap — so the failure counter needs no
 * synchronisation. The only value crossing threads is AlertState's flag, which
 * is volatile.
 */
@Component
public class AlertPoller {

    private static final Logger log = LoggerFactory.getLogger(AlertPoller.class);

    /** Failures tolerated quietly before the log starts complaining. */
    private static final int FAILURES_BEFORE_WARNING = 3;

    private final AlertSource alertSource;
    private final AlertState alertState;

    private int consecutiveFailures;

    public AlertPoller(AlertSource alertSource, AlertState alertState) {
        if (alertSource == null || alertState == null) {
            throw new IllegalArgumentException("alertSource and alertState must be non-null");
        }
        this.alertSource = alertSource;
        this.alertState = alertState;
    }

    @Scheduled(fixedDelayString = "${fireroute.alerts.poll-millis:5000}")
    public void poll() {
        switch (readSafely()) {
            case ACTIVE -> {
                consecutiveFailures = 0;
                if (!alertState.isAlertActive()) {
                    log.info("Alert raised for area {}", alertState.getAreaId());
                }
                alertState.setAlertActive(true);
            }
            case QUIET -> {
                consecutiveFailures = 0;
                if (alertState.isAlertActive()) {
                    log.info("Alert cleared for area {}", alertState.getAreaId());
                }
                alertState.setAlertActive(false);
            }
            case UNKNOWN -> onUnknown();
        }
    }

    /**
     * A source is allowed to fail; it is not allowed to kill the poller. If this
     * method ever let an exception escape, Spring would stop rescheduling the
     * task and the service would go silently blind.
     */
    private AlertReading readSafely() {
        try {
            AlertReading reading = alertSource.read();
            return reading != null ? reading : AlertReading.UNKNOWN;
        } catch (Exception exception) {
            log.debug("Alert source threw", exception);
            return AlertReading.UNKNOWN;
        }
    }

    /**
     * The state is left exactly as it was.
     *
     * Clearing an alert because the source became unreachable would turn a
     * network failure into an all-clear, which is the dangerous direction to be
     * wrong in. Holding a stale alert is merely inconvenient.
     */
    private void onUnknown() {
        consecutiveFailures++;

        if (consecutiveFailures == FAILURES_BEFORE_WARNING) {
            log.warn("Alert source unreachable for {} consecutive polls; holding last known state (alertActive={})",
                    consecutiveFailures, alertState.isAlertActive());
        }
    }
}
