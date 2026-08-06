package alerts;
// represents a collection of active alerts
import java.util.List;

public class ActiveAlerts {

    private final List<Alert> alerts;

    public ActiveAlerts(List<Alert> alerts) {
        this.alerts = List.copyOf(alerts);
    }

    public List<Alert> getAlerts() {
        return alerts;
    }
    
    public static ActiveAlerts empty() {
        return new ActiveAlerts(List.of());
    }
}