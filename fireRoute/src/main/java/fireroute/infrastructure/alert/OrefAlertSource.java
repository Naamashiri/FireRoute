package fireroute.infrastructure.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fireroute.application.AlertReading;
import fireroute.application.AlertSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Reads the alert feed that the Home Front Command website itself calls.
 *
 * There is no official public API. This endpoint is undocumented, unannounced,
 * reachable only from inside Israel, and has changed before without notice —
 * which is why every failure here becomes {@link AlertReading#UNKNOWN} rather
 * than an exception or a quiet "no alert", and why the whole class sits behind
 * the AlertSource interface where it can be swapped out.
 *
 * Reading the response:
 * an empty body means no alert; a body with a non-empty "data" array means one
 * is live. Anything else — a timeout, a non-200, HTML served instead of JSON,
 * a shape that no longer parses — is UNKNOWN.
 *
 * KNOWN LIMITATION: the feed is national. A non-empty response means an alert
 * somewhere in Israel, not necessarily in this service's area. Narrowing it
 * needs the list of settlement names belonging to the area, matched against the
 * "data" entries; until then this reports the country, not the neighbourhood.
 */
public class OrefAlertSource implements AlertSource {

    private static final Logger log = LoggerFactory.getLogger(OrefAlertSource.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final URI endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrefAlertSource(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint must be non-null and non-blank");
        }

        this.endpoint = URI.create(endpoint);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public AlertReading read() {
        try {
            HttpResponse<String> response = httpClient.send(
                    buildRequest(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.debug("Alert feed returned HTTP {}", response.statusCode());
                return AlertReading.UNKNOWN;
            }

            return interpret(response.body());

        } catch (InterruptedException interrupted) {
            // Restore the flag rather than swallowing it: something is trying to
            // shut this thread down and the scheduler needs to hear about it.
            Thread.currentThread().interrupt();
            return AlertReading.UNKNOWN;

        } catch (Exception exception) {
            log.debug("Alert feed unreachable", exception);
            return AlertReading.UNKNOWN;
        }
    }

    /**
     * The headers are not decoration. The endpoint serves the website's own
     * front end and returns nothing useful to a request that does not look like
     * it came from there.
     */
    private HttpRequest buildRequest() {
        return HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Referer", "https://www.oref.org.il/")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (FireRoute)")
                .GET()
                .build();
    }

    private AlertReading interpret(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return AlertReading.QUIET;
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray() || !data.iterator().hasNext()) {
            return AlertReading.QUIET;
        }

        return AlertReading.ACTIVE;
    }
}
