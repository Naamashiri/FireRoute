package fireroute.domain.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteParamsTest {

    private static final double DELTA = 1e-9;

    @Test
    void averagePaceGivesMultiplierOfOne() {
        RouteParams params = new RouteParams(WalkingPace.AVERAGE, 1.0);
        assertEquals(1.0, params.getPaceMultiplier(), DELTA);
    }

    @Test
    void slowerPaceGivesMultiplierGreaterThanOne() {
        RouteParams params = new RouteParams(WalkingPace.SLOW, 1.0);
        assertEquals(WalkingPace.AVERAGE.getSpeedKmh() / WalkingPace.SLOW.getSpeedKmh(),
                params.getPaceMultiplier(), DELTA);
        assertEquals(1.4285714286, params.getPaceMultiplier(), 1e-6);
    }

    @Test
    void fasterPaceGivesMultiplierLessThanOne() {
        RouteParams params = new RouteParams(WalkingPace.FAST, 1.0);
        assertEquals(WalkingPace.AVERAGE.getSpeedKmh() / WalkingPace.FAST.getSpeedKmh(),
                params.getPaceMultiplier(), DELTA);
        assertEquals(0.8333333333, params.getPaceMultiplier(), 1e-6);
    }

    @Test
    void noArgConstructorDefaultsToAveragePace() {
        RouteParams params = new RouteParams();
        assertEquals(1.0, params.getPaceMultiplier(), DELTA);
    }

    @Test
    void nullWalkingPaceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteParams(null, 1.0));
    }
}
