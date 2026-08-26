package fireroute.infrastructure.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fireroute.domain.geo.GeoPoint;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;

import java.io.IOException;
import java.io.InputStream;

public class ShelterLoader {

    private final ObjectMapper objectMapper;

    public ShelterLoader() {
        this.objectMapper = new ObjectMapper();
    }

    public ShelterLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * טוען מקלטים מתוך קובץ שנמצא ב-src/main/resources
     * @param resourcePath הנתיב לקובץ (למשל "shelters.json")
     * @return ShelterRepository מאוכלס בכל המקלטים
     */
    public ShelterRepository loadFromResources(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource file not found: " + resourcePath);
            }
            return loadFromInputStream(is);
        }
    }

    /**
     * קורא את ה-JSON ומחלץ את המקלטים
     */
    public ShelterRepository loadFromInputStream(InputStream inputStream) throws IOException {
        ShelterRepository repository = new ShelterRepository();
        JsonNode root = objectMapper.readTree(inputStream);

        JsonNode features = root.has("features") ? root.get("features") : root;

        if (features.isArray()) {
            for (JsonNode item : features) {
                Shelter shelter = parseShelter(item);
                if (shelter != null) {
                    repository.addShelter(shelter);
                }
            }
        }

        return repository;
    }

    private Shelter parseShelter(JsonNode node) {
        try {
            JsonNode properties = node.has("properties") ? node.get("properties") : node;

            // 1. חילוץ מזהה מקלט (מתאים ל-ms_miklat או UniqueId של עיריית תל אביב)
            String id = "unknown";
            if (properties.has("UniqueId")) {
                id = properties.get("UniqueId").asText();
            } else if (properties.has("ms_miklat")) {
                id = String.valueOf(properties.get("ms_miklat").asText());
            } else if (properties.has("id")) {
                id = properties.get("id").asText();
            }

            // 2. חילוץ כתובת (מתאים ל-Full_Address או shem של העירייה)
            String address = "ללא כתובת";
            if (properties.has("Full_Address") && !properties.get("Full_Address").asText().isBlank()) {
                address = properties.get("Full_Address").asText().trim();
            } else if (properties.has("shem") && !properties.get("shem").isNull()) {
                address = properties.get("shem").asText().trim();
            }

            // 3. חילוץ קואורדינטות (ב-GeoJSON של ArcGIS: coordinates = [lon, lat])
            double lat;
            double lon;

            if (node.has("geometry") && node.get("geometry").has("coordinates")) {
                JsonNode coords = node.get("geometry").get("coordinates");
                lon = coords.get(0).asDouble();
                lat = coords.get(1).asDouble();
            } else if (properties.has("lat") && properties.has("lon")) {
                lat = properties.get("lat").asDouble();
                lon = properties.get("lon").asDouble();
            } else {
                return null;
            }

            // 4. בדיקת נגישות / כשרות לשימוש
            boolean accessible = properties.has("miklat_mungash") && !properties.get("miklat_mungash").isNull();

            // יצירת GeoPoint עם (lon, lat)
            GeoPoint location = new GeoPoint(lon, lat);
            return new Shelter(id, address, location, accessible);

        } catch (Exception e) {
            return null;
        }
    }
}
