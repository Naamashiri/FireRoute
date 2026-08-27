package fireroute.config;

import fireroute.application.FireRouteEngine;
import fireroute.domain.graph.Graph;
import fireroute.domain.risk.RiskEvaluator;
import fireroute.domain.risk.RiskProfile;
import fireroute.domain.shelter.ShelterRepository;
import fireroute.infrastructure.loader.JsonDataLoader;
import fireroute.infrastructure.loader.ShelterLoader;
import fireroute.routing.DijkstraPathFinder;
import fireroute.routing.PathFinder;
import fireroute.routing.RouteCostCalculator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Assembles the domain object graph and hands it to the container.
 *
 * None of the classes constructed here carry a Spring annotation of their own:
 * the domain has no idea a framework exists, and wiring is treated as a detail
 * of the runtime rather than a property of the objects. That is what keeps the
 * algorithm unit-testable without an ApplicationContext.
 *
 * Every method declares the dependencies it needs as parameters; Spring resolves
 * them by type and calls the methods in the right order. They are never called
 * directly from one another.
 *
 * Loaders are instantiated inline rather than exposed as beans on purpose. They
 * run once, at startup; publishing them would invite a controller to inject one
 * and re-read a file in the middle of a request.
 *
 * Resource paths default to values that work with no configuration file present,
 * and can be overridden per environment without recompiling.
 */
@Configuration
public class AppConfig {

    /**
     * The road network. Loading failure is intentionally fatal: a routing service
     * without a map is not a routing service, and failing at startup is far easier
     * to diagnose than every request returning an error later on.
     */
    @Bean
    public Graph graph(
            @Value("${fireroute.data.map:map.json}") String mapResource
    ) throws IOException {
        return new JsonDataLoader().loadGraphFromResources(mapResource);
    }

    /**
     * Shelters as real objects (id, address, location). Independent of the graph.
     */
    @Bean
    public ShelterRepository shelterRepository(
            @Value("${fireroute.data.shelters:shelters.json}") String sheltersResource
    ) throws IOException {
        return new ShelterLoader().loadFromResources(sheltersResource);
    }

    /**
     * Alert state for the single area this service covers.
     *
     * Mutable and shared on purpose: it is the one thing in the domain that
     * changes while the application runs. Recording an alert here is what makes
     * the next route calculation avoid the area, without rebuilding anything.
     *
     * It starts quiet — no alerts recorded, none active — so a freshly started
     * service routes on walking time alone until something tells it otherwise.
     */
    @Bean
    public RiskProfile areaRiskProfile(
            @Value("${fireroute.area.id:ever-hayarkon}") String areaId
    ) {
        return new RiskProfile(areaId, 0, null, false);
    }

    @Bean
    public RiskEvaluator riskEvaluator(RiskProfile areaRiskProfile) {
        return new RiskEvaluator(areaRiskProfile);
    }

    @Bean
    public RouteCostCalculator routeCostCalculator(
            RiskEvaluator riskEvaluator,
            ShelterRepository shelterRepository
    ) {
        return new RouteCostCalculator(riskEvaluator, shelterRepository);
    }

    /**
     * Declared as PathFinder, not DijkstraPathFinder, so everything downstream
     * depends on the abstraction and the algorithm stays replaceable from this
     * one line.
     *
     * The constructor runs a multi-source Dijkstra over the whole graph to build
     * the shelter distance map. Expensive, but paid once here at startup instead
     * of on every request.
     */
    @Bean
    public PathFinder pathFinder(
            Graph graph,
            RiskEvaluator riskEvaluator,
            RouteCostCalculator routeCostCalculator
    ) {
        return new DijkstraPathFinder(graph, riskEvaluator, routeCostCalculator);
    }

    @Bean
    public FireRouteEngine fireRouteEngine(Graph graph, PathFinder pathFinder) {
        return new FireRouteEngine(graph, pathFinder);
    }
}
