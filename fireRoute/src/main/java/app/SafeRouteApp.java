package app;

import graph.Graph;
import graph.Junction;
import loader.JsonDataLoader;
import loader.JsonRiskZoneLoader;
import risk.RiskEvaluator;
import risk.RiskZone;
import risk.ZoneIndex;
import routing.PathFinder;
import routing.PathResult;
import routing.RouteParams;
import routing.WalkingPace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SafeRouteApp {
    public static void main(String[] args) {
        JsonDataLoader loader = new JsonDataLoader();

        try {
            // 1. טעינת נתונים מהקבצים האמיתיים
            Graph graph = loader.loadGraph("src/main/resources/map.json");
            List<RiskZone> zones = new JsonRiskZoneLoader().load(Path.of("src/main/resources/alerts.json"));

            // 2. בניית האינדקס המרחבי (ביצועים O(1))
            ZoneIndex zoneIndex = new ZoneIndex();
            zoneIndex.build(graph, zones);

            // 3. יצירת מעריך הסיכונים וה-PathFinder
            RiskEvaluator evaluator = new RiskEvaluator(zoneIndex);
            PathFinder pathFinder = new PathFinder(graph, evaluator);

            // 4. הגדרת פרמטרי נסיעה (כאן את שולטת על רמת הפחד!)
            RouteParams params = new RouteParams(
                    2.0,               // maxShelterMinutes: חייבים מקלט תוך 2 דקות
                    WalkingPace.AVERAGE,
                    10.0               // fearFactor: כמה אנחנו מפחדים מסיכון (10 = גבוה מאוד)
            );

            // 5. הרצת ניווט: ממרכז דיזינגוף לאלנבי
            Junction start = graph.getJunction("Dizengoff_Center");
            Junction goal = graph.getJunction("Rothschild_Allenby");

            System.out.println("\n--- Calculating Safe Route ---");
            PathResult result = pathFinder.findPath(start, goal, params);

            // 6. הצגת התוצאות
            if (result.hasPath()) {
                System.out.println("Path Found!");
                System.out.println("Total Time: " + result.getTotalTime() + " mins");
                System.out.println("Max Risk Encountered: " + (result.getMaxRisk() * 100) + "%");
                System.out.println("Route Stations: " + result.getPath());
            } else {
                System.out.println("No safe path found under current constraints!");
            }

        } catch (IOException e) {
            System.err.println("Critical Error: " + e.getMessage());
        }
    }
}