package fireroute.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class ApiValidationTest {
    @Autowired MockMvc mvc;

    @Test void normalRouteRequiresBothJunctionIds() throws Exception {
        mvc.perform(get("/api/routes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.sourceId").value("sourceId must not be blank"))
                .andExpect(jsonPath("$.errors.destinationId").value("destinationId must not be blank"));
    }

    @Test void emergencyRouteRejectsNegativeTuningValues() throws Exception {
        mvc.perform(get("/api/routes/emergency")
                        .param("sourceId", "123")
                        .param("fearFactor", "-1")
                        .param("maxShelterMinutes", "-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fearFactor").value("fearFactor must not be negative"))
                .andExpect(jsonPath("$.errors.maxShelterMinutes").value("maxShelterMinutes must not be negative"));
    }

    @Test void nearestJunctionRejectsInvalidLatitude() throws Exception {
        mvc.perform(get("/api/junctions/nearest").param("lat", "91").param("lon", "34.77"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").value("lat must be at most 90"));
    }

    @Test void nearestJunctionRejectsInvalidLongitude() throws Exception {
        mvc.perform(get("/api/junctions/nearest").param("lat", "32.08").param("lon", "181"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lon").value("lon must be at most 180"));
    }

    @Test void nearestJunctionRejectsAValidCoordinateOutsideTheServiceArea() throws Exception {
        mvc.perform(get("/api/junctions/nearest").param("lat", "31.7683").param("lon", "35.2137"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Location Outside Coverage"))
                .andExpect(jsonPath("$.maximumMeters").value(250));
    }

    @Test void nearestJunctionStillAcceptsACoordinateInsideTheServiceArea() throws Exception {
        mvc.perform(get("/api/junctions/nearest").param("lat", "32.068").param("lon", "34.774"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test void routeReturns404ForAnUnknownJunction() throws Exception {
        mvc.perform(get("/api/routes/emergency").param("sourceId", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Junction Not Found"))
                .andExpect(jsonPath("$.detail").value("Junction not found: does-not-exist"));
    }

    @Test void nearestJunctionReportsAMissingParameterAs400() throws Exception {
        mvc.perform(get("/api/junctions/nearest").param("lat", "32.08"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.parameter").value("lon"));
    }

    @Test void nearestJunctionReportsMalformedNumbersAs400() throws Exception {
        mvc.perform(get("/api/junctions/nearest").param("lat", "abc").param("lon", "34.77"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.parameter").value("lat"));
    }
}
