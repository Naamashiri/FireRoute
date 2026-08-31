package fireroute.infrastructure.alert;

import fireroute.domain.alert.AlertStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrefAlertSourceTest {

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void constructorRejectsNullDependencies() {
        OrefAlertClient client = mock(OrefAlertClient.class);
        OrefAlertParser parser = mock(OrefAlertParser.class);

        assertThrows(IllegalArgumentException.class, () -> new OrefAlertSource(null, parser));
        assertThrows(IllegalArgumentException.class, () -> new OrefAlertSource(client, null));
    }

    @Test
    void successfulResponseIsParsed() throws Exception {
        OrefAlertClient client = mock(OrefAlertClient.class);
        OrefAlertParser parser = mock(OrefAlertParser.class);
        HttpResponse<String> response = response(200, "{\"data\":[]}");

        when(client.fetchAlerts()).thenReturn(response);
        when(parser.parse(response.body())).thenReturn(AlertStatus.QUIET);

        assertEquals(AlertStatus.QUIET, new OrefAlertSource(client, parser).read());
    }

    @Test
    void non200ResponseMeansUnknown() throws Exception {
        OrefAlertClient client = mock(OrefAlertClient.class);
        OrefAlertParser parser = mock(OrefAlertParser.class);
        HttpResponse<String> response = response(503, "unavailable");
        when(client.fetchAlerts()).thenReturn(response);

        assertEquals(AlertStatus.UNKNOWN, new OrefAlertSource(client, parser).read());
    }

    @Test
    void networkFailureMeansUnknown() throws Exception {
        OrefAlertClient client = mock(OrefAlertClient.class);
        OrefAlertParser parser = mock(OrefAlertParser.class);
        when(client.fetchAlerts()).thenThrow(new IOException("network down"));

        assertEquals(AlertStatus.UNKNOWN, new OrefAlertSource(client, parser).read());
    }

    @Test
    void interruptionMeansUnknownAndRestoresInterruptFlag() throws Exception {
        assertFalse(Thread.currentThread().isInterrupted());

        OrefAlertClient client = mock(OrefAlertClient.class);
        OrefAlertParser parser = mock(OrefAlertParser.class);
        when(client.fetchAlerts()).thenThrow(new InterruptedException("shutdown"));

        assertEquals(AlertStatus.UNKNOWN, new OrefAlertSource(client, parser).read());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
