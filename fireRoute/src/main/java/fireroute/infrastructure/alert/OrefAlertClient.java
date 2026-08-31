package fireroute.infrastructure.alert;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
/*Send an HTTP request to the Oref alert endpoint
and return the alert status */

public class OrefAlertClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final URI endpoint;
    private final HttpClient httpClient;

    public OrefAlertClient(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint must not be null or blank");
        }

        URI parsedEndpoint = URI.create(endpoint);
        if (!"https".equalsIgnoreCase(parsedEndpoint.getScheme()) || parsedEndpoint.getHost() == null) {
            throw new IllegalArgumentException("endpoint must be an absolute HTTPS URL");
        }

        this.endpoint = parsedEndpoint;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public HttpResponse<String> fetchAlerts() throws Exception {
        return sendRequest();
    }

    private HttpResponse<String> sendRequest() throws Exception {
        HttpRequest request = buildRequest();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildRequest() {
        return HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Referer", "https://www.oref.org.il/")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (FireRoute)")
                .GET()
                .build();
    }
}
