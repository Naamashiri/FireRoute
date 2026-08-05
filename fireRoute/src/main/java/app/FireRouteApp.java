package app;

import engine.FireRouteEngine;
import graph.Graph;
import loader.JsonRiskZoneLoader;
import risk.RiskEvaluator;
import risk.RiskZone;
import risk.ZoneIndex;
import routing.PathFinder;
import routing.PathResult;
import routing.RouteParams;

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
            ZoneIndex zoneIndex = new ZoneIndex();
            zoneIndex.build(graph, riskZones);

            // 4. יצירת מחשב הסיכון
            RiskEvaluator riskEvaluator =
                    new RiskEvaluator(zoneIndex);

            // 5. יצירת אלגוריתם הניתוב (בונה את ShelterMap שלו פנימית)
            PathFinder pathFinder =
                    new PathFinder(graph, riskEvaluator);

            // 6. יצירת המנוע
            FireRouteEngine engine =
                    new FireRouteEngine(graph, pathFinder);

            // 7. קלט זמני
            RouteParams params =
                    createRouteParams();

            String sourceId = "A";
            String destinationId = "F";

            // 8. חישוב המסלול
            PathResult result =
                    engine.calculateRoute(
                            sourceId,
                            destinationId,
                            params
                    );

            // 9. הצגת התוצאה
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

    private static RouteParams createRouteParams() {
        return new RouteParams();
    }

    private static void printResult(PathResult result) {
        System.out.println("FireRoute result:");
        System.out.println(result);
    }
}