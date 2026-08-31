package fireroute.api.controller;

import fireroute.api.ApiExceptionHandler;
import fireroute.api.mapper.ShelterMapper;
import fireroute.application.ShelterNotFoundException;
import fireroute.application.ShelterService;
import fireroute.domain.graph.GeoPoint;
import fireroute.domain.shelter.Shelter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SheltersControllerTest {
    private final ShelterService service = mock(ShelterService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new SheltersController(service, new ShelterMapper()))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    private final Shelter shelter = new Shelter("S1", "Dizengoff 100",
            new GeoPoint(34.774, 32.078), true);

    @Test void returnsAllShelters() throws Exception {
        when(service.getAllShelters()).thenReturn(List.of(shelter));

        mvc.perform(get("/api/v1/shelters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("S1"))
                .andExpect(jsonPath("$[0].latitude").value(32.078))
                .andExpect(jsonPath("$[0].longitude").value(34.774));
    }

    @Test void returnsOneShelter() throws Exception {
        when(service.getShelterById("S1")).thenReturn(shelter);

        mvc.perform(get("/api/v1/shelters/S1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Dizengoff 100"))
                .andExpect(jsonPath("$.accessible").value(true));
    }

    @Test void returns404WhenShelterDoesNotExist() throws Exception {
        when(service.getShelterById("missing")).thenThrow(new ShelterNotFoundException("missing"));

        mvc.perform(get("/api/v1/shelters/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Shelter Not Found"));
    }
}
