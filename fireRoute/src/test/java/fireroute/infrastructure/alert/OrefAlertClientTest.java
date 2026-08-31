package fireroute.infrastructure.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrefAlertClientTest {

    @Test
    void constructorAcceptsHttpsEndpoint() {
        assertDoesNotThrow(() -> new OrefAlertClient("https://www.oref.org.il/alerts.json"));
    }

    @Test
    void constructorRejectsMissingOrInsecureEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> new OrefAlertClient(null));
        assertThrows(IllegalArgumentException.class, () -> new OrefAlertClient(" "));
        assertThrows(IllegalArgumentException.class,
                () -> new OrefAlertClient("http://www.oref.org.il/alerts.json"));
    }
}
