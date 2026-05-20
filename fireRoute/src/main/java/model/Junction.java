package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph node representing a road junction.
 * Supports optional coordinates and incoming edges for reversed graph algorithms.
 */
public class Junction {
    private final String id;
    private final List<RoadSegment> outGoingRoads;
    private final List<RoadSegment> incomingRoads;
    private final Double x; // Using Double to allow null (optional coordinates)
    private final Double y; // Using Double to allow null (optional coordinates)
    private final boolean isShelter;

    /**
     * Constructor for mock graphs or junctions without physical coordinates.
     */
    public Junction(String id, boolean isShelter) {
        this(id, null, null, isShelter);
    }

    /**
     * Constructor for realistic graphs with coordinates.
     */
    public Junction(String id, Double x, Double y, boolean isShelter) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isShelter = isShelter;
        this.outGoingRoads = new ArrayList<>();
        this.incomingRoads = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<RoadSegment> getOutGoingRoads() {
        return outGoingRoads;
    }

    public List<RoadSegment> getIncomingRoads() {
        return incomingRoads;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public boolean hasCoordinates() {
        return x != null && y != null;
    }

    /**
     * @return true if this junction is a designated shelter.
     */
    public boolean isShelter() {
        return isShelter;
    }

    public void addOutgoing(RoadSegment roadSegment) {
        outGoingRoads.add(roadSegment);
    }

    public void addIncoming(RoadSegment roadSegment) {
        incomingRoads.add(roadSegment);
    }
}