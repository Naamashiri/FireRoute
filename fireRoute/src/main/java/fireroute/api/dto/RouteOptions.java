package fireroute.api.dto;

/**
 * The tuning knobs shared by every kind of route request: how fast the user
 * walks, how heavily they weight risk, and how far from cover they are willing
 * to be.
 *
 * Both request records implement it, which lets RouteMapper resolve defaults in
 * one place instead of once per endpoint. What differs between the endpoints is
 * only the destination — a chosen one, or the nearest shelter — and that stays
 * on the records themselves.
 *
 * Every field is boxed so that "omitted" (null) stays distinguishable from
 * "sent as zero"; maxShelterMinutes = 0 is a meaningful request, admitting only
 * junctions that are shelters themselves.
 */
public interface RouteOptions {

    WalkingPaceDto walkingPace();

    Double fearFactor();

    Double maxShelterMinutes();

    /**
     * Shared validation for the record constructors. Records cannot inherit a
     * constructor, so the rule lives here and each compact constructor calls it.
     */
    static void validate(Double fearFactor, Double maxShelterMinutes) {
        if (fearFactor != null && fearFactor < 0) {
            throw new IllegalArgumentException("fearFactor must not be negative");
        }
        if (maxShelterMinutes != null && maxShelterMinutes < 0) {
            throw new IllegalArgumentException("maxShelterMinutes must not be negative");
        }
    }
}
