package fireroute.application;

public class ShelterNotFoundException extends RuntimeException {
    public ShelterNotFoundException(String shelterId) {
        super("Shelter not found: " + shelterId);
    }
}
