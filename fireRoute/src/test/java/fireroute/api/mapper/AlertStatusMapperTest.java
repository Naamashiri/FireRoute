package fireroute.api.mapper;

import fireroute.domain.alert.AlertState;
import fireroute.domain.alert.AlertStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AlertStatusMapperTest {
    private static final Instant NOW = Instant.parse("2026-08-29T07:00:10Z");

    @Test void exposesStatusLastKnownStateAndTimestamps() {
        AlertState state = new AlertState("tel-aviv-center");
        Instant success = NOW.minusSeconds(3);
        state.record(AlertStatus.ACTIVE, success);
        state.record(AlertStatus.UNKNOWN, NOW);

        var response = mapper(10).toResponse(state);

        assertThat(response.status()).isEqualTo(AlertStatus.UNKNOWN);
        assertThat(response.lastKnownStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(response.alertActive()).isTrue();
        assertThat(response.simulated()).isTrue();
        assertThat(response.stale()).isFalse();
        assertThat(response.lastAttemptAt()).isEqualTo(NOW);
        assertThat(response.lastSuccessfulReadAt()).isEqualTo(success);
        assertThat(response.lastChangedAt()).isEqualTo(success);
    }

    @Test void marksInformationStaleAtTheConfiguredBoundary() {
        AlertState state = new AlertState("tel-aviv-center");
        state.record(AlertStatus.QUIET, NOW.minusSeconds(10));
        assertThat(mapper(10).toResponse(state).stale()).isTrue();
    }

    private AlertStatusMapper mapper(long staleAfterSeconds) {
        return new AlertStatusMapper(Clock.fixed(NOW, ZoneOffset.UTC), staleAfterSeconds, "simulated");
    }
}
