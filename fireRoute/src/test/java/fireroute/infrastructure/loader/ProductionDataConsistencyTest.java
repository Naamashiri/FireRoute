package fireroute.infrastructure.loader;

import fireroute.domain.graph.GeoPoint;
import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import fireroute.domain.graph.RoadSegment;
import fireroute.domain.routing.ShelterMap;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Fails the build early when the production map and shelter files contradict themselves. */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class ProductionDataConsistencyTest {
    @Autowired Graph graph;
    @Autowired ShelterMap shelterMap;
    @Autowired ShelterRepository shelterRepository;

    @Test void graphContainsValidUniqueJunctionsAndRoads() {
        assertThat(graph.size()).as("production graph must not be empty").isGreaterThan(1_000);
        Set<String> junctionIds = new HashSet<>();

        for (Junction junction : graph.getJunctions()) {
            assertThat(junctionIds.add(junction.getId()))
                    .as("junction id must be unique: %s", junction.getId()).isTrue();
            assertValidCoordinates(junction.getX(), junction.getY(), "junction " + junction.getId());

            for (RoadSegment road : junction.getOutGoingRoads()) {
                assertThat(road.getSourceJunction()).as("road source must match its owner").isSameAs(junction);
                assertThat(graph.getJunction(road.getTargetJunction().getId()))
                        .as("road target must belong to the graph").isSameAs(road.getTargetJunction());
                assertThat(road.getTravelTime()).as("road travel time must be finite and positive")
                        .isFinite().isPositive();
            }
        }
    }

    @Test void municipalSheltersHaveUniqueIdsAndValidCoordinates() {
        assertThat(shelterRepository.findAll()).as("production shelters must not be empty").isNotEmpty();
        Set<String> shelterIds = new HashSet<>();

        for (Shelter shelter : shelterRepository.findAll()) {
            assertThat(shelterIds.add(shelter.getId()))
                    .as("shelter id must be unique: %s", shelter.getId()).isTrue();
            assertValidCoordinates(shelter.getLocation().lon(), shelter.getLocation().lat(),
                    "shelter " + shelter.getId());
        }
    }

    @Test void shelterMapWasComputedForEveryJunctionAndLinksEveryShelterNode() {
        assertThat(graph.getShelters()).as("routing graph must mark shelter destinations").isNotEmpty();
        assertThat(shelterMap.getDistToShelterMap()).hasSize(graph.size());

        for (Junction shelterNode : graph.getShelters()) {
            assertThat(shelterMap.getDistanceToShelter(shelterNode))
                    .as("shelter node %s must route to itself", shelterNode.getId()).isZero();
            assertThat(shelterMap.getNearestShelter(shelterNode)).isSameAs(shelterNode);
            assertThat(shelterRepository.findNearestTo(new GeoPoint(shelterNode.getX(), shelterNode.getY())))
                    .as("shelter node %s must link to municipal shelter data", shelterNode.getId()).isPresent();
        }
    }

    private static void assertValidCoordinates(Double longitude, Double latitude, String label) {
        assertThat(longitude).as(label + " longitude").isNotNull().isFinite().isBetween(-180.0, 180.0);
        assertThat(latitude).as(label + " latitude").isNotNull().isFinite().isBetween(-90.0, 90.0);
    }
}
