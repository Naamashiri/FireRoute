package routing;

import geom.GeoPoint;
import geom.GeoPolygon;
import graph.Graph;
import graph.Junction;
import graph.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import risk.RiskEvaluator;
import risk.RiskProfile;
import risk.RiskZone;
import risk.ZoneIndex;
import shelters.Shelter;
import shelters.ShelterRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteCostCalculatorTest {

    private ShelterRepository shelterRepository;
    private ZoneIndex zoneIndex;
    private RiskEvaluator riskEvaluator;
    private RouteCostCalculator calculator;

    @BeforeEach
    void setUp() {
        shelterRepository = new ShelterRepository();
        zoneIndex = new ZoneIndex();
        riskEvaluator = new RiskEvaluator(zoneIndex);

        calculator = new RouteCostCalculator(
                riskEvaluator,
                shelterRepository
        );
    }

    // ---------------------------------------------------------
    // Constructor / argument validation
    // ---------------------------------------------------------

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void constructorRejectsNullDependencies() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new RouteCostCalculator(
                        null,
                        shelterRepository
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new RouteCostCalculator(
                        riskEvaluator,
                        null
                )
        );
    }

    @Test
    @DisplayName("calculateCost should reject null arguments")
    void calculateCostRejectsNullArguments() {

        Junction a = new Junction("A", false);
        Junction b = new Junction("B", false);

        RoadSegment segment =
                new RoadSegment(a, b, 5.0, 0.0);

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        1.0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateCost(null, params)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateCost(segment, null)
        );
    }

    // ---------------------------------------------------------
    // Travel time / walking pace
    // ---------------------------------------------------------

    @Test
    @DisplayName("Fear factor 0 should leave only actual walking time")
    void zeroFearFactorReturnsOnlyActualTravelTime() {

        Junction a = new Junction("A", false);
        Junction b = new Junction("B", false);

        RoadSegment segment =
                new RoadSegment(a, b, 5.0, 0.0);

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        0.0
                );

        double cost =
                calculator.calculateCost(segment, params);

        double expected =
                segment.getTravelTime()
                        * params.getPaceMultiplier();

        assertEquals(expected, cost, 1e-9);
    }

    @Test
    @DisplayName("Slower walking pace should increase route cost")
    void slowerWalkingPaceIncreasesCost() {

        Junction a = new Junction("A", false);
        Junction b = new Junction("B", true);

        RoadSegment segment =
                new RoadSegment(a, b, 10.0, 0.0);

        RouteParams average =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        0.0
                );

        RouteParams slow =
                new RouteParams(
                        7.0,
                        WalkingPace.SLOW,
                        0.0
                );

        double averageCost =
                calculator.calculateCost(segment, average);

        double slowCost =
                calculator.calculateCost(segment, slow);

        assertTrue(slowCost > averageCost);

        assertEquals(
                averageCost * slow.getPaceMultiplier(),
                slowCost,
                1e-9
        );
    }

    // ---------------------------------------------------------
    // Risk
    // ---------------------------------------------------------

    @Test
    @DisplayName("Risk should increase cost according to fear factor")
    void riskIncreasesCostAccordingToFearFactor() {

        Junction a =
                new Junction(
                        "A",
                        32.0800,
                        34.7800,
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        32.0802,
                        34.7802,
                        false
                );

        RoadSegment segment =
                new RoadSegment(a, b, 10.0, 0.0);

        /*
         * Shelter exactly at segment midpoint,
         * therefore shelter penalty = 0.
         */
        GeoPoint midpoint =
                new GeoPoint(
                        (a.getX() + b.getX()) / 2.0,
                        (a.getY() + b.getY()) / 2.0
                );

        shelterRepository.addShelter(
                new Shelter(
                        "S1",
                        "Test Shelter",
                        midpoint,
                        true
                )
        );

        Graph graph = new Graph();
        graph.addJunction(a);
        graph.addJunction(b);

        RiskProfile profile =
                new RiskProfile(
                        "area-1",
                        5,
                        Instant.now(),
                        false
                );

        GeoPolygon polygon =
                new GeoPolygon(
                        List.of(
                                new GeoPoint(32.0790, 34.7790),
                                new GeoPoint(32.0810, 34.7790),
                                new GeoPoint(32.0810, 34.7810),
                                new GeoPoint(32.0790, 34.7810)
                        )
                );

        RiskZone zone =
                new RiskZone(
                        "zone-1",
                        "Test Risk Zone",
                        polygon,
                        profile
                );

        zoneIndex.build(
                graph,
                List.of(zone)
        );

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        2.0
                );

        double segmentRisk =
                riskEvaluator.getSegmentRisk(segment);

        assertTrue(segmentRisk > 0.0);

        double expected =
                segment.getTravelTime()
                        * params.getPaceMultiplier()
                        * (
                                1.0
                                + params.getFearFactor()
                                * segmentRisk
                        );

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    @DisplayName("Higher fear factor should increase cost of risky segment")
    void higherFearFactorIncreasesRiskySegmentCost() {

        Junction a =
                new Junction(
                        "A",
                        32.0800,
                        34.7800,
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        32.0802,
                        34.7802,
                        false
                );

        RoadSegment segment =
                new RoadSegment(a, b, 10.0, 0.0);

        GeoPoint midpoint =
                new GeoPoint(
                        (a.getX() + b.getX()) / 2.0,
                        (a.getY() + b.getY()) / 2.0
                );

        shelterRepository.addShelter(
                new Shelter(
                        "S1",
                        "Test Shelter",
                        midpoint,
                        true
                )
        );

        Graph graph = new Graph();
        graph.addJunction(a);
        graph.addJunction(b);

        RiskProfile profile =
                new RiskProfile(
                        "area-1",
                        5,
                        Instant.now(),
                        false
                );

        RiskZone zone =
                new RiskZone(
                        "zone-1",
                        "Risk Zone",
                        new GeoPolygon(
                                List.of(
                                        new GeoPoint(32.0790, 34.7790),
                                        new GeoPoint(32.0810, 34.7790),
                                        new GeoPoint(32.0810, 34.7810),
                                        new GeoPoint(32.0790, 34.7810)
                                )
                        ),
                        profile
                );

        zoneIndex.build(
                graph,
                List.of(zone)
        );

        RouteParams lowFear =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        0.5
                );

        RouteParams highFear =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        2.0
                );

        double lowFearCost =
                calculator.calculateCost(
                        segment,
                        lowFear
                );

        double highFearCost =
                calculator.calculateCost(
                        segment,
                        highFear
                );

        assertTrue(highFearCost > lowFearCost);
    }

    // ---------------------------------------------------------
    // Shelter penalty - abstract graphs
    // ---------------------------------------------------------

    @Test
    @DisplayName("Abstract graph without shelter should receive exact shelter penalty")
    void abstractGraphWithoutShelterGetsPenalty() {

        Junction a = new Junction("A", false);
        Junction b = new Junction("B", false);

        RoadSegment segment =
                new RoadSegment(a, b, 6.0, 0.0);

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        1.5
                );

        double actualTime =
                6.0 * params.getPaceMultiplier();

        double expectedShelterPenalty =
                params.getFearFactor()
                        * params.getMaxShelterMinutes();

        double expected =
                actualTime
                        + expectedShelterPenalty;

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    @DisplayName("Abstract segment connected to shelter junction should have no shelter penalty")
    void abstractGraphWithShelterJunctionGetsNoPenalty() {

        Junction a = new Junction("A", false);
        Junction shelterJunction =
                new Junction("S", true);

        RoadSegment segment =
                new RoadSegment(
                        a,
                        shelterJunction,
                        6.0,
                        0.0
                );

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        1.5
                );

        double expected =
                segment.getTravelTime()
                        * params.getPaceMultiplier();

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        assertEquals(expected, actual, 1e-9);
    }

    // ---------------------------------------------------------
    // Shelter penalty - geographical graphs
    // ---------------------------------------------------------

    @Test
    @DisplayName("Shelter at midpoint should produce zero shelter penalty")
    void shelterAtMidpointProducesNoPenalty() {

        Junction a =
                new Junction(
                        "A",
                        32.0800,
                        34.7800,
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        32.0802,
                        34.7802,
                        false
                );

        RoadSegment segment =
                new RoadSegment(a, b, 5.0, 0.0);

        GeoPoint midpoint =
                new GeoPoint(
                        (a.getX() + b.getX()) / 2.0,
                        (a.getY() + b.getY()) / 2.0
                );

        shelterRepository.addShelter(
                new Shelter(
                        "S1",
                        "Midpoint Shelter",
                        midpoint,
                        true
                )
        );

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        2.0
                );

        double expected =
                segment.getTravelTime()
                        * params.getPaceMultiplier();

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    @DisplayName("No available shelter should apply maximum shelter penalty")
    void noShelterAppliesMaximumPenalty() {

        Junction a =
                new Junction(
                        "A",
                        32.0800,
                        34.7800,
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        32.0802,
                        34.7802,
                        false
                );

        RoadSegment segment =
                new RoadSegment(a, b, 4.0, 0.0);

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        2.0
                );

        double actualTime =
                segment.getTravelTime()
                        * params.getPaceMultiplier();

        double expectedPenalty =
                params.getFearFactor()
                        * params.getMaxShelterMinutes();

        double expected =
                actualTime + expectedPenalty;

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    @DisplayName("Shelter farther than 90 seconds should add penalty only for excess time")
    void distantShelterAddsPenaltyOnlyBeyondSafeThreshold() {

        Junction a =
                new Junction(
                        "A",
                        32.0000,
                        34.0000,
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        32.0002,
                        34.0002,
                        false
                );

        RoadSegment segment =
                new RoadSegment(a, b, 4.0, 0.0);

        GeoPoint midpoint =
                new GeoPoint(
                        (a.getX() + b.getX()) / 2.0,
                        (a.getY() + b.getY()) / 2.0
                );

        GeoPoint farShelterLocation =
                new GeoPoint(
                        32.0100,
                        34.0100
                );

        shelterRepository.addShelter(
                new Shelter(
                        "S-FAR",
                        "Far Shelter",
                        farShelterLocation,
                        true
                )
        );

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        2.0
                );

        double distanceMeters =
                midpoint.distanceTo(farShelterLocation);

        double metersPerMinute =
                params.getWalkingSpeedKmh()
                        * 1000.0 / 60.0;

        double timeToShelter =
                distanceMeters / metersPerMinute;

        double excessMinutes =
                Math.max(
                        0.0,
                        timeToShelter - 1.5
                );

        double expectedPenalty =
                params.getFearFactor()
                        * excessMinutes;

        double expected =
                segment.getTravelTime()
                        * params.getPaceMultiplier()
                        + expectedPenalty;

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        assertTrue(excessMinutes > 0.0);

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    @DisplayName("Shelter reachable within 90 seconds should add no penalty")
    void nearbyShelterWithinSafeThresholdAddsNoPenalty() {

        Junction a =
                new Junction(
                        "A",
                        32.0800,
                        34.7800,
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        32.0802,
                        34.7802,
                        false
                );

        RoadSegment segment =
                new RoadSegment(a, b, 4.0, 0.0);

        GeoPoint midpoint =
                new GeoPoint(
                        (a.getX() + b.getX()) / 2.0,
                        (a.getY() + b.getY()) / 2.0
                );

        // Very close to midpoint
        GeoPoint nearbyShelter =
                new GeoPoint(
                        midpoint.x() + 0.0001,
                        midpoint.y()
                );

        shelterRepository.addShelter(
                new Shelter(
                        "S-NEAR",
                        "Nearby Shelter",
                        nearbyShelter,
                        true
                )
        );

        RouteParams params =
                new RouteParams(
                        7.0,
                        WalkingPace.AVERAGE,
                        2.0
                );

        double actual =
                calculator.calculateCost(
                        segment,
                        params
                );

        double expected =
                segment.getTravelTime()
                        * params.getPaceMultiplier();

        assertEquals(expected, actual, 1e-9);
    }
}