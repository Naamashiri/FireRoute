package fireroute.domain.alert;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertSnapshotTest {
    @Test void initialSnapshotIsUnknownInactiveAndStale() {
        AlertSnapshot snapshot = AlertSnapshot.initial();
        assertThat(snapshot.status()).isEqualTo(AlertStatus.UNKNOWN);
        assertThat(snapshot.alertActive()).isFalse();
        assertThat(snapshot.isStale(Instant.now(), Duration.ofSeconds(10))).isTrue();
    }

    @Test void successfulRecentSnapshotIsNotStale() {
        Instant success = Instant.parse("2026-08-29T07:00:00Z");
        AlertSnapshot snapshot = new AlertSnapshot(
                AlertStatus.QUIET, AlertStatus.QUIET, success, success, success);
        assertThat(snapshot.isStale(success.plusSeconds(9), Duration.ofSeconds(10))).isFalse();
        assertThat(snapshot.isStale(success.plusSeconds(10), Duration.ofSeconds(10))).isTrue();
    }
}
