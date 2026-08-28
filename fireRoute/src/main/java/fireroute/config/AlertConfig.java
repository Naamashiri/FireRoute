package fireroute.config;

import fireroute.application.AlertSource;
import fireroute.infrastructure.alert.OrefAlertSource;
import fireroute.infrastructure.alert.SimulatedAlertSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public AlertSource orefAlertSource(
            @Value("${fireroute.alerts.oref-url:https://www.oref.org.il/WarningMessages/alert/alerts.json}")
            String endpoint
    ) {
        return new OrefAlertSource(endpoint);
    }
}
