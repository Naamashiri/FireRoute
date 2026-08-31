package fireroute.infrastructure.alert;

import fireroute.application.alert.AlertMonitoringService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertPoller {
    private final AlertMonitoringService monitoringService;

    public AlertPoller(AlertMonitoringService monitoringService) {
        if (monitoringService == null) throw new IllegalArgumentException("monitoringService must not be null");
        this.monitoringService = monitoringService;
    }

    @Scheduled(
            initialDelayString =
                    "${fireroute.alerts.initial-delay-millis:2000}",
            fixedDelayString =
                    "${fireroute.alerts.poll-millis:1500}"
    )
    public void pollAlerts() {
        monitoringService.checkNow();
    }
}
