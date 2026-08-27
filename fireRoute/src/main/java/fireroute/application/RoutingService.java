package fireroute.application;

import fireroute.routing.PathResult;
import fireroute.routing.RouteParams;
import org.springframework.stereotype.Service;

@Service

public class RoutingService {

    private final FireRouteEngine engine;

    public RoutingService(FireRouteEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }
        this.engine = engine;
    }

    /**
     * מחשב מסלול מותאם אישית בין שתי נקודות.
     */
    public PathResult calculateRoute(String sourceId, String destinationId, RouteParams params) {
        if (sourceId == null || sourceId.isBlank() || destinationId == null || destinationId.isBlank()) {
            throw new IllegalArgumentException("sourceId and destinationId must not be null or blank");
        }
        if (params == null) {
            params = new RouteParams(); // שימוש בערכי ברירת מחדל אם לא סופקו
        }

        return engine.calculateRoute(sourceId, destinationId, params);
    }

    /**
     * ניתוב חירום למקלט הקרוב ביותר מנקודת המוצא.
     */
    public PathResult calculateEmergencyRoute(String sourceId, RouteParams params) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be null or blank");
        }
        if (params == null) {
            params = new RouteParams();
        }

        return engine.calculateEmergencyRoute(sourceId, params);
    }
}