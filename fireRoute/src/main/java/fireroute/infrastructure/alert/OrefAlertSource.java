package fireroute.infrastructure.alert;

import fireroute.application.alert.AlertSource;
import fireroute.domain.alert.AlertStatus;

import java.net.http.HttpResponse;

public class OrefAlertSource implements AlertSource {

    private final OrefAlertClient client;
    private final OrefAlertParser parser;

    public OrefAlertSource(
            OrefAlertClient client,
            OrefAlertParser parser
    ) {
        if (client == null) {
            throw new IllegalArgumentException(
                    "client must not be null"
            );
        }

        if (parser == null) {
            throw new IllegalArgumentException(
                    "parser must not be null"
            );
        }

        this.client = client;
        this.parser = parser;
    }

    @Override
    public AlertStatus read() {
        try {
            HttpResponse<String> response =
                    client.fetchAlerts();

            if (response.statusCode() != 200) {
                return AlertStatus.UNKNOWN;
            }

            return parser.parse(response.body());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return AlertStatus.UNKNOWN;

        } catch (Exception exception) {
            return AlertStatus.UNKNOWN;
        }
    }
}