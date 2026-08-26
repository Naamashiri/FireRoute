package fireroute.application;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import fireroute.routing.PathFinder;
import fireroute.routing.PathResult;
import fireroute.routing.RouteParams;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FireRouteEngineTest {

    private Graph graph;
    private TestPathFinder pathFinder;
    private FireRouteEngine engine;

    private Junction a;
    private Junction b;

    @BeforeEach
    void setUp() {
        graph = new Graph();

        a = new Junction("A", true);
        b = new Junction("B", true);

        graph.addJunction(a);
        graph.addJunction(b);

        pathFinder = new TestPathFinder();
        engine = new FireRouteEngine(graph, pathFinder);
    }

    // ---------------------------------------------------------
    // Constructor validation
    // ---------------------------------------------------------

    @Test
    @DisplayName("Constructor should reject null graph")
    void constructorRejectsNullGraph() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FireRouteEngine(null, pathFinder)
        );
    }

    @Test
    @DisplayName("Constructor should reject null pathFinder")
    void constructorRejectsNullPathFinder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FireRouteEngine(graph, null)
        );
    }

    // ---------------------------------------------------------
    // calculateRoute validation
    // ---------------------------------------------------------

    @Test
    @DisplayName("calculateRoute should reject null sourceId")
    void calculateRouteRejectsNullSourceId() {
        RouteParams params = new RouteParams();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateRoute(
                        null,
                        "B",
                        params
                )
        );
    }

    @Test
    @DisplayName("calculateRoute should reject blank sourceId")
    void calculateRouteRejectsBlankSourceId() {
        RouteParams params = new RouteParams();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateRoute(
                        "   ",
                        "B",
                        params
                )
        );
    }

    @Test
    @DisplayName("calculateRoute should reject null destinationId")
    void calculateRouteRejectsNullDestinationId() {
        RouteParams params = new RouteParams();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateRoute(
                        "A",
                        null,
                        params
                )
        );
    }

    @Test
    @DisplayName("calculateRoute should reject blank destinationId")
    void calculateRouteRejectsBlankDestinationId() {
        RouteParams params = new RouteParams();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateRoute(
                        "A",
                        "   ",
                        params
                )
        );
    }

    @Test
    @DisplayName("calculateRoute should reject null params")
    void calculateRouteRejectsNullParams() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateRoute(
                        "A",
                        "B",
                        null
                )
        );
    }

    @Test
    @DisplayName("calculateRoute should reject unknown source junction")
    void calculateRouteRejectsUnknownSource() {
        RouteParams params = new RouteParams();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateRoute(
                                "UNKNOWN",
                                "B",
                                params
                        )
                );

        assertTrue(
                exception.getMessage().contains("UNKNOWN")
        );
    }

    @Test
    @DisplayName("calculateRoute should reject unknown destination junction")
    void calculateRouteRejectsUnknownDestination() {
        RouteParams params = new RouteParams();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateRoute(
                                "A",
                                "UNKNOWN",
                                params
                        )
                );

        assertTrue(
                exception.getMessage().contains("UNKNOWN")
        );
    }

    // ---------------------------------------------------------
    // calculateRoute delegation
    // ---------------------------------------------------------

    @Test
    @DisplayName("calculateRoute should resolve junction IDs and delegate to PathFinder")
    void calculateRouteDelegatesToPathFinder() {
        RouteParams params = new RouteParams();

        PathResult expected =
                new PathResult(
                        List.of(a, b),
                        5.0,
                        5.0,
                        0.0,
                        0.0
                );

        pathFinder.normalRouteResult = expected;

        PathResult actual =
                engine.calculateRoute(
                        "A",
                        "B",
                        params
                );

        assertSame(expected, actual);

        assertSame(
                a,
                pathFinder.receivedStart
        );

        assertSame(
                b,
                pathFinder.receivedGoal
        );

        assertSame(
                params,
                pathFinder.receivedParams
        );

        assertEquals(
                1,
                pathFinder.findPathCallCount
        );
    }

    @Test
    @DisplayName("calculateRoute should return noPath result from PathFinder unchanged")
    void calculateRouteReturnsNoPathUnchanged() {
        RouteParams params = new RouteParams();

        PathResult noPath =
                PathResult.noPath();

        pathFinder.normalRouteResult =
                noPath;

        PathResult result =
                engine.calculateRoute(
                        "A",
                        "B",
                        params
                );

        assertSame(
                noPath,
                result
        );

        assertFalse(
                result.hasPath()
        );
    }

    @Test
    @DisplayName("calculateRoute should allow source and destination to be the same junction")
    void calculateRouteAllowsSameSourceAndDestination() {
        RouteParams params = new RouteParams();

        PathResult expected =
                new PathResult(
                        List.of(a),
                        0.0,
                        0.0,
                        0.0,
                        0.0
                );

        pathFinder.normalRouteResult =
                expected;

        PathResult result =
                engine.calculateRoute(
                        "A",
                        "A",
                        params
                );

        assertSame(
                expected,
                result
        );

        assertSame(
                a,
                pathFinder.receivedStart
        );

        assertSame(
                a,
                pathFinder.receivedGoal
        );
    }

    // ---------------------------------------------------------
    // calculateEmergencyRoute validation
    // ---------------------------------------------------------

    @Test
    @DisplayName("calculateEmergencyRoute should reject null sourceId")
    void emergencyRouteRejectsNullSourceId() {
        RouteParams params = new RouteParams();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateEmergencyRoute(
                        null,
                        params
                )
        );
    }

    @Test
    @DisplayName("calculateEmergencyRoute should reject blank sourceId")
    void emergencyRouteRejectsBlankSourceId() {
        RouteParams params = new RouteParams();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateEmergencyRoute(
                        "   ",
                        params
                )
        );
    }

    @Test
    @DisplayName("calculateEmergencyRoute should reject null params")
    void emergencyRouteRejectsNullParams() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateEmergencyRoute(
                        "A",
                        null
                )
        );
    }

    @Test
    @DisplayName("calculateEmergencyRoute should reject unknown source junction")
    void emergencyRouteRejectsUnknownSource() {
        RouteParams params = new RouteParams();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateEmergencyRoute(
                                "UNKNOWN",
                                params
                        )
                );

        assertTrue(
                exception.getMessage().contains("UNKNOWN")
        );
    }

    // ---------------------------------------------------------
    // calculateEmergencyRoute delegation
    // ---------------------------------------------------------

    @Test
    @DisplayName("calculateEmergencyRoute should delegate to nearest shelter path finder")
    void emergencyRouteDelegatesToPathFinder() {
        RouteParams params = new RouteParams();

        PathResult expected =
                new PathResult(
                        List.of(a, b),
                        3.0,
                        4.0,
                        0.2,
                        0.0
                );

        pathFinder.emergencyRouteResult =
                expected;

        PathResult actual =
                engine.calculateEmergencyRoute(
                        "A",
                        params
                );

        assertSame(
                expected,
                actual
        );

        assertSame(
                a,
                pathFinder.receivedEmergencyStart
        );

        assertSame(
                params,
                pathFinder.receivedEmergencyParams
        );

        assertEquals(
                1,
                pathFinder.findEmergencyPathCallCount
        );
    }

    @Test
    @DisplayName("calculateEmergencyRoute should return noPath result unchanged")
    void emergencyRouteReturnsNoPathUnchanged() {
        RouteParams params = new RouteParams();

        PathResult noPath =
                PathResult.noPath();

        pathFinder.emergencyRouteResult =
                noPath;

        PathResult result =
                engine.calculateEmergencyRoute(
                        "A",
                        params
                );

        assertSame(
                noPath,
                result
        );

        assertFalse(
                result.hasPath()
        );
    }

    // ---------------------------------------------------------
    // Test double for PathFinder
    // ---------------------------------------------------------

    private static class TestPathFinder implements PathFinder {

        private PathResult normalRouteResult =
                PathResult.noPath();

        private PathResult emergencyRouteResult =
                PathResult.noPath();

        private Junction receivedStart;
        private Junction receivedGoal;
        private RouteParams receivedParams;

        private Junction receivedEmergencyStart;
        private RouteParams receivedEmergencyParams;

        private int findPathCallCount = 0;
        private int findEmergencyPathCallCount = 0;

        @Override
        public PathResult findPath(
                Junction start,
                Junction goal,
                RouteParams params
        ) {
            findPathCallCount++;

            receivedStart = start;
            receivedGoal = goal;
            receivedParams = params;

            return normalRouteResult;
        }

        @Override
        public PathResult findPathToNearestShelter(
                Junction start,
                RouteParams params
        ) {
            findEmergencyPathCallCount++;

            receivedEmergencyStart = start;
            receivedEmergencyParams = params;

            return emergencyRouteResult;
        }
    }
}