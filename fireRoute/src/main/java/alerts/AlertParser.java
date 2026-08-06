package alerts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * מפרק את תשובת פיקוד העורף (מחרוזת JSON) לרשימת Alert.
 * מחזיר רשימה ריקה כשאין אזעקה (גוף ריק / BOM / [] / {}).
 */
public class AlertParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Alert> parse(String body) {
        //CASE1- no alerts
        if (body == null) {
            return List.of();
        }
        String clean = body.strip();
        if (clean.isEmpty() || clean.equals("[]") || clean.equals("{}")) {
            return List.of();
        }
        if (!clean.isEmpty() && clean.charAt(0) == '\uFEFF') {
            clean = clean.substring(1).strip(); // הסרת BOM
        }
        

        JsonNode root;
        try {
            root = mapper.readTree(clean);
        } catch (Exception e) {
            return List.of();
        }

        String id       = text(root, "id");
        String title    = text(root, "title");
        String cat = text(root, "cat");
        List<String> affectedAreas = new ArrayList<>();

        if(cat.equals("1")){
        List<Alert> alerts = new ArrayList<>();
        JsonNode dataNode = root.get("data");
        if (dataNode != null && dataNode.isArray()) {
            for (JsonNode areaNode : dataNode) {
                String area = areaNode.asText();
                if (area != null && !area.isBlank()) {
                    affectedAreas.add(area);
                }
            }
        }
        return List.of(new Alert(id, affectedAreas, System.currentTimeMillis()));
        } else {
            return List.of();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null) ? null : v.asText();
    }
}