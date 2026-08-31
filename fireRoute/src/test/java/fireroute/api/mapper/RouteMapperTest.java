package fireroute.api.mapper;

import fireroute.domain.graph.GeoPoint;
import fireroute.domain.graph.Junction;
import fireroute.domain.routing.PathResult;
import fireroute.domain.shelter.Shelter;
import fireroute.infrastructure.shelter.InMemoryShelterRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import fireroute.api.dto.RouteFailureReason;
import fireroute.api.dto.RouteType;

class RouteMapperTest {
    @Test void emergencyResponseContainsThePhysicalShelterDetails() {
        Shelter shelter = new Shelter("S1", "Dizengoff 100", new GeoPoint(34.774, 32.078), true);
        RouteMapper mapper = new RouteMapper(
                new InMemoryShelterRepository(List.of(shelter)), new ShelterMapper());
        Junction destination = new Junction("J1", 34.7741, 32.0781, true);

        var response = mapper.toEmergencyRouteResponse(
                new PathResult(List.of(destination), 0, 0, 0));

        assertThat(response.destinationShelter()).isNotNull();
        assertThat(response.destinationShelter().id()).isEqualTo("S1");
        assertThat(response.routeType()).isEqualTo(RouteType.EMERGENCY);
        assertThat(response.alreadyAtShelter()).isTrue();
    }

    @Test void normalResponseDoesNotPretendItsDestinationIsAShelter() {
        RouteMapper mapper = new RouteMapper(new InMemoryShelterRepository(List.of()), new ShelterMapper());
        Junction destination = new Junction("J1", 34.7741, 32.0781, false);

        var response = mapper.toRouteResponse(new PathResult(List.of(destination), 0, 0, 0));

        assertThat(response.destinationShelter()).isNull();
        assertThat(response.routeType()).isEqualTo(RouteType.NORMAL);
        assertThat(response.alreadyAtShelter()).isFalse();
    }

    @Test void emergencyFailureExplainsThatNoShelterIsReachable() {
        RouteMapper mapper = new RouteMapper(new InMemoryShelterRepository(List.of()), new ShelterMapper());
        var response = mapper.toEmergencyRouteResponse(PathResult.noPath());
        assertThat(response.found()).isFalse();
        assertThat(response.routeType()).isEqualTo(RouteType.EMERGENCY);
        assertThat(response.failureReason()).isEqualTo(RouteFailureReason.NO_REACHABLE_SHELTER);
    }
}
