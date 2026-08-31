package fireroute.infrastructure.alert;

import fireroute.application.alert.AlertSource;
import fireroute.domain.alert.AlertStatus;

/**
 * An alert source whose answer is set by hand instead of fetched.
 *
 * It backs both the demo and the tests. Triggering an alert through it rather
 * than by writing to AlertState directly matters: the poller would overwrite a
 * hand-set flag on its next tick, and going through the source exercises the
 * real pipeline end to end — which is the point of the demo.
 *
 * The field is volatile because a request thread writes it (through the
 * simulation endpoint) while the scheduler thread reads it. One field, so no
 * lock: there is no second value it could fall out of step with.
 */
public class SimulatedAlertSource implements AlertSource {

    private volatile AlertStatus reading = AlertStatus.QUIET;

    @Override
    public AlertStatus read() {
        return reading;
    }

    public void set(AlertStatus reading) {
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }
        this.reading = reading;
    }
}
