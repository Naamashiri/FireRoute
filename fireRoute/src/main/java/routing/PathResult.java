package routing;

import graph.Junction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Route result: the path and key metrics.
 * maxRisk is the bottleneck (maximum) risk along the route.
 */
public class PathResult {

    private final List<Junction> path;  // empty => no path
    private final double totalTime;     // minutes
    private final double totalCost;     // algorithm cost (e.g., time + fear*risk)
    private final double maxRisk;       // bottleneck risk on the route (max edge risk)

    public PathResult(List<Junction> path, double totalTime, double totalCost, double maxRisk) {
        if (path == null) throw new IllegalArgumentException("path must be non-null");
        if (totalTime < 0) throw new IllegalArgumentException("totalTime must be >= 0");
        if (totalCost < 0) throw new IllegalArgumentException("totalCost must be >= 0");
        if (maxRisk < 0) throw new IllegalArgumentException("maxRisk must be >= 0");

        this.path = Collections.unmodifiableList(new ArrayList<>(path));
        this.totalTime = totalTime;
        this.totalCost = totalCost;
        this.maxRisk = maxRisk;
    }

    public static PathResult noPath() {
        return new PathResult(List.of(), 0.0, 0.0, 0.0);
    }

    public List<Junction> getPath() { return path; }
    public double getTotalTime() { return totalTime; }
    public double getTotalCost() { return totalCost; }
    public double getMaxRisk() { return maxRisk; }

    public boolean hasPath() { return !path.isEmpty(); }

    @Override
    public String toString() {
        return "PathResult{nodes=" + path.size() +
                ", totalTime=" + totalTime +
                ", totalCost=" + totalCost +
                ", maxRisk=" + maxRisk + "}";
    }
}