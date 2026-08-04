package risk;

import java.util.*;

import graph.Graph;
import graph.Junction;

public class ZoneIndex {

    private final Map<Junction, RiskZone> junctionToZone = new HashMap<>();

    public void build(Graph graph, List<RiskZone> zones) {
        if (graph == null || zones == null) {
            throw new IllegalArgumentException("graph and zones must be non-null");
        }

        junctionToZone.clear();

        for (Junction j : graph.getJunctions()) {

            if (!j.hasCoordinates()) {
                continue;
            }

            for (RiskZone zone : zones) {
                if (zone.contains(j)) {
                    junctionToZone.put(j, zone);
                    break;
                }
            }
        }
    }

    public RiskZone getZone(Junction j) {
        return junctionToZone.get(j);
    }
}