package fireroute.api.controller;

import fireroute.domain.graph.Graph;
import fireroute.domain.graph.Junction;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the public API with the real Spring wiring and production data files. */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired Graph graph;
    @Autowired ShelterRepository shelterRepository;

    @Test void nearestJunctionReturnsAUsableJunction() throws Exception {
        Junction junction = graph.getJunctions().stream()
                .filter(Junction::hasCoordinates).findFirst().orElseThrow();

        mvc.perform(get("/api/junctions/nearest")
                        .param("lat", junction.getY().toString())
                        .param("lon", junction.getX().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(junction.getId()));
    }

    @Test void normalRouteReturnsAnExplicitNormalResult() throws Exception {
        Junction junction = graph.getJunctions().iterator().next();

        mvc.perform(get("/api/routes")
                        .param("sourceId", junction.getId())
                        .param("destinationId", junction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.routeType").value("NORMAL"))
                .andExpect(jsonPath("$.failureReason").value("NONE"))
                .andExpect(jsonPath("$.destinationShelter").doesNotExist())
                .andExpect(jsonPath("$.alreadyAtShelter").value(false));
    }

    @Test void emergencyRouteExplainsThatTheUserIsAlreadyAtAShelter() throws Exception {
        Junction shelterJunction = graph.getShelters().stream().findFirst().orElseThrow();

        mvc.perform(get("/api/routes/emergency").param("sourceId", shelterJunction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.routeType").value("EMERGENCY"))
                .andExpect(jsonPath("$.failureReason").value("NONE"))
                .andExpect(jsonPath("$.alreadyAtShelter").value(true))
                .andExpect(jsonPath("$.destinationShelter.id").isNotEmpty())
                .andExpect(jsonPath("$.destinationShelter.address").isNotEmpty());
    }

    @Test void sheltersEndpointServesTheLoadedProductionShelters() throws Exception {
        Shelter shelter = shelterRepository.findAll().getFirst();

        mvc.perform(get("/api/v1/shelters/{id}", shelter.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(shelter.getId()))
                .andExpect(jsonPath("$.address").value(shelter.getAddress()));
    }

    @Test void alertEndpointReturnsTheRichStatusContract() throws Exception {
        mvc.perform(get("/api/alerts/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaId").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.lastKnownStatus").isNotEmpty())
                .andExpect(jsonPath("$.alertActive").isBoolean())
                .andExpect(jsonPath("$.stale").isBoolean());
    }

    @Test void openApiDescribesEveryPublicApiGroup() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/routes']").exists())
                .andExpect(jsonPath("$.paths['/api/routes/emergency']").exists())
                .andExpect(jsonPath("$.paths['/api/junctions/nearest']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/shelters']").exists())
                .andExpect(jsonPath("$.paths['/api/alerts/status']").exists());
    }
}
