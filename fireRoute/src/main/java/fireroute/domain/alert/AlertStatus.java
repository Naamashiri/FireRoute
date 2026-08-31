package fireroute.domain.alert;

/**
 * What an alert source managed to determine on one attempt.
 *
 * Three values, not a boolean, and the third is the reason why: the only
 * available alert feed is undocumented, unofficial and reachable only from
 * inside Israel, so failure is a normal outcome rather than an exception.
 * Collapsing a failed check into "no alert" would have the service report
 * quiet skies whenever it loses its connection — the one lie an alert system
 * must never tell.
 */
public enum AlertStatus {

    /** An alert is live in this service's area. */
    ACTIVE,

    /** Checked successfully; no alert. */
    QUIET,

    /** Could not be determined. Says nothing about whether an alert exists. */
    UNKNOWN
}
