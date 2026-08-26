package fireroute.routing;

import fireroute.domain.geo.GeoPoint;
import fireroute.domain.graph.Junction;
import fireroute.domain.graph.RoadSegment;
import fireroute.domain.risk.RiskEvaluator;
import fireroute.domain.shelter.Shelter;
import fireroute.domain.shelter.ShelterRepository;

/**
 * מחשבת את העלות המשוקללת של מקטע דרך בהתחשב בזמן הליכה, סיכון אזורי וקרבה למקלטים.
 */
public class RouteCostCalculator {

    private final RiskEvaluator riskEvaluator;
    private final ShelterRepository shelterRepository;
    private static final double MAX_SAFE_TIME_SECONDS = 90.0; // 1.5 minutes

    public RouteCostCalculator(RiskEvaluator riskEvaluator, ShelterRepository shelterRepository) {
        if (riskEvaluator == null || shelterRepository == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.riskEvaluator = riskEvaluator;
        this.shelterRepository = shelterRepository;
    }

    public double calculateCost(RoadSegment segment, RouteParams userParams) {
        if (segment == null || userParams == null) {
            throw new IllegalArgumentException("Segment and userParams cannot be null");
        }

        // 1. חישוב זמן הליכה אמיתי במקטע
        double baseTime = segment.getTravelTime();
        double paceMultiplier = userParams.getPaceMultiplier();
        double actualTime = baseTime * paceMultiplier;

        // 2. שקלול סיכון אזורי
        double segmentRisk = riskEvaluator.getSegmentRisk(segment);
        double fearFactor = userParams.getFearFactor();

        // 3. חישוב קנס מקלט (Shelter Penalty)
        double shelterPenalty = calculateShelterPenalty(segment, userParams);

        // 4. עלות משוקללת כוללת
        return actualTime * (1.0 + fearFactor * segmentRisk) + shelterPenalty;
    }

    /**
     * מחשב את קנס המרחק למקלט על בסיס נקודת האמצע של המקטע.
     */
   private double calculateShelterPenalty(RoadSegment segment, RouteParams userParams) {
    Junction source = segment.getSourceJunction();
    Junction target = segment.getTargetJunction();

    // מקרה 1: גרף מופשט ללא קואורדינטות (בדיקות / Mock)
    if (!source.hasCoordinates() || !target.hasCoordinates()) {
        // אם אף אחד מהצמתים אינו מקלט בעצמו, ניתן לקנוס לפי חוסר כיסוי או להחזיר 0
        if (!source.isShelter() && !target.isShelter()) {
            return userParams.getFearFactor() * userParams.getMaxShelterMinutes();
        }
        return 0.0;
    }

    // מקרה 2: גרף גיאוגרפי עם קואורדינטות
    double midX = (source.getX() + target.getX()) / 2.0;
    double midY = (source.getY() + target.getY()) / 2.0;
    GeoPoint midpoint = new GeoPoint(midX, midY);

    Shelter nearestShelter = shelterRepository.findNearestShelter(midpoint);
    if (nearestShelter == null) {
        return userParams.getFearFactor() * userParams.getMaxShelterMinutes();
    }

    // חישוב מרחק במטרים למקלט הקרוב
    double distanceMeters = midpoint.distanceTo(nearestShelter.getLocation());

    // המרת מהירות המשתמש לקצב של מטרים לדקה: (km/h * 1000) / 60
    double metersPerMinute = (userParams.getWalkingSpeedKmh() * 1000.0) / 60.0;
    double timeToShelterMinutes = distanceMeters / metersPerMinute;

    // סף זמן בטוח בדקות (90 שניות = 1.5 דקות)
    double safeThresholdMinutes = MAX_SAFE_TIME_SECONDS / 60.0;

    // קנס על כל דקה מעבר לסף הבטיחות
    double excessMinutes = Math.max(0.0, timeToShelterMinutes - safeThresholdMinutes);

    return userParams.getFearFactor() * excessMinutes;
}
}