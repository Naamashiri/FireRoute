package fireroute.infrastructure.loader;

import org.junit.jupiter.api.Test;
import fireroute.domain.geo.GeoPoint;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reads a fixture from src/test/resources, never the production shelters.json.
 * The real file is data that will be replaced with a live municipal export; a
 * test that asserts on it would either break on that day or quietly stop
 * checking anything. Three shelters at known coordinates make the expected
 * answers exact instead of "more than zero".
 */
public class ShelterLoaderTest {

    private static final String FIXTURE = "shelters-test.json";

    @Test
    void loadsEveryShelterInTheFile() throws Exception {
        ShelterRepository repository = new ShelterLoader().loadFromResources(FIXTURE);

        assertEquals(3, repository.getAllShelters().size());
    }

    @Test
    void parsesIdAddressAndLocation() throws Exception {
        ShelterRepository repository = new ShelterLoader().loadFromResources(FIXTURE);

        Shelter first = repository.getAllShelters().get(0);

        assertEquals("S1", first.getId());
        assertEquals("Dizengoff 100, Tel Aviv", first.getAddress());
        // GeoPoint takes (x, y) = (longitude, latitude).
        assertEquals(34.7740, first.getLocation().x(), 1e-9);
        assertEquals(32.0780, first.getLocation().y(), 1e-9);
    }

    @Test
    void findsTheShelterClosestToAPoint() throws Exception {
        ShelterRepository repository = new ShelterLoader().loadFromResources(FIXTURE);

        // Sitting almost exactly on S1.
        Shelter nearest = repository.findNearestShelter(new GeoPoint(34.7741, 32.0781));

        assertNotNull(nearest);
        assertEquals("S1", nearest.getId());
    }

    @Test
    void findsEveryShelterWithinARadius() throws Exception {
        ShelterRepository repository = new ShelterLoader().loadFromResources(FIXTURE);

        // S3 is 817m from S1 and S2 is 1679m, so a 1km radius takes in S1 and S3.
        assertEquals(2, repository.findSheltersInRadius(new GeoPoint(34.7740, 32.0780), 1000).size());
    }

    @Test
    void rejectsAMissingResource() {
        ShelterLoader loader = new ShelterLoader();

        assertThrows(
                IllegalArgumentException.class,
                () -> loader.loadFromResources("no_such_file.json")
        );
    }
}
