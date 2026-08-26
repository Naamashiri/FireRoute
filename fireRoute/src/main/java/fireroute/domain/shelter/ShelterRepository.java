package fireroute.domain.shelter;

import fireroute.domain.geo.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class ShelterRepository {
    private final List<Shelter> shelters = new ArrayList<>();

    public void addShelter(Shelter shelter) {
        shelters.add(shelter);
    }

    public List<Shelter> getAllShelters() {
        return shelters;
    }

    /**
     * מוצא את המקלט הקרוב ביותר לנקודה נתונה
     */
    public Shelter findNearestShelter(GeoPoint point) {
        Shelter nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Shelter shelter : shelters) {
            double dist = point.distanceTo(shelter.getLocation());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = shelter;
            }
        }
        return nearest;
    }

    /**
     * מוצא את כל המקלטים ברדיוס מסוים (למשל עבור Endpoint שמציג מקלטים קרובים)
     */
    public List<Shelter> findSheltersInRadius(GeoPoint center, double radiusMeters) {
        List<Shelter> result = new ArrayList<>();
        for (Shelter shelter : shelters) {
            if (center.distanceTo(shelter.getLocation()) <= radiusMeters) {
                result.add(shelter);
            }
        }
        return result;
    }
}