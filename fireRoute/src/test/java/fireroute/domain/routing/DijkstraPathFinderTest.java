package fireroute.domain.routing;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import fireroute.domain.graph.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraPathFinderTest {

    private Graph graph;

    @BeforeEach
    void setUp() {
        graph = new Graph();
    }

    /**
     * ShelterMap has to be computed after the graph is populated, and the finder
     * and the cost calculator must share the one instance, so every test builds
     * its finder through here once its junctions and roads are in place.
     */
    private DijkstraPathFinder pathFinderFor(Graph graph) {
        ShelterMap shelterMap = new ShelterMap(graph);
        shelterMap.compute();

        return new DijkstraPathFinder(
                graph,
                shelterMap,
                new RouteCostCalculator(shelterMap)
        );
    }

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void constructorRejectsNullDependencies() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DijkstraPathFinder(
                        null,
                        new ShelterMap(graph),
                        new RouteCostCalculator(new ShelterMap(graph))
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new DijkstraPathFinder(
                        graph,
                        null,
                        new RouteCostCalculator(new ShelterMap(graph))
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new DijkstraPathFinder(
                        graph,
                        new ShelterMap(graph),
                        null
                )
        );
    }

    @Test
    @DisplayName("findPath should reject null arguments")
    void findPathRejectsNullArguments() {

        Junction a = new Junction("A", true);
        Junction b = new Junction("B", true);

        graph.addJunction(a);
        graph.addJunction(b);

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> pathFinder.findPath(
                        null,
                        b,
                        params
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> pathFinder.findPath(
                        a,
                        null,
                        params
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> pathFinder.findPath(
                        a,
                        b,
                        null
                )
        );
    }

    @Test
    @DisplayName("Start equals goal should return zero-cost path containing only start")
    void startEqualsGoal() {

        Junction a = new Junction("A", true);
        graph.addJunction(a);

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        1.0
                );

        PathResult result =
                pathFinder.findPath(
                        a,
                        a,
                        params
                );

        assertEquals(
                List.of(a),
                result.getPath()
        );

        assertEquals(
                0.0,
                result.getTotalTime(),
                0.001
        );

        assertEquals(
                0.0,
                result.getTotalCost(),
                0.001
        );
    }

    @Test
    @DisplayName("Should find a direct path")
    void findsDirectPath() {

        Junction a = new Junction("A", true);
        Junction b = new Junction("B", true);

        graph.addJunction(a);
        graph.addJunction(b);

        connect(
                a,
                b,
                5.0
        );

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        PathResult result =
                pathFinder.findPath(
                        a,
                        b,
                        params
                );

        assertEquals(
                List.of(a, b),
                result.getPath()
        );

        assertEquals(
                5.0 * params.getPaceMultiplier(),
                result.getTotalTime(),
                0.001
        );

        assertEquals(
                5.0 * params.getPaceMultiplier(),
                result.getTotalCost(),
                0.001
        );
    }

    @Test
    @DisplayName("Dijkstra should choose cheaper indirect path")
    void choosesCheaperIndirectPath() {

        Junction a = new Junction("A", true);
        Junction b = new Junction("B", true);
        Junction c = new Junction("C", true);

        graph.addJunction(a);
        graph.addJunction(b);
        graph.addJunction(c);

        connect(
                a,
                b,
                10.0
        );

        connect(
                a,
                c,
                2.0
        );

        connect(
                c,
                b,
                2.0
        );

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        PathResult result =
                pathFinder.findPath(
                        a,
                        b,
                        params
                );

        assertEquals(
                List.of(a, c, b),
                result.getPath()
        );

        double expectedTime =
                4.0 * params.getPaceMultiplier();

        assertEquals(
                expectedTime,
                result.getTotalTime(),
                0.001
        );

        assertEquals(
                expectedTime,
                result.getTotalCost(),
                0.001
        );
    }

    @Test
    @DisplayName("Should return no path when goal is disconnected")
    void returnsNoPathWhenDisconnected() {

        Junction a = new Junction("A", true);
        Junction b = new Junction("B", true);

        graph.addJunction(a);
        graph.addJunction(b);

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        PathResult result =
                pathFinder.findPath(
                        a,
                        b,
                        params
                );

        assertTrue(
                result.getPath().isEmpty()
        );
    }

    @Test
    @DisplayName("Path should avoid junction violating shelter constraint")
    void avoidsJunctionWithoutShelterCoverage() {

        Junction start = new Junction("START", true);
        Junction unsafe = new Junction("UNSAFE", false);
        Junction safe = new Junction("SAFE", true);
        Junction goal = new Junction("GOAL", true);

        graph.addJunction(start);
        graph.addJunction(unsafe);
        graph.addJunction(safe);
        graph.addJunction(goal);

        connect(
                start,
                unsafe,
                1.0
        );

        connect(
                unsafe,
                goal,
                1.0
        );

        connect(
                start,
                safe,
                4.0
        );

        connect(
                safe,
                goal,
                4.0
        );

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0,
                        // every junction on the route must be within half a minute of
                        // cover, which is what makes UNSAFE/START fail the constraint
                        0.5
                );

        PathResult result =
                pathFinder.findPath(
                        start,
                        goal,
                        params
                );

        assertEquals(
                List.of(start, safe, goal),
                result.getPath()
        );
    }

    @Test
    @DisplayName("Should still route from an unsafe start, flagging the relaxed constraint")
    void routesFromAnUnsafeStartAndFlagsIt() {

        Junction start = new Junction("START", false);
        Junction goal = new Junction("GOAL", true);

        graph.addJunction(start);
        graph.addJunction(goal);

        connect(
                start,
                goal,
                1.0
        );

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0,
                        // every junction on the route must be within half a minute of
                        // cover, which is what makes UNSAFE/START fail the constraint
                        0.5
                );

        PathResult result =
                pathFinder.findPath(
                        start,
                        goal,
                        params
                );

        // The user does not choose where they are standing. Refusing to answer
        // would leave them with nothing in exactly the situation the service
        // exists for, so the route is returned and marked instead.
        assertFalse(
                result.getPath().isEmpty(),
                "an exposed start must still get a route"
        );

        assertEquals(
                List.of(start, goal),
                result.getPath()
        );

        assertFalse(
                result.isShelterConstraintSatisfied(),
                "the route exceeds the requested shelter limit and must say so"
        );
    }

    @Test
    @DisplayName("findPathToNearestShelter should reject null arguments")
    void findNearestShelterRejectsNullArguments() {

        Junction start =
                new Junction(
                        "A",
                        true
                );

        graph.addJunction(start);

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> pathFinder.findPathToNearestShelter(
                        null,
                        params
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> pathFinder.findPathToNearestShelter(
                        start,
                        null
                )
        );
    }

    @Test
    @DisplayName("Should find path to nearest shelter")
    void findsPathToNearestShelter() {

        Junction start =
                new Junction(
                        "START",
                        false
                );

        Junction middle =
                new Junction(
                        "MIDDLE",
                        false
                );

        Junction shelter =
                new Junction(
                        "SHELTER",
                        true
                );

        graph.addJunction(start);
        graph.addJunction(middle);
        graph.addJunction(shelter);

        connect(
                start,
                middle,
                2.0
        );

        connect(
                middle,
                shelter,
                3.0
        );

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        PathResult result =
                pathFinder.findPathToNearestShelter(
                        start,
                        params
                );

        assertEquals(
                List.of(
                        start,
                        middle,
                        shelter
                ),
                result.getPath()
        );

        double expectedTime =
                5.0 * params.getPaceMultiplier();

        assertEquals(
                expectedTime,
                result.getTotalTime(),
                0.001
        );
    }

    @Test
    @DisplayName("Nearest shelter from shelter itself should return single-junction path")
    void startAlreadyShelter() {

        Junction shelter =
                new Junction(
                        "S",
                        true
                );

        graph.addJunction(shelter);

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        1.0
                );

        PathResult result =
                pathFinder.findPathToNearestShelter(
                        shelter,
                        params
                );

        assertEquals(
                List.of(shelter),
                result.getPath()
        );

        assertEquals(
                0.0,
                result.getTotalTime(),
                0.001
        );

        assertEquals(
                0.0,
                result.getTotalCost(),
                0.001
        );
    }

    @Test
    @DisplayName("Should return no path to shelter when no shelter exists")
    void noShelterExists() {

        Junction a =
                new Junction(
                        "A",
                        false
                );

        Junction b =
                new Junction(
                        "B",
                        false
                );

        graph.addJunction(a);
        graph.addJunction(b);

        connect(
                a,
                b,
                2.0
        );

        DijkstraPathFinder pathFinder =
                pathFinderFor(graph);

        RouteParams params =
                new RouteParams(
                        WalkingPace.AVERAGE,
                        0.0
                );

        PathResult result =
                pathFinder.findPathToNearestShelter(
                        a,
                        params
                );

        assertTrue(
                result.getPath().isEmpty()
        );
    }

    /*
     * Helper method.
     *
     * If your RoadSegment constructor automatically adds
     * itself to the source junction, remove addOutgoingRoad.
     */
    private void connect(
            Junction from,
            Junction to,
            double travelTime
    ) {

        RoadSegment segment =
                new RoadSegment(
                        to,
                        from,
                        travelTime
                );

        from.addOutgoing(segment);
    }
}