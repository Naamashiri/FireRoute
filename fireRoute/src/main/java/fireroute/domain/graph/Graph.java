package fireroute.domain.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


/**
 * Represents a road network as a directed graph:
 * vertices are {@link Junction} objects and edges are {@link RoadSegment} objects.
 *
 * <p>Each {@code Junction} holds its outgoing {@code RoadSegment}s (adjacency list).</p>
 */
public class Graph {

    // Key = junction id, Value = Junction object
    private final Map<String, Junction> junctions;

    // Construct an empty Graph.
    public Graph() {
        this.junctions = new HashMap<>();
    }

    /**
     * Adds a junction to the graph.
     * @param junction != null && junction.id != null && !junction.id.isEmpty()
     * @throws IllegalArgumentException if junction is null or has invalid id,
     *                                  or if a junction with the same id already exists.
     */
    public void addJunction(Junction junction) {
        if (junction == null) {
            throw new IllegalArgumentException("junction must be non-null");
        }
        String id = junction.getId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("junction id must be non-null and non-empty");
        }
        if (junctions.containsKey(id)) {
            throw new IllegalArgumentException("junction with id '" + id + "' already exists");
        }
        junctions.put(id, junction);
    }

    /**
     *
     * @param id != null && !id.isEmpty()
     * @return the junction with the given id if present; otherwise null
     */
    public Junction getJunction(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id must be non-null and non-empty");
        }
        return junctions.get(id);
    }

    /**
     * @return an unmodifiable view of all junctions in the graph.
     */
    public Collection<Junction> getJunctions() {
        return Collections.unmodifiableCollection(junctions.values());
    }

    /**
     * @return number of junctions in the graph.
     */
    public int size() {
        return junctions.size();
    }

    /**
     * Adds a directed road segment from {@code fromId} to {@code toId}.
     * The segment is stored inside the source {@link Junction}'s adjacency list.
     *
     * @param fromId     source junction id
     * @param toId       target junction id
     * @param travelTime travel time in minutes (must be >= 0)
     * @param riskLevel  risk level (must be >= 0; recommended in [0,1])
     * - getJunction(fromId) != null
     * @throws IllegalArgumentException if ids are invalid or junctions are missing or values invalid
     */
    public void addRoadSegment(String fromId, String toId, double travelTime, double riskLevel) {
        if (travelTime < 0) {
            throw new IllegalArgumentException("travelTime must be >= 0");
        }
        if (riskLevel < 0) {
            throw new IllegalArgumentException("riskLevel must be >= 0");
        }

        Junction from = getJunction(fromId);
        Junction to = getJunction(toId);

        if (from == null) {
            throw new IllegalArgumentException("source junction not found: " + fromId);
        }
        if (to == null) {
            throw new IllegalArgumentException("target junction not found: " + toId);
        }

        RoadSegment seg = new RoadSegment(to, from, travelTime, riskLevel);
        from.addOutgoing(seg);
    }

    /**
     * @return a list of all junctions that are marked as shelters.
     */
    public List<Junction> getShelters() {
        return junctions.values().stream()
                .filter(Junction::isShelter)
                .toList();
    }
}