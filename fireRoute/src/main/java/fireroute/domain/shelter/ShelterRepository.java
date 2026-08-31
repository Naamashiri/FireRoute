package fireroute.domain.shelter;

import java.util.List;
import java.util.Optional;

import fireroute.domain.graph.GeoPoint;

/** Domain-facing collection contract; storage details belong to infrastructure. */
public interface ShelterRepository {
    List<Shelter> findAll();
    Optional<Shelter> findById(String id);
    Optional<Shelter> findNearestTo(GeoPoint point);
    List<Shelter> findWithinRadius(GeoPoint center, double radiusMeters);
}
