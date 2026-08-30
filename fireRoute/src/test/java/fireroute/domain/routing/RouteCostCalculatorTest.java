package fireroute.routing;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import fireroute.domain.graph.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cost of a segment is walking time plus fearFactor times the minutes of
 * exposure beyond the 1.5-minute safe threshold, where exposure is the walking
 * time from the segment's target junction to the nearest shelter.
 *
 * Test graph, one direction:
 *
 * <pre>
 *   A --5.0--> B --3.0--> S (shelter)
 * </pre>
 *
 * so exposure is 8.0 minutes at A, 3.0 at B and 0.0 at S. Every expected value
 * below is derived from those three numbers by hand rather than from the code.
 */
class RouteCostCalculatorTest {

    private static final double SAFE_MINUTES = 1.5;

    private Graph graph;
    private ShelterMap shelterMap;
    private RouteCostCalculator calculator;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        graph.addJunction(new Junction("A", 34.77, 32.07, false));
        graph.addJunction(new Junction("B", 34.78, 32.07, false));
        graph.addJunction(new Junction("S", 34.79, 32.07, true));

        graph.addRoadSegment("A", "B", 5.0);
        graph.addRoadSegment("B", "S", 3.0);

        shelterMap = new ShelterMap(graph);
        shelterMap.compute();

        calculator = new RouteCostCalculator(shelterMap);
    }

    private RoadSegment segment(String fromId, String toId) {
        for (RoadSegment road : graph.getJunction(fromId).getOutGoingRoads()) {
            if (road.getTargetJunction().getId().equals(toId)) {
                return road;
            }
        }
        throw new IllegalStateException("no segment " + fromId + " -> " + toId);
    }

    // ---------------------------------------------------------
    // Validation
    // ---------------------------------------------------------

    @Test
    @DisplayName("Constructor should reject a null shelter map")
    void constructorRejectsNullShelterMap() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RouteCostCalculator(null)
        );
    }

    @Test
    @DisplayName("calculateCost should reject null arguments")
    void calculateCostRejectsNullArguments() {
        RoadSegment ab = segment("A", "B");

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateCost(null, new RouteParams())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateCost(ab, null)
        );
    }

    // ---------------------------------------------------------
    // The two terms of the formula
    // ---------------------------------------------------------

    @Test
    @DisplayName("Fear factor zero should leave only walking time")
    void zeroFearFactorLeavesOnlyWalkingTime() {
        RouteParams params = new RouteParams(WalkingPace.AVERAGE, 0.0);

        assertEquals(
                5.0,
                calculator.calculateCost(segment("A", "B"), params),
                1e-9,
                "with no fear, exposure is free and only the 5-minute walk counts"
        );
    }

    @Test
    @DisplayName("Exposure beyond the safe threshold should be charged")
    void exposureBeyondSafeThresholdIsCharged() {
        // Target B sits 3.0 minutes from the shelter, so 1.5 of those are excess.
        RouteParams params = new RouteParams(WalkingPace.AVERAGE, 1.0);

        assertEquals(
                5.0 + 1.0 * (3.0 - SAFE_MINUTES),
                calculator.calculateCost(segment("A", "B"), params),
                1e-9
        );
    }

    @Test
    @DisplayName("A segment ending at a shelter should carry no exposure penalty")
    void segmentEndingAtShelterHasNoPenalty() {
        RouteParams params = new RouteParams(WalkingPace.AVERAGE, 5.0);

        assertEquals(
                3.0,
                calculator.calculateCost(segment("B", "S"), params),
                1e-9,
                "exposure at the shelter is zero, so even a very cautious walker pays nothing extra"
        );
    }

    @Test
    @DisplayName("Higher fear factor should raise the cost of an exposed segment")
    void higherFearFactorRaisesExposedSegmentCost() {
        RoadSegment exposed = segment("A", "B");

        double cautious = calculator.calculateCost(
                exposed, new RouteParams(WalkingPace.AVERAGE, 4.0));
        double relaxed = calculator.calculateCost(
                exposed, new RouteParams(WalkingPace.AVERAGE, 1.0));

        assertTrue(cautious > relaxed);
    }

    @Test
    @DisplayName("Fear factor should not change the cost of a sheltered segment")
    void fearFactorDoesNotChangeShelteredSegmentCost() {
        RoadSegment sheltered = segment("B", "S");

        assertEquals(
                calculator.calculateCost(sheltered, new RouteParams(WalkingPace.AVERAGE, 0.0)),
                calculator.calculateCost(sheltered, new RouteParams(WalkingPace.AVERAGE, 9.0)),
                1e-9
        );
    }

    // ---------------------------------------------------------
    // Pace
    // ---------------------------------------------------------

    @Test
    @DisplayName("A slower pace should raise both walking time and exposure")
    void slowerPaceRaisesCost() {
        RoadSegment ab = segment("A", "B");

        double average = calculator.calculateCost(
                ab, new RouteParams(WalkingPace.AVERAGE, 1.0));
        double slow = calculator.calculateCost(
                ab, new RouteParams(WalkingPace.SLOW, 1.0));

        assertTrue(
                slow > average,
                "the same road takes a slow walker longer and leaves them exposed longer"
        );
    }

    // ---------------------------------------------------------
    // No shelter at all
    // ---------------------------------------------------------

    @Test
    @DisplayName("A junction with no reachable shelter should fall back to the caller's limit")
    void unreachableShelterFallsBackToTheRequestedLimit() {
        Graph shelterless = new Graph();
        shelterless.addJunction(new Junction("X", 34.77, 32.07, false));
        shelterless.addJunction(new Junction("Y", 34.78, 32.07, false));
        shelterless.addRoadSegment("X", "Y", 2.0);

        ShelterMap emptyMap = new ShelterMap(shelterless);
        emptyMap.compute();

        RouteCostCalculator shelterlessCalculator = new RouteCostCalculator(emptyMap);

        RoadSegment xy = shelterless.getJunction("X").getOutGoingRoads().get(0);
        RouteParams params = new RouteParams(WalkingPace.AVERAGE, 1.0, 10.0);

        // Exposure is infinite, so the requested 10-minute limit stands in for it.
        assertEquals(
                2.0 + 1.0 * (10.0 - SAFE_MINUTES),
                shelterlessCalculator.calculateCost(xy, params),
                1e-9
        );
    }
}
