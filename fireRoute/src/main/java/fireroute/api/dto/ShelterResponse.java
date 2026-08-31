package fireroute.api.dto;

public record ShelterResponse(
        String id,
        String address,
        double latitude,
        double longitude,
        boolean accessible
) {
}
