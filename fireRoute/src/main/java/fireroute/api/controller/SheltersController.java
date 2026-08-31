package fireroute.api.controller;

import fireroute.api.dto.ShelterResponse;
import fireroute.api.mapper.ShelterMapper;
import fireroute.application.ShelterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shelters")
public class SheltersController {
    private final ShelterService service;
    private final ShelterMapper mapper;

    public SheltersController(ShelterService service, ShelterMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ShelterResponse> all() {
        return service.getAllShelters().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ShelterResponse byId(@PathVariable String id) {
        return mapper.toResponse(service.getShelterById(id));
    }
}
