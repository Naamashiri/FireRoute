package fireroute.api.controller;

import fireroute.api.dto.AlertStatusResponse;
import fireroute.api.mapper.AlertStatusMapper;
import fireroute.domain.alert.AlertState;
import fireroute.domain.alert.AlertStatus;
import fireroute.infrastructure.alert.SimulatedAlertSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Raises and clears an alert by hand, so the feature can be demonstrated
 * without waiting for a real one.
 *
 * It is the one place the api layer touches infrastructure, which is why it
 * sits in a class of its own, conditional on the same property that selects the
 * simulated source. Point the service at the real feed and this bean — and with
 * it the endpoint — stops existing. Nothing else in the api layer depends on a
 * source implementation.
 *
 * The change is applied to the source rather than to AlertState, so the next
 * poll carries it through the same path a real alert takes. The response
 * therefore still shows the OLD state — the new one appears within one poll
 * interval.
 */
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(
        name = "fireroute.alerts.source",
        havingValue = "simulated",
        matchIfMissing = true
)
public class AlertSimulationController {

    private final SimulatedAlertSource simulatedAlertSource;
    private final AlertState alertState;
    private final AlertStatusMapper mapper;

    public AlertSimulationController(
            SimulatedAlertSource simulatedAlertSource,
            AlertState alertState,
            AlertStatusMapper mapper
    ) {
        if (simulatedAlertSource == null || alertState == null || mapper == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }
        this.simulatedAlertSource = simulatedAlertSource;
        this.alertState = alertState;
        this.mapper = mapper;
    }

    @PostMapping("/alerts/simulate")
    public AlertStatusResponse simulate(@RequestParam boolean active) {
        simulatedAlertSource.set(active ? AlertStatus.ACTIVE : AlertStatus.QUIET);

        return mapper.toResponse(alertState);
    }
}
