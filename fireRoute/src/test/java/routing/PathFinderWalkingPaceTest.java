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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a slower WalkingPace increases both the reported travel time
 * and the risk-weighted cost of a route, without changing the risk score
 * itself or the chosen path.
 */
class PathFinderWalkingPaceTest {

    /** A now-shaped danger zone around junction A's coordinates, junction B sits outside it. */
    private static RiskEvaluator riskEvaluatorOverZoneAroundOrigin(Graph graph) {
        GeoPolygon dangerZone = new GeoPolygon(List.of(
                new GeoPoint(-1, -1), new GeoPoint(1, -1),
                new GeoPoint(1, 1), new GeoPoint(-1, 1)));
        RiskProfile hotProfile = new RiskProfile(10, 0.8, System.currentTimeMillis());
        RiskZone zone = new RiskZone("z1", "danger-zone", dangerZone, hotProfile);

        ZoneIndex zoneIndex = new ZoneIndex();
        zoneIndex.build(graph, List.of(zone));
        return new RiskEvaluator(zoneIndex);
    }

    private static Graph twoJunctionGraph(double travelTimeMinutes) {
        Graph graph = new Graph();
        graph.addJunction(new Junction("A", 0.0, 0.0, false));
        graph.addJunction(new Junction("B", 10.0, 10.0, true));
        graph.addRoadSegment("A", "B", travelTimeMinutes, 0.0);
        return graph;
    }

    @Test
    void slowerPaceIncreasesTravelTimeAndCostButNotRiskOrPath() {
        Graph graph = twoJunctionGraph(2.0);
        RiskEvaluator riskEvaluator = riskEvaluatorOverZoneAroundOrigin(graph);
        PathFinder pathFinder = new PathFinder(graph, riskEvaluator);

        Junction a = graph.getJunction("A");
        Junction b = graph.getJunction("B");

        RouteParams averagePace = new RouteParams(10.0, WalkingPace.AVERAGE, 1.0);
        RouteParams slowPace = new RouteParams(10.0, WalkingPace.SLOW, 1.0);

        PathResult averageResult = pathFinder.findPath(a, b, averagePace);
        PathResult slowResult = pathFinder.findPath(a, b, slowPace);

        assertTrue(averageResult.hasPath());
        assertTrue(slowResult.hasPath());

        assertEquals(averageResult.getPath(), slowResult.getPath());
        // RiskProfile's recency weight is computed from System.currentTimeMillis() on each
        // call, so it drifts by nanoseconds between the two findPath() calls above.
        assertEquals(averageResult.getMaxRisk(), slowResult.getMaxRisk(), 1e-6);

        assertTrue(slowResult.getTotalTime() > averageResult.getTotalTime());
        assertTrue(slowResult.getTotalCost() > averageResult.getTotalCost());

        double expectedRatio = slowPace.getPaceMultiplier();
        assertEquals(averageResult.getTotalTime() * expectedRatio, slowResult.getTotalTime(), 1e-9);
        assertEquals(averageResult.getTotalCost() * expectedRatio, slowResult.getTotalCost(), 1e-9);
    }

    @Test
    void slowPaceCanFailAShelterCheckThatAveragePacePasses() {
        // travelTime chosen so that AVERAGE pace (x1.0) fits maxShelterMinutes,
        // but SLOW pace (x1.4286) does not.
        Graph graph = twoJunctionGraph(6.0);
        RiskEvaluator riskEvaluator = riskEvaluatorOverZoneAroundOrigin(graph);
        PathFinder pathFinder = new PathFinder(graph, riskEvaluator);

        Junction a = graph.getJunction("A");
        Junction b = graph.getJunction("B");

        RouteParams averagePace = new RouteParams(6.5, WalkingPace.AVERAGE, 1.0);
        RouteParams slowPace = new RouteParams(6.5, WalkingPace.SLOW, 1.0);

        assertTrue(pathFinder.findPath(a, b, averagePace).hasPath());
        assertFalse(pathFinder.findPath(a, b, slowPace).hasPath());
    }
}
