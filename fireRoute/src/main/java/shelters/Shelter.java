package shelters;

import geom.GeoPoint;

public class Shelter {
    private final String id;
    private final String address;
    private final GeoPoint location;
    private final boolean accessible;

    public Shelter(String id, String address, GeoPoint location, boolean accessible) {
        this.id = id;
        this.address = address;
        this.location = location;
        this.accessible = accessible;
    }

    public String getId() { return id; }
    public String getAddress() { return address; }
    public GeoPoint getLocation() { return location; }
    public boolean isAccessible() { return accessible; }
}
