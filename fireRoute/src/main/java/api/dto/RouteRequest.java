package api.dto;

import routing.WalkingPace;

/**
 * DTO for incoming route calculation requests.
 * JSON -> RouteRequest
 */
public record RouteRequest(
        String sourceId,
        String destinationId,
        double maxShelterMinutes,
        WalkingPace walkingPace,
        double fearFactor
) {
    // בנאי קומפקטי עם ערכי ברירת מחדל אם מגיעים שדות חסרים
    public RouteRequest {
        if (maxShelterMinutes <= 0) {
            maxShelterMinutes = 7.0; // ברירת מחדל: עד 7 דקות למקלט
        }
        if (walkingPace == null) {
            walkingPace = WalkingPace.AVERAGE;
        }
    }
}