package fireroute.api.dto;

/**
 * Why a route could not be produced.
 *
 * Without this the client sees only found=false and cannot tell a genuinely
 * unreachable destination from a request it could usefully retry — so it can
 * neither explain the outcome to a user nor choose a sensible next step.
 */
public enum RouteFailureReason {

    /** A route was found. */
    NONE,

    /** No sequence of roads connects the two junctions in the requested direction. */
    NO_ROUTE_EXISTS
}
