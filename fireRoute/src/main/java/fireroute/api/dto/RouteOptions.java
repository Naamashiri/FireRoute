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

}
