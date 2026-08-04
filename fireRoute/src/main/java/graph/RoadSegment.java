package graph;

/**
 * Represents a directed edge in the road graph.
 * Each edge leads from an implicit source node (the node that owns this edge)
 * to a target node, with an associated travel time and risk level.
 */
public class RoadSegment {

    /** The destination node this edge leads to. */
    private final Junction targetJunction;

    /** Time in minutes to traverse this road. */
    private final double travelTime;

    /** Current edge - risk level (0.0 safe <--> 1.0 dangerous). */
    private double riskLevel;


    /**
     *
     * @param targetJunction != null
     * @param travelTime > 0
     * @param riskLevel inRange()
     */
    public RoadSegment(Junction targetJunction, double travelTime, double riskLevel) {
        if(targetJunction == null) {
            throw new IllegalArgumentException("Junction must not be null or empty");
            
        }
        if (!inRange(riskLevel)) {
            throw new IllegalArgumentException("Risk must be between 0 and 1");
        }
        if (travelTime <= 0) {
            throw new IllegalArgumentException("Time must be positive value");
        }

        this.targetJunction = targetJunction;
        this.travelTime = travelTime;
        this.riskLevel = riskLevel;
        }
    public double cost(double fearFactor) {
        return travelTime + fearFactor * riskLevel;
    }
    public Junction getTargetJunction() { return targetJunction; }
    public double getTravelTime() { return travelTime; }
    public double getRiskLevel() { return riskLevel; }

    /**
     *
     * @param riskLevel inRange()
     */
    public void setRiskLevel(double riskLevel) {
        if(!inRange(riskLevel))
            throw new IllegalArgumentException("risk level should be in [0,1]");
        this.riskLevel = riskLevel;
    }

    /**
     *
     * @return true if and only if riskLevel is in [0,1]
     */
    public boolean inRange(double riskLevel){
        return riskLevel >= 0 && riskLevel <= 1;
    }

}


