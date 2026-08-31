package fireroute.application;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JunctionLocatorTest {
    private final Graph graph = graphWithOneJunction();

    @Test void returnsAJunctionWithinTheSnapLimit() {
        JunctionLocator locator = new JunctionLocator(graph, 100);
        assertThat(locator.nearestTo(32.0801, 34.7801).getId()).isEqualTo("J1");
    }

    @Test void rejectsAPointBeyondTheSnapLimit() {
        JunctionLocator locator = new JunctionLocator(graph, 100);
        assertThatThrownBy(() -> locator.nearestTo(32.09, 34.79))
                .isInstanceOf(LocationOutsideCoverageException.class);
    }

    @Test void rejectsNonFiniteCoordinatesEvenOutsideHttp() {
        JunctionLocator locator = new JunctionLocator(graph, 100);
        assertThatThrownBy(() -> locator.nearestTo(Double.NaN, 34.78))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Graph graphWithOneJunction() {
        Graph graph = new Graph();
        graph.addJunction(new Junction("J1", 34.78, 32.08, false));
        return graph;
    }
}
