package fireroute.config;

import fireroute.application.FireRouteEngine;
import fireroute.domain.graph.Graph;
import fireroute.domain.risk.RiskEvaluator;
import fireroute.domain.risk.RiskZone;
import fireroute.domain.risk.ZoneIndex;
import fireroute.domain.shelter.ShelterRepository;
import fireroute.infrastructure.loader.JsonDataLoader;
import fireroute.infrastructure.loader.JsonRiskZoneLoader;
import fireroute.infrastructure.loader.ShelterLoader;
import fireroute.routing.DijkstraPathFinder;
import fireroute.routing.PathFinder;
import fireroute.routing.RouteCostCalculator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
            @Value("${fireroute.data.map:static/map.json}") String mapResource
    ) throws IOException {
        return new JsonDataLoader().loadGraphFromResources(mapResource);
    }

    /**
     * Shelters as real objects (id, address, location). Independent of the graph.
     */
    @Bean
    public ShelterRepository shelterRepository(
            @Value("${fireroute.data.shelters:static/shelters.json}") String sheltersResource
    ) throws IOException {
        return new ShelterLoader().loadFromResources(sheltersResource);
    }

    /**
     * Maps every junction to the risk zone containing it.
     *
     * ZoneIndex is not usable straight out of its constructor — build() is what
     * populates it. It is called here so that a half-initialised index can never
     * escape into the rest of the system, where an empty map would silently read
     * as "no risk anywhere".
     *
     * JsonRiskZoneLoader is still a stub returning an empty list, so today this
     * yields an index with no zones. The seam is what matters: replacing the stub
     * changes nothing outside the loader.
     */
    @Bean
    public ZoneIndex zoneIndex(
            Graph graph,
            @Value("${fireroute.data.riskZones:static/alerts.json}") String riskZonesResource
    ) {
        List<RiskZone> zones = new JsonRiskZoneLoader().load(Path.of(riskZonesResource));

        ZoneIndex zoneIndex = new ZoneIndex();
        zoneIndex.build(graph, zones);
        return zoneIndex;
    }

    @Bean
    public RiskEvaluator riskEvaluator(ZoneIndex zoneIndex) {
        return new RiskEvaluator(zoneIndex);
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
