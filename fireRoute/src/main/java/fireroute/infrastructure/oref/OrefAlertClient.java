package fireroute.infrastructure.oref;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
/*  */

public class OrefAlertClient {
    
    private final HttpClient httpClient;
    private final String alertUrl;

    public OrefAlertClient(String alertUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.alertUrl = alertUrl;
    }

    public boolean isAlertActive() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(alertUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Assuming the response body contains a JSON object with a field "alertActive"
            return responseBody.contains("\"alertActive\":true");
        } catch (Exception e) {
            // Handle exceptions (e.g., network issues, parsing errors)
            e.printStackTrace();
            return false; // Default to no alert if there's an error
        }
    }
}
