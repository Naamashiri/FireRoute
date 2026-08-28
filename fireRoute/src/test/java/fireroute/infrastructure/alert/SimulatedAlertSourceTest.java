package fireroute.infrastructure.alert;

import fireroute.application.AlertReading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulatedAlertSourceTest {

    @Test
    @DisplayName("Should start quiet")
    void startsQuiet() {
        assertEquals(AlertReading.QUIET, new SimulatedAlertSource().read());
    }

    @Test
    @DisplayName("Should report whatever was set")
    void reportsWhatWasSet() {
        SimulatedAlertSource source = new SimulatedAlertSource();

        source.set(AlertReading.ACTIVE);
        assertEquals(AlertReading.ACTIVE, source.read());

        source.set(AlertReading.QUIET);
        assertEquals(AlertReading.QUIET, source.read());
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
