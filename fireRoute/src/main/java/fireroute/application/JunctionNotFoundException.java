package fireroute.application;

/** Raised when a caller supplies a junction id that is not in the loaded graph. */
public class JunctionNotFoundException extends RuntimeException {
    public JunctionNotFoundException(String junctionId) {
        super("Junction not found: " + junctionId);
    }
}
