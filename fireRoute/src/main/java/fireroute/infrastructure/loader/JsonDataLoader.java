package fireroute.infrastructure.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads a graph from JSON.
 *
 * Reading from the classpath is the form the application uses, so that the data
 * still resolves once the project is packaged as a JAR, where no file path into
 * src/main/resources exists any more. The file-path form is kept for tests and
 * for pointing the service at data outside the artifact.
 */
public class JsonDataLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Loads the graph from a file on the classpath, e.g. "static/map.json".
     */
    public Graph loadGraphFromResources(String resourcePath) throws IOException {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("resourcePath must be non-null and non-empty");
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource file not found: " + resourcePath);
            }
            return loadGraph(is);
        }
    }

    /**
     * Loads the graph from a file on disk.
     */
    public Graph loadGraph(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must be non-null and non-empty");
        }

        try (InputStream is = new FileInputStream(new File(filePath))) {
            return loadGraph(is);
        }
    }

    /**
     * Parses a graph from an already-open stream. The two public entry points
     * differ only in where the stream comes from, so the parsing lives here once.
     */
    public Graph loadGraph(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must be non-null");
        }

        Graph graph = new Graph();

        JsonNode root = mapper.readTree(inputStream);
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

            graph.addRoadSegment(fromId, toId, travelTime);
        }

        return graph;
    }
}
