package fireroute.application.alert;

import fireroute.domain.alert.AlertState;
import fireroute.domain.alert.AlertStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AlertMonitoringServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-29T07:00:00Z");
    private final AlertState state = new AlertState("test-area");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test void successfulReadingUpdatesStatusAndAllSuccessTimes() {
        var snapshot = service(() -> AlertStatus.ACTIVE).checkNow();

        assertThat(snapshot.status()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(snapshot.lastKnownStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(snapshot.alertActive()).isTrue();
        assertThat(snapshot.lastAttemptAt()).isEqualTo(NOW);
        assertThat(snapshot.lastSuccessfulReadAt()).isEqualTo(NOW);
        assertThat(snapshot.lastChangedAt()).isEqualTo(NOW);
    }

    @Test void unknownKeepsTheLastKnownAlertButReportsTheFailedAttempt() {
        state.record(AlertStatus.ACTIVE, NOW.minusSeconds(5));

        var snapshot = service(() -> AlertStatus.UNKNOWN).checkNow();

        assertThat(snapshot.status()).isEqualTo(AlertStatus.UNKNOWN);
        assertThat(snapshot.lastKnownStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(snapshot.alertActive()).isTrue();
        assertThat(snapshot.lastAttemptAt()).isEqualTo(NOW);
        assertThat(snapshot.lastSuccessfulReadAt()).isEqualTo(NOW.minusSeconds(5));
    }

    @Test void exceptionAndNullAreBothUnknown() {
        assertThat(service(() -> { throw new IllegalStateException("down"); }).checkNow().status())
                .isEqualTo(AlertStatus.UNKNOWN);
        assertThat(service(() -> null).checkNow().status()).isEqualTo(AlertStatus.UNKNOWN);
    }

    @Test void repeatedValueDoesNotChangeLastChangedAt() {
        state.record(AlertStatus.QUIET, NOW.minusSeconds(5));
        var snapshot = service(() -> AlertStatus.QUIET).checkNow();
        assertThat(snapshot.lastChangedAt()).isEqualTo(NOW.minusSeconds(5));
        assertThat(snapshot.lastSuccessfulReadAt()).isEqualTo(NOW);
    }

    private AlertMonitoringService service(AlertSource source) {
        return new AlertMonitoringService(source, state, clock);
    }
}
