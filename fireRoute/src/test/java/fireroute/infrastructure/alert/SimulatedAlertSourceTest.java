package fireroute.infrastructure.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fireroute.domain.alert.AlertStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulatedAlertSourceTest {

    @Test
    @DisplayName("Should start quiet")
    void startsQuiet() {
        assertEquals(AlertStatus.QUIET, new SimulatedAlertSource().read());
    }

    @Test
    @DisplayName("Should report whatever was set")
    void reportsWhatWasSet() {
        SimulatedAlertSource source = new SimulatedAlertSource();

        source.set(AlertStatus.ACTIVE);
        assertEquals(AlertStatus.ACTIVE, source.read());

        source.set(AlertStatus.QUIET);
        assertEquals(AlertStatus.QUIET, source.read());
    }

    @Test
    @DisplayName("Should reject a null reading")
    void rejectsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SimulatedAlertSource().set(null)
        );
    }
}
