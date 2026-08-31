package fireroute.application;

import fireroute.domain.graph.GeoPoint;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;

import java.util.List;

public final class ShelterService {
    private final ShelterRepository repository;

    public ShelterService(ShelterRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
        this.repository = repository;
    }

    public List<Shelter> getAllShelters() { return repository.findAll(); }

    public Shelter getShelterById(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        return repository.findById(id).orElseThrow(() -> new ShelterNotFoundException(id));
    }

    public Shelter getNearestShelter(GeoPoint point) {
        return repository.findNearestTo(point)
                .orElseThrow(() -> new ShelterNotFoundException("No shelters are available"));
    }
}
