package fireroute.application.alert;

import fireroute.domain.alert.AlertStatus;

/**
 * Somewhere to ask whether an alert is live right now.
 *
 * The interface lives here, with the code that needs it, while the
 * implementations live in infrastructure — so the application never learns that
 * Home Front Command, HTTP or JSON exist. It knows only the three answers in
 * {@link AlertStatus}.
 *
 * That seam is not speculative. Without it the alert feature could only be
 * exercised from an Israeli network during an actual rocket alert: no test, no
 * demo, no way to debug. With it, a fake source makes the whole chain runnable
 * on a laptop in seconds.
 */
public interface AlertSource {

    /**
     * One method: to determine the current state.
     *
     * Implementations should return {@link AlertStatus#UNKNOWN} rather than
     * throw, but callers must not rely on that.
     */
    AlertStatus read();
}
