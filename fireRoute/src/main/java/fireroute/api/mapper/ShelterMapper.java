package fireroute.api.mapper;

import fireroute.api.dto.ShelterResponse;
import fireroute.domain.shelter.Shelter;
import org.springframework.stereotype.Component;

@Component
public class ShelterMapper {
    public ShelterResponse toResponse(Shelter shelter) {
        if (shelter == null) throw new IllegalArgumentException("shelter must not be null");
        return new ShelterResponse(shelter.getId(), shelter.getAddress(),
                shelter.getLocation().lat(), shelter.getLocation().lon(), shelter.isAccessible());
    }
}
