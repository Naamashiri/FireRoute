package routing;

import geom.GeoPoint;
import geom.GeoPolygon;
import graph.Graph;
import graph.Junction;
import org.junit.jupiter.api.Test;
import risk.RiskEvaluator;
import risk.RiskProfile;
import risk.RiskZone;
import risk.ZoneIndex;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how walking pace affects route time, total cost
 * and shelter accessibility.
 */
class PathFinderWalkingPaceTest {

    /**
     * Creates a graph containing two connected junctions.
     */
    private static Graph createTwoJunctionGraph(
            double travelTimeMinutes
    ) {
        Graph graph = new Graph();

        graph.addJunction(
                new Junction("A", 0.0, 0.0, false)
        );

        graph.addJunction(
                new Junction("B", 10.0, 10.0, true)
        );

        graph.addRoadSegment(
                "A",
                "B",
                travelTimeMinutes,
                0.0
        );

        return graph;
    }

    /**
     * Creates a risk evaluator with a danger zone surrounding
     * junction A. Junction B is outside the zone.
     */
    private static RiskEvaluator createRiskEvaluator(
            Graph graph
    ) {
        GeoPolygon dangerZonePolygon = new GeoPolygon(
                List.of(
                        new GeoPoint(-1.0, -1.0),
                        new GeoPoint(1.0, -1.0),
                        new GeoPoint(1.0, 1.0),
                        new GeoPoint(-1.0, 1.0)
                )
        );

        RiskProfile riskProfile = new RiskProfile(
                "area-10",
                8,
                Instant.now(),
                false
        );

        RiskZone riskZone = new RiskZone(
                "zone-1",
                "danger-zone",
                dangerZonePolygon,
                riskProfile
        );

        ZoneIndex zoneIndex = new ZoneIndex();
        zoneIndex.build(graph, List.of(riskZone));

        return new RiskEvaluator(zoneIndex);
    }

    @Test
    void slowerPaceIncreasesTravelTimeAndCostWithoutChangingPathOrRisk() {
        Graph graph = createTwoJunctionGraph(2.0);
        RiskEvaluator riskEvaluator =
                createRiskEvaluator(graph);

        PathFinder pathFinder =
                new PathFinder(graph, riskEvaluator);

        Junction source = graph.getJunction("A");
        Junction destination = graph.getJunction("B");

        RouteParams averageParams = new RouteParams(
                10.0,
                WalkingPace.AVERAGE,
                1.0
        );

        RouteParams slowParams = new RouteParams(
                10.0,
                WalkingPace.SLOW,
                1.0
        );

        PathResult averageResult = pathFinder.findPath(
                source,
                destination,
                averageParams
        );

        PathResult slowResult = pathFinder.findPath(
                source,
                destination,
                slowParams
        );

        assertTrue(averageResult.hasPath());
        assertTrue(slowResult.hasPath());

        assertEquals(
                averageResult.getPath(),
                slowResult.getPath()
        );

        assertEquals(
                averageResult.getMaxRisk(),
                slowResult.getMaxRisk(),
                1e-9
        );

        assertTrue(
                slowResult.getTotalTime()
                        > averageResult.getTotalTime()
        );

        assertTrue(
                slowResult.getTotalCost()
                        > averageResult.getTotalCost()
        );

        double expectedRatio =
                slowParams.getPaceMultiplier();

        assertEquals(
                averageResult.getTotalTime() * expectedRatio,
                slowResult.getTotalTime(),
                1e-9
        );

        assertEquals(
                averageResult.getTotalCost() * expectedRatio,
                slowResult.getTotalCost(),
                1e-9
        );
    }

    @Test
    void slowPaceCanFailShelterLimitThatAveragePacePasses() {
        Graph graph = createTwoJunctionGraph(6.0);
        RiskEvaluator riskEvaluator =
                createRiskEvaluator(graph);

        PathFinder pathFinder =
                new PathFinder(graph, riskEvaluator);

        Junction source = graph.getJunction("A");
        Junction destination = graph.getJunction("B");

        RouteParams averageParams = new RouteParams(
                6.5,
                WalkingPace.AVERAGE,
                1.0
        );

        RouteParams slowParams = new RouteParams(
                6.5,
                WalkingPace.SLOW,
                1.0
        );

        PathResult averageResult = pathFinder.findPath(
                source,
                destination,
                averageParams
        );

        PathResult slowResult = pathFinder.findPath(
                source,
                destination,
                slowParams
        );

        assertTrue(averageResult.hasPath());
        assertFalse(slowResult.hasPath());
    }
}