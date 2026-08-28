package fireroute.api.controller;

import fireroute.api.dto.AlertStatusResponse;
import fireroute.domain.alert.AlertState;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a client find out whether an alert is live.
 *
 * A plain GET the client polls, rather than a pushed stream: the alert reaches
 * this service through polling anyway, so the delay is bounded by the poll
 * interval either way and a second delivery mechanism would add machinery
 * without shortening it.
 */
@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertState alertState;

    public AlertController(AlertState alertState) {
        if (alertState == null) {
            throw new IllegalArgumentException("alertState must not be null");
        }
        this.alertState = alertState;
    }

    @GetMapping("/alerts/status")
    public AlertStatusResponse status() {
        return new AlertStatusResponse(
                alertState.getAreaId(),
                alertState.isAlertActive()
        );
    }
}
