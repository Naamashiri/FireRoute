package fireroute.api.controller;

import fireroute.api.dto.JunctionResponse;
import fireroute.application.JunctionLocator;
import fireroute.domain.graph.Junction;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Junctions as a resource of their own: they are not routes, and they answer a
 * different question with a different collaborator.
 */
@RestController
@RequestMapping("/api")
public class JunctionController {

    private final JunctionLocator junctionLocator;

    public JunctionController(JunctionLocator junctionLocator) {
        if (junctionLocator == null) {
            throw new IllegalArgumentException("junctionLocator must not be null");
        }
        this.junctionLocator = junctionLocator;
    }

    /**
     * The junction to route from, given where the caller actually is.
     *
     * This is the first call any real client makes: a phone has a GPS fix, not a
     * junction id.
     */
    @GetMapping("/junctions/nearest")
    public JunctionResponse nearest(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        Junction junction = junctionLocator.nearestTo(lat, lon);

        return new JunctionResponse(
                junction.getId(),
                junction.getY(),   // Junction stores y as latitude
                junction.getX(),   // and x as longitude
                junction.isShelter()
        );
    }
}
