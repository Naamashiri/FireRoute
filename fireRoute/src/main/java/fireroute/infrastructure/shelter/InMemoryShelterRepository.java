package fireroute.infrastructure.shelter;

import fireroute.domain.graph.GeoPoint;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;

import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryShelterRepository implements ShelterRepository {
    private final Map<String, Shelter> sheltersById;

    public InMemoryShelterRepository(List<Shelter> shelters) {
        if (shelters == null) throw new IllegalArgumentException("shelters must not be null");
        Map<String, Shelter> indexed = new LinkedHashMap<>();
        for (Shelter shelter : shelters) {
            if (shelter == null) throw new IllegalArgumentException("shelters must not contain null");
            if (indexed.putIfAbsent(shelter.getId(), shelter) != null) {
                throw new IllegalArgumentException("duplicate shelter id: " + shelter.getId());
            }
        }
        sheltersById = Collections.unmodifiableMap(indexed);
    }

    @Override public List<Shelter> findAll() { return List.copyOf(sheltersById.values()); }

    @Override public Optional<Shelter> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(sheltersById.get(id));
    }

    @Override public Optional<Shelter> findNearestTo(GeoPoint point) {
        if (point == null) throw new IllegalArgumentException("point must not be null");
        return sheltersById.values().stream()
                .min(Comparator.comparingDouble(shelter -> point.distanceTo(shelter.getLocation())));
    }

    @Override public List<Shelter> findWithinRadius(GeoPoint center, double radiusMeters) {
        if (center == null) throw new IllegalArgumentException("center must not be null");
        if (radiusMeters < 0) throw new IllegalArgumentException("radiusMeters must not be negative");
        return sheltersById.values().stream()
                .filter(shelter -> center.distanceTo(shelter.getLocation()) <= radiusMeters)
                .toList();
    }
}
