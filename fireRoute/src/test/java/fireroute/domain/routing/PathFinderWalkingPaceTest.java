package fireroute.domain.routing;

import fireroute.domain.graph.GeoPoint;
import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import org.junit.jupiter.api.Test;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;

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

        graph.addRoadSegment("A",
                "B",
                travelTimeMinutes);

        return graph;
    }

    /**
     * Finder and cost calculator sharing one ShelterMap over the populated graph.
     */
    private static DijkstraPathFinder pathFinderFor(Graph graph) {
        ShelterMap shelterMap = new ShelterMap(graph);
        shelterMap.compute();

        return new DijkstraPathFinder(
                graph,
                shelterMap,
                new RouteCostCalculator(shelterMap)
        );
    }

    @Test
    void slowerPaceIncreasesTravelTimeAndCostWithoutChangingPathOrRisk() {
        Graph graph = createTwoJunctionGraph(2.0);
                DijkstraPathFinder pathFinder = pathFinderFor(graph);

        Junction source = graph.getJunction("A");
        Junction destination = graph.getJunction("B");

        RouteParams averageParams = new RouteParams(
                WalkingPace.AVERAGE,
                1.0
        );

        RouteParams slowParams = new RouteParams(
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
    void slowPaceCanBreachShelterLimitThatAveragePaceRespects() {
        Graph graph = createTwoJunctionGraph(6.0);
                DijkstraPathFinder pathFinder = pathFinderFor(graph);

        Junction source = graph.getJunction("A");
        Junction destination = graph.getJunction("B");

        RouteParams averageParams = new RouteParams(
                WalkingPace.AVERAGE,
                1.0
        );

        RouteParams slowParams = new RouteParams(
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

        // Same road, same limit: at an average pace the walker is inside it, at a
        // slow pace the identical distance takes long enough to breach it. Both
        // still get a route; only the verdict on its safety differs.
        assertTrue(averageResult.hasPath());
        assertTrue(slowResult.hasPath());

        assertTrue(
                averageResult.isShelterConstraintSatisfied(),
                "an average pace stays within the shelter limit here"
        );

        assertFalse(
                slowResult.isShelterConstraintSatisfied(),
                "a slow pace turns the same road into an exposed one"
        );
    }
}