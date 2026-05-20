package data;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Graph;
import model.Junction;

import java.io.File;
import java.io.IOException;


/**
 * Loads a graph from a JSON file.
 */
public class JsonDataLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public Graph loadGraph(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must be non-null and non-empty");
        }

        Graph graph = new Graph();

        JsonNode root = mapper.readTree(new File(filePath));
        JsonNode junctionsNode = root.get("junctions");
        JsonNode roadsNode = root.get("roads");

        if (junctionsNode == null || !junctionsNode.isArray()) {
            throw new IllegalArgumentException("JSON must contain an array field 'junctions'");
        }
        if (roadsNode == null || !roadsNode.isArray()) {
            throw new IllegalArgumentException("JSON must contain an array field 'roads'");
        }

        // 1. Load junctions
        for (JsonNode jNode : junctionsNode) {
            String id = jNode.get("id").asText();
            double x = jNode.get("x").asDouble();
            double y = jNode.get("y").asDouble();
            boolean isShelter = jNode.get("isShelter").asBoolean();

            Junction junction = new Junction(id, x, y, isShelter);
            graph.addJunction(junction);
        }

        // 2. Load roads
        for (JsonNode rNode : roadsNode) {
            String fromId = rNode.get("from").asText();
            String toId = rNode.get("to").asText();
            double travelTime = rNode.get("travelTime").asDouble();

            // optional field; default = 0.0
            double riskLevel = rNode.has("riskLevel") ? rNode.get("riskLevel").asDouble() : 0.0;

            graph.addRoadSegment(fromId, toId, travelTime, riskLevel);
        }

        return graph;
    }
}