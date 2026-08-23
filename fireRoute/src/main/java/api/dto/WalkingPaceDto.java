package api.dto;

/**
 * Walking pace as it appears on the wire.
 *
 * Deliberately separate from routing.WalkingPace: the domain enum carries a
 * speed in km/h and is free to change, while these names are a published
 * contract that clients depend on. RouteMapper translates between them.
 */
public enum WalkingPaceDto {
    SLOW,
    AVERAGE,
    FAST
}
