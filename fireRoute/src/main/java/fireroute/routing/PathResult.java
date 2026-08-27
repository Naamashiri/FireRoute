package fireroute.routing;

import fireroute.domain.graph.Junction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Route result: the path and its key metrics.
 *
 * maxRisk is the bottleneck (maximum) risk along the route, and
 * maxMinutesToShelter is the worst exposure at any point on it — the figure that
 * decides whether the route is safe, as opposed to totalTime, which only says
 * how long it is.
 */
public class PathResult {

    private final List<Junction> path;  // empty => no path
    private final double totalTime;     // minutes
    private final double totalCost;     // algorithm cost (e.g., time + fear*risk)
    private final double maxRisk;       // bottleneck risk on the route (max edge risk)
    private final double maxMinutesToShelter; // worst time to reach a shelter from any point on the route
    private final boolean shelterConstraintSatisfied;

    /**
     * @param shelterConstraintSatisfied whether every junction on the route is
     *        within the requested distance of a shelter. False means a route was
     *        found only by relaxing that limit, and the caller should say so
     *        rather than present it as safe. Meaningless when the path is empty.
     */
    public PathResult(
            List<Junction> path,
            double totalTime,
            double totalCost,
            double maxRisk,
            double maxMinutesToShelter,
            boolean shelterConstraintSatisfied
    ) {
        if (path == null) throw new IllegalArgumentException("path must be non-null");
        if (totalTime < 0) throw new IllegalArgumentException("totalTime must be >= 0");
        if (totalCost < 0) throw new IllegalArgumentException("totalCost must be >= 0");
        if (maxRisk < 0) throw new IllegalArgumentException("maxRisk must be >= 0");

        this.path = Collections.unmodifiableList(new ArrayList<>(path));
        this.totalTime = totalTime;
        this.totalCost = totalCost;
        this.maxRisk = maxRisk;
        this.maxMinutesToShelter = maxMinutesToShelter;
        this.shelterConstraintSatisfied = shelterConstraintSatisfied;
    }

    /**
     * Convenience for callers that build a result already known to respect the
     * shelter limit — chiefly tests working on abstract graphs.
     */
    public PathResult(List<Junction> path, double totalTime, double totalCost, double maxRisk, double maxMinutesToShelter) {
        this(path, totalTime, totalCost, maxRisk, maxMinutesToShelter, true);
    }

    public static PathResult noPath() {
        return new PathResult(List.of(), 0.0, 0.0, 0.0, 0.0, true);
    }

    public List<Junction> getPath() { return path; }
    public double getTotalTime() { return totalTime; }
    public double getTotalCost() { return totalCost; }
    public double getMaxRisk() { return maxRisk; }
    public double getMaxMinutesToShelter() { return maxMinutesToShelter; }
    public boolean isShelterConstraintSatisfied() { return shelterConstraintSatisfied; }

    public boolean hasPath() { return !path.isEmpty(); }

    @Override
    public String toString() {
        return "PathResult{nodes=" + path.size() +
                ", totalTime=" + totalTime +
                ", totalCost=" + totalCost +
                ", maxRisk=" + maxRisk +
                ", maxMinutesToShelter=" + maxMinutesToShelter +
                ", shelterConstraintSatisfied=" + shelterConstraintSatisfied + "}";
    }
}
