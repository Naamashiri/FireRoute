package api.dto;

import java.util.List;

public record RouteResponse(
    boolean found,
    double totalTravelTime,
    List<RoutePoint> pathPoints,
    double maxMinutesToShelter
) {
}
