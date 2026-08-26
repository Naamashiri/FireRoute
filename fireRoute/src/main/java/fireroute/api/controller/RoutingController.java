package fireroute.api.controller;

import fireroute.api.dto.RouteRequest;
import fireroute.api.dto.RouteResponse;
import fireroute.api.mapper.RouteMapper;
import fireroute.application.RoutingService;
import fireroute.routing.PathResult;
import fireroute.routing.RouteParams;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for route calculation.
 *
 * The controller receives, delegates and returns — nothing else. Translation
 * between the wire format and the domain belongs to RouteMapper, and the routing
 * decisions belong to RoutingService; keeping both out of here is what makes the
 * service testable without HTTP and the mapper testable without a controller.
 */
@RestController
@RequestMapping("/api")
public class RoutingController {

    private final RoutingService routingService;
    private final RouteMapper routeMapper;

    public RoutingController(RoutingService routingService, RouteMapper routeMapper) {
        if (routingService == null) {
            throw new IllegalArgumentException("routingService must not be null");
        }
        if (routeMapper == null) {
            throw new IllegalArgumentException("routeMapper must not be null");
        }

        this.routingService = routingService;
        this.routeMapper = routeMapper;
    }

    /**
     * Calculating a route reads state, it never changes any, so GET is the
     * honest verb for it — and it keeps every route reachable from a plain URL,
     * cacheable, and shareable as a link.
     *
     * RouteRequest carries no annotation: Spring binds query parameters straight
     * into the record's constructor for any non-simple parameter type. Should the
     * request ever grow a nested field — a list of waypoints, an area to avoid —
     * a query string stops being expressive enough and this becomes a POST with
     * a JSON body.
     */
    @GetMapping("/routes")
    public RouteResponse route(RouteRequest request) {
        RouteParams params = routeMapper.toRouteParams(request);

        PathResult pathResult = routingService.calculateRoute(
                request.sourceId(),
                request.destinationId(),
                params
        );

        return routeMapper.toRouteResponse(pathResult);
    }
}
