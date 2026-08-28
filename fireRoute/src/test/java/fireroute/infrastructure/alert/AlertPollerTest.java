package fireroute.infrastructure.alert;

import fireroute.application.AlertReading;
import fireroute.application.AlertSource;
import fireroute.domain.alert.AlertState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests are the reason AlertSource is an interface.
 *
 * Without it the only way to see an alert reach the service would be to sit on
 * an Israeli network and wait for a real one. With it, the whole chain runs in
 * microseconds and the awkward cases — a source that throws, a source that
 * returns nothing — can be produced on demand, which is exactly where the
 * interesting decisions live.
 */
class AlertPollerTest {

    private AlertState alertState;

    @BeforeEach
    void setUp() {
        alertState = new AlertState("test-area");
    }

    private AlertPoller pollerReading(AlertSource source) {
        return new AlertPoller(source, alertState);
    }

    private static AlertSource returning(AlertReading reading) {
        return () -> reading;
    }

    // ---------------------------------------------------------
    // Validation
    // ---------------------------------------------------------

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void constructorRejectsNullDependencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlertPoller(null, alertState)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AlertPoller(returning(AlertReading.QUIET), null)
        );
    }

    // ---------------------------------------------------------
    // The two ordinary readings
    // ---------------------------------------------------------

    @Test
    @DisplayName("An ACTIVE reading should raise the alert")
    void activeReadingRaisesTheAlert() {
        pollerReading(returning(AlertReading.ACTIVE)).poll();

        assertTrue(alertState.isAlertActive());
    }

    @Test
    @DisplayName("A QUIET reading should clear a live alert")
    void quietReadingClearsTheAlert() {
        alertState.setAlertActive(true);

        pollerReading(returning(AlertReading.QUIET)).poll();

        assertFalse(
                alertState.isAlertActive(),
                "an alert that can start but never end would keep everyone in shelters forever"
        );
    }

    @Test
    @DisplayName("Repeated polls should keep reporting the same state")
    void repeatedPollsAreStable() {
        AlertPoller poller = pollerReading(returning(AlertReading.ACTIVE));

        poller.poll();
        poller.poll();
        poller.poll();

        assertTrue(alertState.isAlertActive());
    }

    // ---------------------------------------------------------
    // Failure — the decisions worth protecting
    // ---------------------------------------------------------

    @Test
    @DisplayName("An UNKNOWN reading should leave a live alert alone")
    void unknownReadingHoldsALiveAlert() {
        alertState.setAlertActive(true);

        pollerReading(returning(AlertReading.UNKNOWN)).poll();

        assertTrue(
                alertState.isAlertActive(),
                "clearing on a failed check would turn a network fault into an all-clear"
        );
    }

    @Test
    @DisplayName("An UNKNOWN reading should not invent an alert either")
    void unknownReadingDoesNotInventAnAlert() {
        pollerReading(returning(AlertReading.UNKNOWN)).poll();

        assertFalse(alertState.isAlertActive());
    }

    @Test
    @DisplayName("A source that throws should not stop the poller or change the state")
    void throwingSourceIsSurvived() {
        alertState.setAlertActive(true);

        AlertSource broken = () -> {
            throw new IllegalStateException("feed exploded");
        };

        AlertPoller poller = pollerReading(broken);

        // An exception escaping a @Scheduled method stops Spring rescheduling it,
        // and the service would go blind without a single error surfacing.
        poller.poll();
        poller.poll();

        assertTrue(alertState.isAlertActive());
    }

    @Test
    @DisplayName("A source returning null should be treated as UNKNOWN")
    void nullReadingIsTreatedAsUnknown() {
        alertState.setAlertActive(true);

        pollerReading(() -> null).poll();

        assertTrue(alertState.isAlertActive());
    }

    // ---------------------------------------------------------
    // Recovery
    // ---------------------------------------------------------

    @Test
    @DisplayName("The state should follow the source once it recovers")
    void stateFollowsTheSourceAfterRecovery() {
        MutableSource source = new MutableSource();
        AlertPoller poller = pollerReading(source);

        source.reading = AlertReading.ACTIVE;
        poller.poll();
        assertTrue(alertState.isAlertActive());

        source.reading = AlertReading.UNKNOWN;
        poller.poll();
        assertTrue(alertState.isAlertActive(), "held through the outage");

        source.reading = AlertReading.QUIET;
        poller.poll();
        assertFalse(alertState.isAlertActive(), "cleared once the source could answer again");
    }

    private static final class MutableSource implements AlertSource {
        private AlertReading reading = AlertReading.QUIET;

        @Override
        public AlertReading read() {
            return reading;
        }
    }
}
