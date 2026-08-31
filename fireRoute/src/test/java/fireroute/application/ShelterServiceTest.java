package fireroute.application;

import fireroute.domain.graph.GeoPoint;
import fireroute.domain.shelter.Shelter;
import fireroute.infrastructure.shelter.InMemoryShelterRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShelterServiceTest {
    private final Shelter shelter = new Shelter("S1", "Dizengoff 100", new GeoPoint(34.77, 32.08), true);
    private final ShelterService service = new ShelterService(new InMemoryShelterRepository(List.of(shelter)));

    @Test void returnsAllShelters() {
        assertThat(service.getAllShelters()).containsExactly(shelter);
    }

    @Test void returnsShelterById() {
        assertThat(service.getShelterById("S1")).isSameAs(shelter);
    }

    @Test void reportsMissingShelter() {
        assertThatThrownBy(() -> service.getShelterById("missing"))
                .isInstanceOf(ShelterNotFoundException.class);
    }
}
