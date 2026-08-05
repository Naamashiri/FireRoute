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
 * Verifies the core Dijkstra behavior in {@link PathFinder#findPath}:
 * choosing the lowest-cost route, weighing risk against travel time via
 * the fear factor, pruning junctions that fail the shelter-reachability
 * constraint, and handling trivial/unreachable cases.
 */
class DijkstraPathFinderTest {

    /** No risk zones registered => every segment has risk 0.0. */
    private static RiskEvaluator zeroRiskEvaluator() {
        return new RiskEvaluator(new ZoneIndex());
    }

    @Test
    void dijkstraChoosesLowerCostPathAmongMultipleRoutes() {
        Graph graph = new Graph();
        graph.addJunction(new Junction("A", true));
        graph.addJunction(new Junction("B", true));
        graph.addJunction(new Junction("C", true));
        graph.addJunction(new Junction("D", true));

        // Cheap route: A -> B -> D (total 3). Expensive route: A -> C -> D (total 6).
        graph.addRoadSegment("A", "B", 2.0, 0.0);
        graph.addRoadSegment("B", "D", 1.0, 0.0);
        graph.addRoadSegment("A", "C", 1.0, 0.0);
        graph.addRoadSegment("C", "D", 5.0, 0.0);

        PathFinder pathFinder = new PathFinder(graph, zeroRiskEvaluator());
        Junction a = graph.getJunction("A");
        Junction b = graph.getJunction("B");
        Junction d = graph.getJunction("D");

        RouteParams params = new RouteParams(10.0, WalkingPace.AVERAGE, 1.0);
        PathResult result = pathFinder.findPath(a, d, params);

        assertTrue(result.hasPath());
        assertEquals(List.of(a, b, d), result.getPath());
        assertEquals(3.0, result.getTotalTime(), 1e-9);
        // No risk anywhere, so cost equals time.
        assertEquals(result.getTotalTime(), result.getTotalCost(), 1e-9);
        assertEquals(0.0, result.getMaxRisk(), 1e-9);
    }

    @Test
    void higherFearFactorSwitchesRouteToAvoidRiskyShortcut() {
        Graph graph = new Graph();
        graph.addJunction(new Junction("A", -5.0, -5.0, true));
        graph.addJunction(new Junction("C", 1.0, 1.0, true));   // sits inside the danger zone
        graph.addJunction(new Junction("B", 20.0, 20.0, true));
        graph.addJunction(new Junction("D", 30.0, 30.0, true));

        // Short but risky: A -> C -> D (total time 2).
        graph.addRoadSegment("A", "C", 1.0, 0.0);
        graph.addRoadSegment("C", "D", 1.0, 0.0);
        // Longer but safe: A -> B -> D (total time 4).
        graph.addRoadSegment("A", "B", 2.0, 0.0);
        graph.addRoadSegment("B", "D", 2.0, 0.0);

        GeoPolygon dangerZone = new GeoPolygon(List.of(
                new GeoPoint(0, 0), new GeoPoint(2, 0),
                new GeoPoint(2, 2), new GeoPoint(0, 2)));
        RiskProfile hotProfile = new RiskProfile(10, 1.0, System.currentTimeMillis());
        RiskZone zone = new RiskZone("z1", "danger-zone", dangerZone, hotProfile);

        ZoneIndex zoneIndex = new ZoneIndex();
        zoneIndex.build(graph, List.of(zone));
        RiskEvaluator riskEvaluator = new RiskEvaluator(zoneIndex);

        PathFinder pathFinder = new PathFinder(graph, riskEvaluator);
        Junction a = graph.getJunction("A");
        Junction b = graph.getJunction("B");
        Junction c = graph.getJunction("C");
        Junction d = graph.getJunction("D");

        RouteParams lowFear = new RouteParams(10.0, WalkingPace.AVERAGE, 0.0);
        RouteParams highFear = new RouteParams(10.0, WalkingPace.AVERAGE, 5.0);

        PathResult lowFearResult = pathFinder.findPath(a, d, lowFear);
        PathResult highFearResult = pathFinder.findPath(a, d, highFear);

        assertTrue(lowFearResult.hasPath());
        assertTrue(highFearResult.hasPath());

        // With no fear of risk, the shorter risky shortcut wins.
        assertEquals(List.of(a, c, d), lowFearResult.getPath());
        assertTrue(lowFearResult.getMaxRisk() > 0.7);

        // A high fear factor makes the risky shortcut too costly; the safe detour wins instead.
        assertEquals(List.of(a, b, d), highFearResult.getPath());
        assertEquals(0.0, highFearResult.getMaxRisk(), 1e-9);
        assertEquals(4.0, highFearResult.getTotalTime(), 1e-9);
    }

    @Test
    void excludesJunctionWithNoShelterWithinTimeEvenIfItsRouteIsShorter() {
        Graph graph = new Graph();
        graph.addJunction(new Junction("S", false));
        graph.addJunction(new Junction("X", false));
        graph.addJunction(new Junction("Y", false));
        graph.addJunction(new Junction("G", false));
        graph.addJunction(new Junction("Sh", true)); // the only designated shelter

        // Cheaper route: S -> X -> G (total time 3.5). X's only path to a shelter is through
        // G (2.5 + 1 = 3.5 min), which exceeds maxShelterMinutes, so X must be excluded.
        graph.addRoadSegment("S", "X", 1.0, 0.0);
        graph.addRoadSegment("X", "G", 2.5, 0.0);

        // Costlier route: S -> Y -> G (total time 5); Y has its own short path to the
        // shelter (1 min), so it independently satisfies the safety constraint.
        graph.addRoadSegment("S", "Y", 2.0, 0.0);
        graph.addRoadSegment("Y", "G", 3.0, 0.0);
        graph.addRoadSegment("Y", "Sh", 1.0, 0.0);
        graph.addRoadSegment("G", "Sh", 1.0, 0.0);

        PathFinder pathFinder = new PathFinder(graph, zeroRiskEvaluator());
        Junction s = graph.getJunction("S");
        Junction y = graph.getJunction("Y");
        Junction g = graph.getJunction("G");

        // maxShelterMinutes = 3: S (dist 3 via Y), Y (dist 1) and G (dist 1) all qualify;
        // X's only shelter path costs 3.5 min, so it fails and must be excluded even though
        // the route through it is cheaper overall.
        RouteParams params = new RouteParams(3.0, WalkingPace.AVERAGE, 1.0);
        PathResult result = pathFinder.findPath(s, g, params);

        assertTrue(result.hasPath());
        assertEquals(List.of(s, y, g), result.getPath());
        assertEquals(5.0, result.getTotalTime(), 1e-9);
    }

    @Test
    void returnsNoPathWhenGoalIsUnreachable() {
        Graph graph = new Graph();
        graph.addJunction(new Junction("A", true));
        graph.addJunction(new Junction("B", true)); // isolated: no edges at all

        PathFinder pathFinder = new PathFinder(graph, zeroRiskEvaluator());
        Junction a = graph.getJunction("A");
        Junction b = graph.getJunction("B");

        RouteParams params = new RouteParams(10.0, WalkingPace.AVERAGE, 1.0);
        PathResult result = pathFinder.findPath(a, b, params);

        assertFalse(result.hasPath());
        assertEquals(List.of(), result.getPath());
    }

    @Test
    void startEqualsGoalReturnsTrivialZeroCostPath() {
        Graph graph = new Graph();
        graph.addJunction(new Junction("A", true));

        PathFinder pathFinder = new PathFinder(graph, zeroRiskEvaluator());
        Junction a = graph.getJunction("A");

        RouteParams params = new RouteParams(10.0, WalkingPace.AVERAGE, 1.0);
        PathResult result = pathFinder.findPath(a, a, params);

        assertTrue(result.hasPath());
        assertEquals(List.of(a), result.getPath());
        assertEquals(0.0, result.getTotalTime(), 1e-9);
        assertEquals(0.0, result.getTotalCost(), 1e-9);
        assertEquals(0.0, result.getMaxRisk(), 1e-9);
    }
}
