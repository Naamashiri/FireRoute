package alerts;
import org.junit.jupiter.api.Test;                 // האנוטציה @Test
import static org.junit.jupiter.api.Assertions.*;  // assertEquals, assertTrue...

public class AlertParserTest {
    @Test
    public void testParseAlert() {
        String json = """
                {
                    "id": "alert1",
                    "title": "Test Alert",
                    "cat": "1",
                    "data": ["area1", "area2"]
                }
                """;

        AlertParser parser = new AlertParser();
        var alerts = parser.parse(json);

        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals("alert1", alert.alertId);
        assertEquals(2, alert.affectedAreas.size());
        assertTrue(alert.affectedAreas.contains("area1"));
        assertTrue(alert.affectedAreas.contains("area2"));
    }

}
