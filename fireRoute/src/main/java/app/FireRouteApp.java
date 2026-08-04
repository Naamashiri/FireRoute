package app;

import data.JsonRiskZoneLoader;
import data.RiskEvaluator;
import data.RiskZone;
import data.ZoneIndex;
import engine.FireRouteEngine;
import graph.Graph;
import routing.DijkstraPathFinder;
import routing.PathResult;
import routing.RouteParams;
import routing.ShelterMap;

import java.nio.file.Path;
import java.util.List;

public class FireRouteApp {

    public static void main(String[] args) {

        try {
            // 1. טעינת גרף הכבישים
            Graph graph = loadGraph();

            // 2. טעינת אזורי סיכון
            JsonRiskZoneLoader riskZoneLoader =
                    new JsonRiskZoneLoader();

            List<RiskZone> riskZones =
                    riskZoneLoader.load(
                            Path.of("src/main/resources/risk-zones.json")
                    );

            // 3. יצירת אינדקס לאזורי הסיכון
            ZoneIndex zoneIndex =
                    new ZoneIndex(riskZones);

            // 4. יצירת מחשב הסיכון
            RiskEvaluator riskEvaluator =
                    new RiskEvaluator(zoneIndex);

            // 5. חישוב או טעינת מידע על מקלטים
            ShelterMap shelterMap =
                    createShelterMap(graph);

            // 6. יצירת אלגוריתם הניתוב
            DijkstraPathFinder pathFinder =
                    new DijkstraPathFinder(
                            graph,
                            riskEvaluator,
                            shelterMap
                    );

            // 7. יצירת המנוע
            FireRouteEngine engine =
                    new FireRouteEngine(graph, pathFinder);

            // 8. קלט זמני
            RouteParams params =
                    createRouteParams();

            String sourceId = "A";
            String destinationId = "F";

            // 9. חישוב המסלול
            PathResult result =
                    engine.calculateRoute(
                            sourceId,
                            destinationId,
                            params
                    );

            // 10. הצגת התוצאה
            printResult(result);

        } catch (Exception exception) {
            System.err.println(
                    "Failed to start FireRoute: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private static Graph loadGraph() {
        Graph graph = new Graph();

        /*
         * כרגע:
         * אפשר לבנות כאן גרף ידני לצורך בדיקה.
         *
         * בהמשך:
         * GraphLoader graphLoader = new JsonGraphLoader();
         * return graphLoader.load(...);
         */

        return graph;
    }

    private static ShelterMap createShelterMap(Graph graph) {
        /*
         * כאן תטעני מקלטים ותפעילי
         * Multi-source Dijkstra אם זה מה שהמחלקה שלך עושה.
         */

        return new ShelterMap();
    }

    private static RouteParams createRouteParams() {
        /*
         * להתאים ל-constructor האמיתי שלך.
         */

        return new RouteParams(
                // לדוגמה: fearLevel, speed, maxShelterDistance
        );
    }

    private static void printResult(PathResult result) {
        System.out.println("FireRoute result:");
        System.out.println(result);
    }
}