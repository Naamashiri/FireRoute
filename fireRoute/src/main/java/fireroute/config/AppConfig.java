package fireroute.config;

import fireroute.application.JunctionLocator;
import fireroute.application.ShelterService;
import fireroute.domain.graph.Graph;
import fireroute.domain.routing.DijkstraPathFinder;
import fireroute.domain.routing.PathFinder;
import fireroute.domain.routing.RouteCostCalculator;
import fireroute.domain.routing.ShelterMap;
import fireroute.domain.alert.AlertState;
import fireroute.domain.shelter.ShelterRepository;
import fireroute.infrastructure.loader.JsonDataLoader;
import fireroute.infrastructure.loader.ShelterLoader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
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
     * changes while the application runs. It starts quiet, so a freshly started
     * service routes on walking time alone until an alert source says otherwise.
     */
    @Bean
    public AlertState alertState(
            @Value("${fireroute.area.id:ever-hayarkon}") String areaId
    ) {
        return new AlertState(areaId);
    }

    /**
     * Walking time from every junction to its nearest shelter, by multi-source
     * Dijkstra over the reversed graph.
     *
     * Built here rather than inside the path finder because two collaborators
     * need it — the finder, to keep routes near cover, and the cost calculator,
     * to price exposure. One instance means one definition of "near a shelter",
     * and the whole-graph sweep is paid once at startup instead of per request.
     */
    @Bean
    public ShelterMap shelterMap(Graph graph) {
        ShelterMap shelterMap = new ShelterMap(graph);
        shelterMap.compute();
        return shelterMap;
    }

    @Bean
    public RouteCostCalculator routeCostCalculator(ShelterMap shelterMap) {
        return new RouteCostCalculator(shelterMap);
    }

    /**
     * Declared as PathFinder, not DijkstraPathFinder, so everything downstream
     * depends on the abstraction and the algorithm stays replaceable from this
     * one line.
     */
    @Bean
    public PathFinder pathFinder(
            Graph graph,
            ShelterMap shelterMap,
            RouteCostCalculator routeCostCalculator
    ) {
        return new DijkstraPathFinder(graph, shelterMap, routeCostCalculator);
    }

    @Bean
    public JunctionLocator junctionLocator(
            Graph graph,
            @Value("${fireroute.routing.max-snap-distance-meters:250}") double maxSnapDistanceMeters
    ) {
        return new JunctionLocator(graph, maxSnapDistanceMeters);
    }

    @Bean
    public ShelterService shelterService(ShelterRepository shelterRepository) {
        return new ShelterService(shelterRepository);
    }

}
