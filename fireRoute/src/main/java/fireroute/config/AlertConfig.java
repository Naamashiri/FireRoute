package fireroute.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fireroute.application.alert.AlertSource;
import fireroute.application.alert.AlertMonitoringService;
import fireroute.domain.alert.AlertState;
import fireroute.infrastructure.alert.OrefAlertClient;
import fireroute.infrastructure.alert.OrefAlertParser;
import fireroute.infrastructure.alert.OrefAlertSource;
import fireroute.infrastructure.alert.SimulatedAlertSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.time.Clock;

/**
 * Chooses where alerts come from.
 *
 * Exactly one AlertSource bean exists at a time, selected by
 * {@code fireroute.alerts.source}, so nothing downstream needs a qualifier or a
 * primary marker — the poller simply asks for an AlertSource and gets the one
 * that is configured.
 *
 * Separate from AppConfig on purpose: that class assembles the routing domain,
 * this one wires an external feed. Two concerns, two files.
 *
 * The default is the simulator. A service that starts up pointing at an
 * undocumented endpoint it may not be able to reach is a poor default; a
 * developer who wants the real feed can say so.
 */
@Configuration
public class AlertConfig {

    @Bean
    public Clock alertClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AlertMonitoringService alertMonitoringService(
            AlertSource source,
            AlertState state,
            Clock alertClock
    ) {
        return new AlertMonitoringService(source, state, alertClock);
    }

    @Bean
    @ConditionalOnProperty(
            name = "fireroute.alerts.source",
            havingValue = "simulated",
            matchIfMissing = true
    )
    public SimulatedAlertSource simulatedAlertSource() {
        return new SimulatedAlertSource();
    }

    @Bean
    @ConditionalOnProperty(name = "fireroute.alerts.source", havingValue = "oref")
    public OrefAlertClient orefAlertClient(
            @Value("${fireroute.alerts.oref-url:https://www.oref.org.il/WarningMessages/alert/alerts.json}")
            String endpoint
    ) {
        return new OrefAlertClient(endpoint);
    }

    @Bean
    @ConditionalOnProperty(name = "fireroute.alerts.source", havingValue = "oref")
    public OrefAlertParser orefAlertParser(
            ObjectMapper objectMapper,
            @Value("${fireroute.alerts.supported-areas}") String supportedAreas
    ) {
        return new OrefAlertParser(
                objectMapper,
                Arrays.asList(supportedAreas.split("\\|"))
        );
    }

    @Bean
    @ConditionalOnProperty(name = "fireroute.alerts.source", havingValue = "oref")
    public AlertSource orefAlertSource(
            OrefAlertClient client,
            OrefAlertParser parser
    ) {
        return new OrefAlertSource(client, parser);
    }
}
