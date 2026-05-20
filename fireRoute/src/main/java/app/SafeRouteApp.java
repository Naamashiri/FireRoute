package app;

import data.*;
import model.*;
import routing.*;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        JsonDataLoader loader = new JsonDataLoader();

        try {
            // 1. טעינת נתונים מהקבצים האמיתיים
            Graph graph = loader.loadGraph("src/main/resources/map.json");
            List<RiskZone> zones = loader.loadRiskZones("src/main/resources/alerts.json");

            // 2. בניית האינדקס המרחבי (ביצועים O(1))
            ZoneIndex zoneIndex = new ZoneIndex();
            zoneIndex.build(graph, zones);

            // 3. יצירת מעריך הסיכונים וה-PathFinder
            RiskEvaluator evaluator = new RiskEvaluator(zoneIndex);
            PathFinder pathFinder = new PathFinder(graph, evaluator);

            // 4. הגדרת פרמטרי נסיעה (כאן את שולטת על רמת הפחד!)
            RouteParams params = new RouteParams(
                    10.0, // fearFactor: כמה אנחנו מפחדים מסיכון (10 = גבוה מאוד)
                    2.0   // maxShelterMinutes: חייבים מקלט תוך 2 דקות
            );

            // 5. הרצת ניווט: ממרכז דיזינגוף לאלנבי
            Junction start = graph.getJunctionById("Dizengoff_Center");
            Junction goal = graph.getJunctionById("Rothschild_Allenby");

            System.out.println("\n--- Calculating Safe Route ---");
            PathResult result = pathFinder.findPath(start, goal, params);

            // 6. הצגת התוצאות
            if (result.isPathFound()) {
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