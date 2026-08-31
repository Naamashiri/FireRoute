package fireroute.domain.shelter;

import fireroute.domain.graph.GeoPoint;

public class Shelter {
    private final String id;
    private final String address;
    private final GeoPoint location;
    private final boolean accessible;

    public Shelter(String id, String address, GeoPoint location, boolean accessible) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("address must not be blank");
        if (location == null) throw new IllegalArgumentException("location must not be null");
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
