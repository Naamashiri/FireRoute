package fireroute.infrastructure.alert;

import fireroute.application.alert.AlertMonitoringService;
import fireroute.domain.alert.AlertSnapshot;
import fireroute.domain.alert.AlertStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertPollerTest {
    @Test void delegatesOneScheduledTickToTheApplicationService() {
        AlertMonitoringService service = mock(AlertMonitoringService.class);
        AlertPoller poller = new AlertPoller(service);

        poller.pollAlerts();

        verify(service).checkNow();
    }

    @Test void rejectsNullService() {
        assertThatThrownBy(() -> new AlertPoller(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
