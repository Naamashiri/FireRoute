package loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import geom.GeoPoint;
import shelters.Shelter;
import shelters.ShelterRepository;

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

        // תמיכה הן במבנה FeatureCollection של GeoJSON והן במערך JSON רגיל
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
            // טיפול ב-GeoJSON Properties מול מבנה שטוח
            JsonNode properties = node.has("properties") ? node.get("properties") : node;
            
            // חילוץ מזהה מקלט
            String id = "unknown";
            if (properties.has("id")) {
                id = properties.get("id").asText();
            } else if (properties.has("SHELT_NUM")) {
                id = properties.get("SHELT_NUM").asText();
            } else if (properties.has("shelter_id")) {
                id = properties.get("shelter_id").asText();
            }

            // חילוץ כתובת
            String address = "ללא כתובת";
            if (properties.has("address")) {
                address = properties.get("address").asText();
            } else if (properties.has("STREET_NAME")) {
                address = properties.get("STREET_NAME").asText();
            }

            // חילוץ קואורדינטות (GeoJSON geometry או שדות lat/lon ישירים)
            double lat;
            double lon;

            if (node.has("geometry") && node.get("geometry").has("coordinates")) {
                JsonNode coords = node.get("geometry").get("coordinates");
                // ב-GeoJSON הסדר הוא [lon, lat] (x, y)
                lon = coords.get(0).asDouble();
                lat = coords.get(1).asDouble();
            } else if (properties.has("lat") && properties.has("lon")) {
                lat = properties.get("lat").asDouble();
                lon = properties.get("lon").asDouble();
            } else if (properties.has("y") && properties.has("x")) {
                lat = properties.get("y").asDouble();
                lon = properties.get("x").asDouble();
            } else {
                return null; // אין מידע גאוגרפי תקף
            }

            boolean accessible = properties.has("accessible") && properties.get("accessible").asBoolean();

            // יצירת GeoPoint עם (x=lon, y=lat)
            GeoPoint location = new GeoPoint(lon, lat);
            return new Shelter(id, address, location, accessible);

        } catch (Exception e) {
            // דילוג על רשומה פגומה כדי לא להפיל את כל הטעינה
            return null;
        }
    }
}