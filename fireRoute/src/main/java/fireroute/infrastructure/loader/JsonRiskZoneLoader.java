package fireroute.infrastructure.loader;

import fireroute.domain.risk.RiskZone;

import java.nio.file.Path;
import java.util.List;

/**
 * Stub: risk-zone JSON parsing isn't implemented yet, so this always
 * returns an empty list (no risk zones loaded).
 */
public class JsonRiskZoneLoader {
    public List<RiskZone> load(Path filePath) {
        return List.of();
    }
}
