# FireRoute — בנק שאלות ותשובות לראיון

המסמך נכתב לפי המימוש הנוכחי בפרויקט. התשובות מנוסחות בגוף ראשון כדי שאפשר יהיה לתרגל אותן בקול. אין צורך לשנן מילה במילה; יש להבין את ההיגיון ולשמור על מבנה תשובה ברור.

## תוכן עניינים

1. הצגת הפרויקט
2. זרימות end-to-end
3. ארכיטקטורה ושכבות
4. אלגוריתם וגרפים
5. Java Core מתוך המימוש
6. Spring Boot ו־Dependency Injection
7. API, Validation ושגיאות
8. Concurrency ומצב התרעות
9. Offline ו־Frontend
10. בדיקות, ביצועים ו־Production
11. Trade-offs
12. סיפורי STAR
13. שאלות ביקורת ומלכודות

---

# 1. הצגת הפרויקט

## שאלה: ספרי לי על FireRoute

**תשובה:**

FireRoute הוא פרויקט עצמאי שבניתי ב־Java 21 וב־Spring Boot. המערכת מחשבת מסלולי הליכה במרכז תל אביב תוך התחשבות בזמן ההליכה ובקרבה למקלטים. המפה מיוצגת כגרף מכוון: צמתים מייצגים נקודות מפגש וקטעי דרך הם קשתות שמשוקללות בזמן הליכה.

למסלול רגיל השתמשתי ב־Dijkstra עם פונקציית עלות שמשלבת זמן הליכה וחשיפה. בנוסף בניתי `ShelterMap` באמצעות Dijkstra רב־מקורי על הגרף ההפוך. היא מחשבת פעם אחת, בעליית השירות, מהו המקלט הקרוב לכל צומת, מה זמן ההליכה אליו ומה הצעד הבא בדרך. לכן מסלול חירום יכול להיבנות ללא הרצת חיפוש מלאה בכל בקשה.

חילקתי את המערכת לשכבות API, Application, Domain ו־Infrastructure. שמרתי את ה־Domain ללא תלות ב־Spring כדי שהלוגיקה תהיה קלה לבדיקה ולהחלפה. הוספתי גם ניטור התרעות, טיפול מפורש במצב `UNKNOWN`, בדיקות אוטומטיות ויכולת ניתוב מקומית בדפדפן כשהשרת אינו זמין.

## שאלה: מה הייתה המוטיבציה לפרויקט?

**תשובה:**

רציתי פרויקט Backend שאינו CRUD רגיל ושמחבר כמה נושאים: אלגוריתמים, API, ארכיטקטורה, אינטגרציה עם מקור חיצוני, concurrency, בדיקות וביצועים. תרחיש חירום הכריח אותי לחשוב גם על failure modes ועל ההבדל בין "אין סכנה" לבין "אין לי מידע".

זהו פרויקט portfolio ולא מערכת מבצעית מאושרת. אני מציגה אותו כהדגמה הנדסית של פתרון, לא כמוצר מציל חיים שכבר מתאים ל־production.

## שאלה: מה החלק שאת אישית בנית?

**תשובה:**

זהו פרויקט עצמאי, ולכן הייתי אחראית על הגדרת הדרישות, מודל הנתונים, האלגוריתם, שכבות ה־Backend, ה־API, מנגנון ההתראות, בדיקות, מדידות ביצועים וה־offline fallback. בראיון חשוב לי להפריד בין רכיבים שכתבתי לבין מקורות נתונים וספריות שבהם השתמשתי, כמו Spring Boot, Leaflet ונתוני המפה.

## שאלה: מה היה החלק הקשה ביותר?

**תשובה:**

החלק הקשה לא היה מימוש Dijkstra עצמו אלא הגדרת המשמעות של "מסלול בטוח" באופן עקבי. בשלב מסוים היו שתי מדידות שונות לקרבה למקלט: זמן הליכה בגרף ומרחק אווירי. הן גם נתנו תשובות שונות וגם יצרו עבודה חוזרת. איחדתי את שתיהן סביב `ShelterMap`, כך שפונקציית העלות, האילוץ והערך שמוחזר ללקוח משתמשים באותו מושג.

## שאלה: במה הפרויקט שונה מפתרון shortest path רגיל?

**תשובה:**

המסלול אינו ממזער רק זמן. פונקציית העלות משלבת זמן הליכה עם חשיפה, ויש גם אילוץ על המרחק המרבי ממקלט לאורך הדרך. בנוסף קיימת בעיה נפרדת של מציאת המקלט הקרוב לכל צומת, שנפתרת מראש באמצעות Dijkstra רב־מקורי על גרף הפוך. המערכת צריכה גם להחזיר metadata עסקי כמו האם האילוץ התקיים, ולא רק רשימת צמתים.

---

# 2. זרימות end-to-end

## שאלה: מה קורה מרגע שהמשתמשת בוחרת נקודה על המפה?

**תשובה:**

1. אירוע הלחיצה ב־Leaflet מספק latitude ו־longitude.
2. `selectPoint` בדפדפן שולחת `GET /api/junctions/nearest`.
3. `JunctionController` מבצע validation לטווחי הקואורדינטות.
4. `JunctionLocator` סורק את צמתי הגרף, מחשב קירוב מרחק עירוני ובוחר את הקרוב ביותר.
5. אם המרחק מהגרף גדול מ־250 מטר, נזרקת `LocationOutsideCoverageException` ומוחזר 422.
6. אחרת מוחזרים מזהה הצומת והקואורדינטות שלו.
7. הדפדפן שומר את הצומת ב־state ומציג marker.

הפרדה זו נחוצה כי הטלפון מכיר GPS, בעוד שהאלגוריתם עובד עם מזהי צמתים בגרף.

## שאלה: מה קורה מרגע שהלקוחה לוחצת “חשבי מסלול” ועד שמוצג מסלול?

**תשובה מפורטת:**

```text
לחיצה בדפדפן
  ↓
normal() ב-app.js
  ↓
GET /api/routes עם sourceId, destinationId ואפשרויות
  ↓
Spring DispatcherServlet
  ↓
binding ל-RouteRequest + Bean Validation
  ↓
RoutingController
  ↓
RouteMapper: API DTO -> RouteParams
  ↓
RoutingService: בדיקות ואיתור הצמתים ב-Graph
  ↓
PathFinder.findPath
  ↓
Dijkstra strict pass
  ↓ אם אין מסלול
Dijkstra relaxed pass
  ↓
PathResult
  ↓
RouteMapper: Domain -> RouteResponse
  ↓
Jackson serializes JSON
  ↓
app.js show()
  ↓
Leaflet polyline + נתוני זמן וחשיפה
```

פירוט:

1. `normal()` מוודאת שקיימות נקודת התחלה ונקודת יעד ומציגה מצב loading.
2. היא בונה query string עם `sourceId`,‏ `destinationId`, קצב הליכה, `fearFactor` ו־`maxShelterMinutes`.
3. הבקשה מגיעה ל־Spring MVC. Spring מבצע binding ישירות ל־record בשם `RouteRequest`.
4. `@Valid` מפעיל validation: המזהים אינם ריקים והמספרים אינם שליליים.
5. `RouteMapper` ממיר את ה־DTO ל־`RouteParams`. ערך `null` פירושו שהפרמטר הושמט, ולכן נבחרת ברירת המחדל מה־Domain. ערך `0` נשמר כבחירה אמיתית.
6. `RoutingService` מאתר את שני הצמתים ב־`Graph`. מזהה חסר הופך ל־404.
7. `DijkstraPathFinder` מבצע קודם חיפוש strict שמסנן צמתי ביניים הרחוקים מדי ממקלט.
8. בכל relaxation, `RouteCostCalculator` מחשב זמן הליכה ועוד penalty של חשיפה. המרחק למקלט נשלף מ־`ShelterMap` בזמן `O(1)`.
9. אם לא נמצא מסלול strict, מתבצע חיפוש relaxed ללא סינון קשיח.
10. לאחר מציאת היעד, האלגוריתם משחזר את הנתיב באמצעות `previous` ו־`previousSegment`, ומחשב זמן כולל וחשיפה מרבית.
11. `RouteMapper` ממיר את `PathResult` ל־`RouteResponse` בלי לחשוף את `totalCost`, שהוא פרט פנימי של האלגוריתם.
12. Jackson הופך את ה־record ל־JSON.
13. `show()` בדפדפן ממירה כל נקודה ל־latitude/longitude, מציירת `Polyline`, מתאימה את גבולות המפה ומציגה זמן, מרחק וחשיפה.

## שאלה: מה קורה בלחיצה על “מסלול חירום”?

**תשובה:**

הדפדפן שולח `GET /api/routes/emergency` עם `sourceId` ואפשרויות, ללא destination. זו בחירה מכוונת: בחירום השרת בוחר את המקלט ולא המשתמש.

`RoutingService` קורא ל־`findPathToNearestShelter`. במקום להריץ Dijkstra חדש, `DijkstraPathFinder` שואל את `ShelterMap` מה המקלט הקרוב ועוקב אחרי `nextStepToShelter` עד אליו. בזמן השחזור הוא צובר זמן ועלות. ה־Mapper מוצא את אובייקט המקלט הפיזי ומוסיף כתובת ונגישות לתשובה.

## שאלה: מה קורה כשמתקבלת התרעה?

**תשובה:**

בשרת, `AlertPoller` מפעיל לפי schedule את `AlertMonitoringService`. השירות קורא ל־`AlertSource`, שיכול להיות simulated או Oref לפי configuration. התוצאה נרשמת כ־`AlertSnapshot` חדש.

בדפדפן, `poll()` קוראת ל־`/api/alerts/status` פעם בשנייה. אם יש מעבר ממצב לא פעיל ל־`alertActive=true` וכבר קיימת נקודת התחלה, היא קוראת אוטומטית ל־`emergency(true)`, מחליפה את המסלול למסלול חירום ומעדכנת את ה־UI.

חשוב: ה־Backend אינו דוחף את ההתראה לדפדפן. הדפדפן מבצע polling. חלופה עתידית היא SSE או WebSocket.

## שאלה: מה קורה אם השרת אינו זמין?

**תשובה:**

הפונקציה `api()` בדפדפן מנסה `fetch`. אם בקשת GET נכשלת, היא מעבירה את אותה כתובת ל־`OfflineRouter`. הנתונים `map.json` ו־`shelters.json` נשמרו מראש באמצעות Service Worker לאחר ביקור מקוון.

ה־OfflineRouter מממש איתור צומת וניתוב מקומי ב־JavaScript. לכן אפשר לחשב מסלול גם ללא שרת. לעומת זאת, בלי תקשורת אי אפשר לדעת על התרעה חדשה, ולכן מצב ההתראה הופך ללא ודאי והמשתמשת צריכה להפעיל ניתוב חירום ידנית.

## שאלה: מה קורה בזמן עליית האפליקציה?

**תשובה:**

1. Spring Boot יוצר `ApplicationContext`.
2. `AppConfig` טוענת `Graph` ו־`ShelterRepository` מקובצי JSON.
3. נוצר `AlertState` התחלתי במצב `UNKNOWN`.
4. נבנית `ShelterMap` ורצה `compute()` פעם אחת.
5. נוצר `RouteCostCalculator` שתלוי באותה `ShelterMap`.
6. נוצר `DijkstraPathFinder` ונחשף כ־`PathFinder`.
7. Spring יוצר Controllers, Mappers ו־Services ומזריק dependencies.
8. `AlertConfig` בוחר בדיוק `AlertSource` אחד לפי property.
9. לאחר העלייה, scheduler מתחיל polling.

אם טעינת המפה נכשלת, האפליקציה נכשלת בעלייה. זו החלטת fail-fast: אין ערך לשרת ניתוב שעולה בלי גרף תקין.

---

# 3. ארכיטקטורה ושכבות

## שאלה: מה האחריות של כל שכבה?

**תשובה:**

- **API:** HTTP, DTOs, validation, mapping ותרגום exceptions לתשובות HTTP.
- **Application:** orchestration של use cases, למשל איתור צמתים והפעלת מנוע הניתוב.
- **Domain:** גרף, מסלולים, פונקציית עלות, מקלטים ומודל מצב התרעה.
- **Infrastructure:** קובצי JSON, repository בזיכרון, HTTP ל־Oref, parser ו־scheduler.
- **Config:** composition root שמחבר בין abstractions למימושים באמצעות Spring.

## שאלה: למה ה־Domain אינו מכיר Spring?

**תשובה:**

רציתי שהלוגיקה המרכזית תהיה Java רגילה. כך אפשר ליצור `DijkstraPathFinder` בבדיקת יחידה ללא ApplicationContext, להחליף framework בלי לשכתב את האלגוריתם ולשמור את כיוון התלויות פנימה. המחיר הוא wiring מפורש יותר ב־configuration.

## שאלה: למה Controller לא קורא ישירות ל־Dijkstra?

**תשובה:**

ה־Controller צריך לטפל ב־HTTP בלבד. `RoutingService` אחראי ל־use case: בדיקת מזהים, איתור ישויות והפעלת `PathFinder`. כך אפשר לבדוק את ה־use case ללא HTTP ולשנות את מבנה ה־API בלי לגעת באלגוריתם.

## שאלה: למה יש DTOs נפרדים מה־Domain?

**תשובה:**

החוזה החיצוני והמודל הפנימי משתנים מסיבות שונות. DTO מאפשר validation ו־versioning של API בלי להוסיף annotations של HTTP לליבה. הוא גם מונע חשיפה של מידע פנימי: למשל `totalCost` אינו מוחזר ללקוח כי הוא פרט של פונקציית האופטימיזציה.

## שאלה: למה יש `RouteRequest` ו־`EmergencyRouteRequest` נפרדים?

**תשובה:**

למסלול חירום אין destination מבחירה. שימוש ב־request משותף עם destination nullable היה יוצר חוזה שמציג שדה שהשרת מתעלם ממנו. שני ה־records משתפים את האפשרויות באמצעות `RouteOptions`, אך מבטאים use cases שונים.

## שאלה: למה `PathFinder` הוא interface?

**תשובה:**

`RoutingService` צריך יכולת למציאת מסלול, לא תלות ב־Dijkstra. הדבר מאפשר להחליף ל־A*, להכניס fake בבדיקות ולשמור את בחירת האלגוריתם ב־composition root. אני גם מכירה במחיר: בפרויקט קטן עם מימוש אחד זו שכבת abstraction נוספת.

## שאלה: למה `ShelterRepository` מוגדר ב־Domain?

**תשובה:**

ה־Domain מגדיר את החוזה שהוא צריך. Infrastructure מספק את המימוש הנוכחי בזיכרון. אם נעבור ל־SQL, המודל והשירותים לא יצטרכו לדעת כיצד הנתונים נשמרים.

## שאלה: האם זו Clean Architecture?

**תשובה:**

אני מתארת אותה כארכיטקטורה שכבתית שמשתמשת בעקרונות ports and adapters. כיוון התלויות המרכזי פונה פנימה, אבל לא הייתי טוענת שהיא מימוש מלא של כל כללי Clean Architecture.

## שאלה: למה composition עדיפה כאן על ירושה?

**תשובה:**

`DijkstraPathFinder` מקבלת `Graph`,‏ `ShelterMap` ו־`RouteCostCalculator` כמשתפי פעולה במקום לרשת מהם. כל אחד מייצג אחריות שונה, ו־composition מאפשר החלפה ובדיקה בלי hierarchy מלאכותית.

---

# 4. אלגוריתם וגרפים

## שאלה: למה Dijkstra?

**תשובה:**

זהו גרף משוקלל בעל משקלים לא שליליים. BFS אינו מתאים כי קטעי הדרך אינם שווים בעלותם. Bellman–Ford תומך במשקלים שליליים אך יקר יותר ואינו נדרש. Dijkstra מספק פתרון נכון ופשוט יחסית בסיבוכיות `O((V+E) log V)` עם תור עדיפויות.

## שאלה: איך את יודעת שהמשקלים אינם שליליים?

**תשובה:**

זמן הליכה אינו שלילי, `fearFactor` מקבל validation כלא שלילי, ורכיב החשיפה מחושב באמצעות `max(0, ...)`. לכן כל עלות קשת אינה שלילית.

## שאלה: מה בדיוק נשמר בזמן החיפוש?

**תשובה:**

- `dist`: העלות הטובה ביותר הידועה לכל צומת.
- `previous`: הצומת הקודם בנתיב.
- `previousSegment`: הקשת שבה הגענו, לצורך חישוב זמן ועלות בשחזור.
- `settledNodes`: צמתים שהעלות המינימלית שלהם כבר נקבעה.
- `PriorityQueue<State>`: המועמד הבא בעל העלות הנמוכה ביותר.

כל המבנים נוצרים מחדש בכל request ואינם משותפים בין threads.

## שאלה: למה יש entries כפולים ב־PriorityQueue?

**תשובה:**

`PriorityQueue` של Java אינה מספקת decrease-key יעיל. כשמוצאים מחיר טוב יותר לצומת, מכניסים `State` חדש. כאשר entry ישן יוצא, `settledNodes` גורם לדלג עליו. זו טכניקה מקובלת שמפשטת את הקוד במחיר שימוש זמני נוסף בזיכרון.

## שאלה: למה אפשר לעצור כשמוציאים את היעד מהתור?

**תשובה:**

ב־Dijkstra עם משקלים לא שליליים, הצומת בעל העלות הנמוכה ביותר שיוצא ונסגר לא יוכל לקבל מסלול זול יותר בהמשך. לכן כאשר היעד נסגר אפשר לסיים.

## שאלה: למה `ShelterMap` עובדת על הגרף ההפוך?

**תשובה:**

המטרה היא לדעת כיצד להגיע מכל צומת אל מקלט. אם קשת מקורית היא `u -> v`, בהרצה שמתחילה מהמקלטים אני צריכה לנוע לאחור מ־`v` אל `u`. לכן אני הופכת את הקשתות ומריצה חיפוש אחד מכל המקלטים יחד.

## שאלה: למה חיפוש רב־מקורי נכון?

**תשובה:**

אני מכניסה את כל המקלטים לתור עם מרחק אפס. Dijkstra מפיץ את המרחק הקצר ביותר מכל אחד מהם. הצומת הראשון שמגיע לכל נקודה בעלות מינימלית מייצג את המקלט הקרוב ביותר לפי זמן הליכה בגרף.

## שאלה: מה נשמר ב־ShelterMap?

**תשובה:**

- זמן מינימלי למקלט הקרוב.
- זהות המקלט הקרוב.
- הצומת הבא בדרך אליו.

כך מרחק ומקלט מתקבלים ב־`O(1)`, ומסלול חירום משוחזר בזמן לינארי לאורך המסלול.

## שאלה: למה לא למצוא את המקלט הקרוב לפי מרחק אווירי?

**תשובה:**

מקלט יכול להיות קרוב בקו ישר אך לא נגיש במסלול הליכה בגלל מבנה הרחובות או כיוון הקשתות. זמן הליכה בגרף תואם למסלול שהמשתמשת באמת יכולה לבצע וגם משמש מקור אמת יחיד לאילוץ ולפונקציית העלות.

## שאלה: למה לא A*?

**תשובה:**

בגודל הנוכחי Dijkstra פשוט ומהיר מספיק. A* יכול לחסוך סריקה בגרף גדול, אבל ההיוריסטיקה חייבת לא להעריך ביתר את העלות המשולבת. מרחק גיאוגרפי חלקי מהירות יכול להיות lower bound לרכיב הזמן, אך צריך להוכיח שהוא נשאר admissible ביחס לכל פונקציית העלות. הייתי מחליפה רק אחרי benchmark.

## שאלה: למה שני חיפושי Dijkstra במסלול רגיל?

**תשובה:**

הראשון מספק constraint קשיח לצמתי ביניים. אם הוא נכשל, השני מוותר על הסינון כדי לא להחזיר למשתמשת שום מסלול. התשובה מסמנת אם האילוץ התקיים. המחיר הוא כמעט פי שניים עבודה במקרה הגרוע; היתרון הוא הפרדה בין ניסיון לעמוד במדיניות לבין עצם היכולת לעזור.

## שאלה: למה לא לסנן את נקודת המוצא?

**תשובה:**

המשתמשת לא בחרה היכן היא נמצאת. אם היא כבר רחוקה ממקלט, דחיית הבקשה אינה הופכת אותה לבטוחה יותר. הסינון הגיוני בצמתי ביניים שבהם האלגוריתם יכול לבחור דרך אחרת.

## שאלה: מה קורה כש־start שווה ל־goal?

**תשובה:**

מוחזר מסלול בן צומת אחד, זמן ועלות אפס, אבל עדיין מחושבת מידת החשיפה של הצומת. כך "לא זזתי" אינו מתפרש בטעות כ"אני ליד מקלט".

## שאלה: מה קורה אם אין מקלט נגיש?

**תשובה:**

`ShelterMap.getNearestShelter` מחזירה `null`, ו־PathFinder מחזיר `PathResult.noPath()`. ה־Mapper מתרגם זאת ל־`found=false` ול־`NO_REACHABLE_SHELTER` במסלול חירום.

## שאלה: איך פונקציית העלות עובדת?

**תשובה:**

```text
walkingMinutes + fearFactor × max(0, minutesToShelter(target) - 1.5)
```

שני הרכיבים נמדדים בדקות. `fearFactor` הוא שער חליפין: כמה דקות הליכה נוספות המשתמשת מוכנה לשלם כדי לחסוך דקת חשיפה. בתוך 1.5 דקות ממקלט אין penalty.

## שאלה: מה החיסרון בפונקציית העלות?

**תשובה:**

החשיפה נמדדת בצומת היעד של הקשת ולא באופן רציף לאורך כל הקטע. בקטעים קצרים זו approximation סבירה; בקשת ארוכה היא עלולה לפספס אזור חשוף באמצע. אפשר לדגום נקודות לאורך הקשת או לפצל קשתות, במחיר חישוב וזיכרון.

## שאלה: האם `fearFactor` מבוסס מחקרית?

**תשובה:**

לא. הוא פרמטר מוצר שמאפשר להדגים את ה־trade-off. לפני production צריך לכייל אותו עם דרישות משתמש ומומחי דומיין, ולא להציג אותו כמדד בטיחות מוכח.

---

# 5. Java Core מתוך המימוש

## שאלה: למה dependencies נשמרות בשדות `final`?

**תשובה:**

הן חובה לבניית האובייקט ואינן אמורות להשתנות. `final` מונע החלפת reference, מבהיר את invariant של המחלקה ומקל על reasoning במערכת מרובת threads. הוא אינו הופך אוטומטית את האובייקט שאליו מצביע השדה ל־immutable.

## שאלה: למה `State` היא `private static class`?

**תשובה:**

זו מחלקת עזר שהיא פרט מימוש של Dijkstra. היא אינה צריכה גישה ל־instance של המחלקה החיצונית, ולכן `static` מונע reference סמוי ל־`DijkstraPathFinder`. `private` מונע חשיפת API שאינו חלק מהחוזה.

## שאלה: למה חלק מה־DTOs הם records?

**תשובה:**

הם נשאי נתונים עם מבנה קבוע ו־value semantics. record מייצר constructor, accessors,‏ `equals`,‏ `hashCode` ו־`toString`, ומקטין boilerplate. הוא מתאים במיוחד ל־request/response ול־immutable snapshot.

## שאלה: האם record הוא immutable לחלוטין?

**תשובה:**

הרכיבים שלו `final`, ולכן אי אפשר להחליף את ההפניות לאחר הבנייה. אבל אם record מחזיק `List` mutable, אפשר עדיין לשנות את תוכן הרשימה. immutability עמוקה דורשת defensive copy או collections בלתי ניתנות לשינוי.

## שאלה: למה משתמשים ב־HashMap ב־Graph?

**תשובה:**

הפעולה הנפוצה היא איתור צומת לפי מזהה, ו־HashMap נותנת זמן ממוצע `O(1)`. החיסרון הוא שאין סדר מובטח ויש עלות זיכרון נוספת.

## שאלה: מה נדרש מ־`equals` ו־`hashCode` של Junction?

**תשובה:**

`Junction` משמשת כמפתח ב־HashMap וכערך ב־HashSet בתוך האלגוריתם. לכן אם השוויון הלוגי מבוסס על מזהה, `equals` ו־`hashCode` חייבים להשתמש באופן עקבי באותו מזהה. אסור לשנות שדה שמשתתף ב־hash לאחר הכנסה למפה או לקבוצה.

## שאלה: האם Java מעבירה את `Junction` by reference?

**תשובה:**

Java תמיד pass-by-value. כאשר מועברת `Junction`, מועתק ערך ההפניה. הקוד הקורא והמתודה מצביעים לאותו אובייקט, אבל המתודה אינה יכולה להחליף את המשתנה של הקוד הקורא.

## שאלה: איפה הנתונים נשמרים בזיכרון?

**תשובה:**

אובייקטי ה־Graph, הצמתים, הקשתות וה־beans נמצאים ב־heap. בכל request נוצרים stack frames פרטיים ל־thread. ה־maps וה־PriorityQueue של החיפוש הם אובייקטים ב־heap, אבל references אליהם מקומיים לקריאה ולכן אינם משותפים. metadata של מחלקות מנוהל ב־HotSpot המודרנית ב־Metaspace.

## שאלה: האם `Collections.unmodifiableMap` יוצרת עותק?

**תשובה:**

לא. היא יוצרת view שאי אפשר לשנות דרכו, אבל אם המחלקה המקורית ממשיכה לשנות את המפה, השינויים ייראו ב־view. אם צריך snapshot אמיתי משתמשים למשל ב־`Map.copyOf`.

---

# 6. Spring Boot ו־Dependency Injection

## שאלה: מהו bean בפרויקט?

**תשובה:**

Bean הוא אובייקט שה־ApplicationContext של Spring יוצר ומנהל. למשל `Graph`,‏ `ShelterMap`,‏ `RouteCostCalculator` ו־`PathFinder` נוצרים ממתודות `@Bean`; Controllers ו־Mappers מסוימים נמצאים באמצעות component scanning.

## שאלה: האם bean הוא Singleton?

**תשובה:**

ברירת המחדל היא singleton לכל `ApplicationContext`, לא בהכרח singleton מוחלט בכל JVM. המחלקה עצמה אינה אוכפת Singleton pattern ואפשר ליצור ממנה אובייקט נוסף מחוץ ל־Spring.

## שאלה: למה constructor injection?

**תשובה:**

התלויות גלויות בחתימת ה־constructor, אפשר לסמן אותן `final`, אי אפשר ליצור אובייקט חלקי, ובדיקות יכולות להעביר dependencies ללא Spring. Field injection היה קצר יותר אבל מסתיר dependencies ומקשה על בדיקות.

## שאלה: כיצד Spring יודע את סדר יצירת ה־beans?

**תשובה:**

פרמטרים של מתודת `@Bean` מגדירים dependencies. למשל `shelterMap(Graph graph)` מחייב `Graph`, ו־`pathFinder(Graph, ShelterMap, RouteCostCalculator)` מחייב את שלושתם. Spring פותר את הגרף לפי טיפוסים ויוצר אותם בסדר המתאים.

## שאלה: איך נבחר מקור ההתראות?

**תשובה:**

`@ConditionalOnProperty` יוצר בדיוק מקור אחד:

- `simulated` הוא ברירת המחדל.
- `oref` יוצר client, parser ו־OrefAlertSource.

`AlertMonitoringService` תלוי ב־`AlertSource` ואינו יודע איזה מימוש נבחר.

## שאלה: למה simulated הוא ברירת המחדל?

**תשובה:**

כדי שפיתוח, בדיקות ודמו יהיו דטרמיניסטיים ולא תלויים ב־endpoint לא רשמי או בזמינות רשת. מקור אמיתי מופעל רק במפורש.

## שאלה: האם Controllers thread-safe?

**תשובה:**

Spring משתמש כברירת מחדל ב־singleton Controllers, ולכן אסור לשמור בהם state משתנה של request. ה־Controllers בפרויקט stateless: כל המידע מגיע כפרמטר ונשמר במשתנים מקומיים.

---

# 7. API, Validation ושגיאות

## שאלה: למה המסלולים הם GET?

**תשובה:**

חישוב מסלול הוא read-only ואינו משנה state בשרת. GET מאפשר URL ניתן לשיתוף ופשוט לבדיקה. אם הבקשה תכלול רשימת waypoints, polygons או העדפות מורכבות, query string יהפוך למסורבל ואז POST עם JSON יהיה מוצדק.

## שאלה: למה מספרים ב־DTO הם `Double` ולא `double`?

**תשובה:**

`null` מסמן שהפרמטר הושמט, בעוד `0` הוא ערך חוקי ובעל משמעות. עם primitive לא ניתן להבחין ביניהם. ה־Mapper ממלא ברירת מחדל רק עבור `null`.

## שאלה: מה ההבדל בין 400, 404 ו־422 אצלך?

**תשובה:**

- 400: request malformed או validation שנכשל.
- 404: מזהה צומת או מקלט שאינו קיים.
- 422: הקואורדינטות חוקיות תחבירית אבל מחוץ לאזור הכיסוי.

## שאלה: למה `found=false` הוא HTTP 200?

**תשובה:**

הבקשה הייתה תקינה והחישוב בוצע בהצלחה, אך אין דרך בגרף. זו תוצאה עסקית ולא תקלה בפרוטוקול או resource חסר. `failureReason` מאפשר ללקוח להבין אם אין מסלול רגיל או אין מקלט נגיש.

## שאלה: למה `ProblemDetail`?

**תשובה:**

זהו פורמט סטנדרטי לשגיאות HTTP שמפריד בין status, title, detail ושדות הרחבה. הוא נותן חוזה עקבי ולא חושף stack trace.

## שאלה: למה ה־handler יורש מ־`ResponseEntityExceptionHandler`?

**תשובה:**

כדי להשאיר ל־Spring handlers ספציפיים ל־404,‏ 405 ושגיאות MVC. Catch-all של `Exception` לבדו עלול לתפוס שגיאות framework ולהחזיר עליהן בטעות 500.

## שאלה: האם הודעת `IllegalArgumentException` בטוחה תמיד להחזרה ללקוח?

**תשובה:**

לא בהכרח. בפרויקט היא משמשת בעיקר ל־validation פנימי ידוע, אבל ב־production הייתי מוודאת שלא מוחזר מידע פנימי וממפה שגיאות צפויות לסוגים ייעודיים במקום להסתמך על exception כללי.

## שאלה: למה לא להחזיר את `totalCost`?

**תשובה:**

זהו ערך פנימי שמשמעותו תלויה בנוסחת העלות וב־fear factor. הלקוח צריך זמן הליכה וחשיפה, שהם מושגים יציבים. חשיפת total cost הייתה מקבעת פרט מימוש בחוזה הציבורי.

---

# 8. Concurrency ומצב התרעות

## שאלה: כיצד `AlertState` thread-safe?

**תשובה:**

המצב נשמר ב־immutable `AlertSnapshot`. ההפניה אליו היא `volatile`, ולכן readers רואים את ה־snapshot האחרון שפורסם. המתודה `record` היא `synchronized`, משום שהעדכון תלוי ב־snapshot הקודם וכולל read-modify-write של כמה ערכים לוגיים. כך writers אינם מאבדים עדכונים וקוראים אינם צריכים lock.

## שאלה: למה `volatile` לבדו לא מספיק ל־record()?

**תשובה:**

`volatile` מבטיח visibility וסדר סביב קריאה וכתיבה של ההפניה, אבל לא הופך רצף של קריאה, חישוב וכתיבה לפעולה אטומית. שני writers יכולים לקרוא אותו snapshot קודם ולדרוס זה את התוצאה של זה. `synchronized` מגן על הרצף כולו.

## שאלה: למה לא `AtomicReference.updateAndGet`?

**תשובה:**

זו חלופה תקינה שמבצעת CAS loop. בחרתי ב־`volatile` עם writer מסונכרן כי המימוש ברור, הכתיבה מתרחשת פעם במחזור polling ואין contention משמעותי. אם היו writers רבים, הייתי מודדת ושוקלת את החלופה.

## שאלה: למה snapshot ולא כמה שדות volatile?

**תשובה:**

כמה שדות יכולים להיקרא ברגעים שונים וליצור שילוב שמעולם לא היה מצב אמיתי, למשל status חדש עם timestamp ישן. reference יחיד ל־immutable snapshot מספק consistency בין כל השדות.

## שאלה: מה ההבדל בין `status` ל־`lastKnownStatus`?

**תשובה:**

`status` מתאר את ניסיון הקריאה האחרון ויכול להיות `UNKNOWN`. `lastKnownStatus` שומר את התוצאה התקינה האחרונה. כך כשל אינו נמחק או מתפרש כשקט, אבל עדיין אפשר להציג למשתמש מה היה המצב הידוע האחרון יחד עם staleness.

## שאלה: האם `alertActive()` עלול להחזיר true כשה־status הוא UNKNOWN?

**תשובה:**

כן. הוא מבוסס על `lastKnownStatus`. לכן ה־API מחזיר גם `status` וגם `stale`, והלקוח חייב להציג שהמידע אינו ודאי. זה trade-off מכוון בין שמירת המצב הידוע האחרון לבין שקיפות על כשל המקור.

## שאלה: למה `Clock` מוזרק?

**תשובה:**

כדי שהלוגיקה לא תהיה תלויה ישירות בשעון המערכת. בבדיקות אפשר להעביר `Clock.fixed` ולקבל timestamps דטרמיניסטיים.

## שאלה: מה קורה אם `AlertSource.read()` זורקת exception?

**תשובה:**

`AlertMonitoringService` תופס את החריגה, כותב log ומקליט `UNKNOWN`. כך scheduler אינו נעצר והמצב האחרון אינו מוצג כשקט ודאי.

---

# 9. Offline ו־Frontend

## שאלה: מה בדיוק Service Worker שומר?

**תשובה:**

הוא שומר app shell, קובצי המפה והמקלטים, וב־runtime גם נכסי Leaflet ואריחי מפה שכבר נצפו. נתוני החירום עובדים בגישת cache-first.

## שאלה: האם Service Worker מחשב את המסלול?

**תשובה:**

לא. הוא מנהל caching ו־fetch. האלגוריתם המקומי נמצא ב־`OfflineRouter` בתוך `offline-routing.js`. `app.js` קוראת אליו כאשר `fetch` ל־Backend נכשל.

## שאלה: מה החיסרון של מימוש ניתוב גם ב־Java וגם ב־JavaScript?

**תשובה:**

קיימת סכנת drift: תיקון בצד אחד לא בהכרח מגיע לצד השני. יש בדיקות לנכסים האופלייניים, אבל הייתי מוסיפה contract tests שמריצים תרחישים זהים בשני המימושים. חלופה היא ליבה משותפת, למשל WebAssembly, אך היא מגדילה את מורכבות הבנייה.

## שאלה: מה קורה בביקור ראשון ללא אינטרנט?

**תשובה:**

ה־Service Worker ונתוני החירום עדיין לא נשמרו, ולכן אין הבטחה שהאפליקציה תפעל. היכולת האופליינית זמינה לאחר ביקור מקוון מוצלח. זו מגבלה שצריך להציג במפורש.

## שאלה: האם אריחי המפה נחוצים לניתוב?

**תשובה:**

לא. הם נחוצים להצגה חזותית בלבד. הניתוב משתמש ב־`map.json`. גם אם חלק מאריחי הרקע חסרים, אפשר לחשב מסלול על הנתונים המקומיים.

## שאלה: למה הדפדפן מבצע polling כל שנייה?

**תשובה:**

זהו מימוש פשוט שמתאים לדמו ומספק latency נמוך. המחיר הוא בקשות חוזרות גם כשאין שינוי. ב־production הייתי שוקלת SSE, שמתאים במיוחד לזרם חד־כיווני מהשרת ללקוח, או WebSocket אם נדרשת תקשורת דו־כיוונית.

---

# 10. בדיקות, ביצועים ו־Production

## שאלה: אילו סוגי בדיקות קיימים?

**תשובה:**

- Unit tests לאלגוריתם, פונקציית העלות, פרמטרים ומודל התרעות.
- Tests לשירותי application.
- Mapper tests.
- MockMvc ו־integration tests לזרימת API.
- Fixtures ו־HTTP מדומה ל־Oref, ללא תלות באינטרנט.
- בדיקות עקביות לנתוני production.
- בדיקות לנכסי offline.
- תסריטי k6 לעומס מדורג ולקפיצה בזמן התרעה.

במצב שנבדק לאחרונה, `mvn test` הריץ 114 בדיקות בהצלחה.

## שאלה: האם 114 בדיקות מוכיחות שהמערכת נכונה?

**תשובה:**

לא. מספר בדיקות אינו מדד מספיק. הוא מראה שקיימת השקעה בתרחישים, אך עדיין נדרשים בדיקות דפדפן end-to-end, אבטחה, chaos, תאימות בין offline לשרת, ניטור production ואימות דומיין.

## שאלה: כיצד בדקת את מקור Oref בלי להיות תלויה ברשת?

**תשובה:**

הפרדתי client, parser ו־source. בדיקות parser משתמשות ב־fixtures, ובדיקות client/source משתמשות ב־HTTP מדומה. כך הן דטרמיניסטיות ולא נכשלות בגלל רשת או התרעה אמיתית.

## שאלה: ספרי על שיפור הביצועים

**תשובה:**

במדידה על מקרה ארוך קיבלתי בערך 288ms. ניתוח הראה שפונקציית העלות סרקה את כל המקלטים בכל relaxation, בזמן שכבר הייתה `ShelterMap` עם מרחק מחושב מראש. בנוסף היו שתי הגדרות שונות לקרבה למקלט. איחדתי אותן, הסרתי את הסריקה החוזרת ומדדתי כ־8.1ms באותו case. השיפור היה תוצאה של תיקון design ונכונות, לא cache.

## שאלה: האם 8.1ms הוא p95?

**תשובה:**

לא אטען זאת אם המדידה לא חושבה כ־p95. זו מדידה מקומית של case מוגדר. עבור טענת capacity צריך הרצת k6 מתועדת עם warm-up, חומרה, גרסה, משך, throughput ו־percentiles.

## שאלה: למה לא הוספת cache?

**תשובה:**

Cache היה מאיץ חישוב כפול אך משאיר שתי הגדרות סותרות לאותו מושג. העדפתי להסיר את העבודה והכפילות. אם בעתיד cache יידרש, צריך key מלא הכולל מקור, יעד וכל הפרמטרים ומדיניות invalidation בעת שינוי נתונים.

## שאלה: כיצד היית מגדילה לכל הארץ?

**תשובה:**

ראשית הייתי מודדת. לאחר מכן שוקלת חלוקה לאזורים, spatial index לאיתור צומת, A* או hierarchical routing, snapshot versioned של הגרף, עדכון אטומי של נתונים, כמה instances מאחורי load balancer ו־observability. לא הייתי מניחה שאחסון כל המדינה ומעבר Dijkstra מלא לכל בקשה מתאים בלי benchmark.

## שאלה: כיצד תעבדי עם כמה instances?

**תשובה:**

הגרף ו־ShelterMap הם read-mostly ויכולים להיטען בכל instance. מצב ההתרעה כרגע מקומי; כדי למנוע polling כפול וחוסר אחידות אפשר ליצור ingestion service יחיד שמפרסם snapshots דרך Redis, Kafka או storage משותף. endpoint הסימולציה צריך להיות מושבת ב־production.

## שאלה: אילו metrics היית מוסיפה?

**תשובה:**

- latency לפי endpoint ו־p50/p95/p99.
- מספר צמתים settled בכל חיפוש.
- שיעור strict success מול relaxed fallback.
- polling failures ומשך staleness.
- זמני בניית ShelterMap.
- throughput, error rate ו־application failures.
- CPU, heap, GC pauses ו־thread pool saturation.

## שאלה: מה נדרש לפני production?

**תשובה:**

מקור התרעות רשמי עם SLA, אבטחה ו־rate limiting, ניטור והתראות, deployment redundant, תהליך עדכון נתונים, בדיקות E2E ו־load מתועדות, אימות דומיין של הספים והנוסחה, privacy review למיקום המשתמש ו־incident response.

---

# 11. Trade-offs מרכזיים

## Dijkstra מול A*

- **בחרתי:** Dijkstra.
- **הרווחתי:** פשטות, נכונות ומימוש שקל לבדוק.
- **שילמתי:** סריקה רחבה יותר.
- **מתי אשנה:** כשהגרף יגדל ומדידה תראה שהחיפוש הוא bottleneck.

## Precomputation מול חישוב לפי בקשה

- **בחרתי:** ShelterMap בעלייה.
- **הרווחתי:** `O(1)` למרחק ומסלול חירום מהיר.
- **שילמתי:** זמן עלייה, זיכרון וקושי בעדכונים דינמיים.

## In-memory מול Database

- **בחרתי:** נתונים בזיכרון מקובצי JSON.
- **הרווחתי:** latency נמוך ופשטות.
- **שילמתי:** restart לעדכונים ומגבלת scale.

## Constraint קשיח מול תשובת fallback

- **בחרתי:** strict ואז relaxed.
- **הרווחתי:** ניסיון לעמוד בהבטחה בלי להשאיר משתמשת בלי תשובה.
- **שילמתי:** חיפוש שני במקרה הגרוע ו־UX שצריך להסביר חריגה.

## `volatile` + `synchronized` מול AtomicReference

- **בחרתי:** readers ללא lock ו־writer מסונכרן.
- **הרווחתי:** מודל פשוט וברור לעומס כתיבה נמוך.
- **שילמתי:** monitor lock בכתיבה.
- **חלופה:** `AtomicReference.updateAndGet` עם CAS loop.

## Polling מול Push

- **בחרתי:** polling.
- **הרווחתי:** פשטות ותאימות למקור.
- **שילמתי:** latency ובקשות חוזרות.
- **חלופה:** SSE לעדכונים חד־כיווניים.

## Offline כפול מול ליבה משותפת

- **בחרתי:** Java בשרת ו־JavaScript בדפדפן.
- **הרווחתי:** offline ללא תלות בשרת.
- **שילמתי:** סכנת drift ותחזוקה כפולה.

---

# 12. סיפורי STAR

## STAR 1: ביצועים חשפו בעיית נכונות

**Situation:** לאחר מעבר מגרף דמה לגרף אמיתי, מדדתי זמן בקשה של כ־288ms במקרה ארוך.

**Task:** לזהות את מקור ההאטה לפני הוספת optimization אקראית.

**Action:** ניתחתי את מספר ה־edge relaxations וגיליתי שכל אחת סרקה את כל המקלטים. במקביל זיהיתי ששתי שכבות הגדירו "קרבה למקלט" בצורה שונה. הפכתי את `ShelterMap` למקור אמת משותף והזרקתי אותה גם ל־PathFinder וגם ל־RouteCostCalculator.

**Result:** הסריקה החוזרת נעלמה, שתי השכבות הפכו עקביות והמדידה באותו case ירדה לכ־8.1ms.

**Reflection:** לא כל בעיית ביצועים דורשת cache או מבנה נתונים חדש. לפעמים האיטיות היא סימפטום של כפילות ושל בעיית design.

## STAR 2: מערכת שסירבה לעזור למשתמשת חשופה

**Situation:** constraint קשיח פסל גם את נקודת המוצא, ולכן משתמשת שהתחילה מעט מעבר לסף קיבלה "אין מסלול".

**Task:** להחליט אם לדבוק באילוץ או לספק תשובה שימושית.

**Action:** הפרדתי בין צמתי ביניים שהאלגוריתם בוחר לבין מיקום המוצא שהמשתמשת לא בחרה. הוספתי חיפוש strict ולאחריו relaxed fallback, והחזרתי `shelterConstraintSatisfied`.

**Result:** המערכת מספקת מסלול כאשר קיים ומציגה בכנות אם הוא חורג מהאילוץ.

**Reflection:** "לא נמצא" ו־"נמצא אך אינו עומד במדיניות" הם מצבים שונים וחייבים להיות מיוצגים בחוזה.

## STAR 3: מניעת מצב קרוע בהתראות

**Situation:** מצב ההתרעה כלל status וכמה timestamps. עדכון שדות נפרדים היה מאפשר לקורא לראות שילוב לא עקבי.

**Task:** לאפשר polling writer וקריאות HTTP מקבילות בלי לחשוף מצב חלקי.

**Action:** יצרתי `AlertSnapshot` immutable. שמרתי reference מסוג `volatile` לקריאות ללא lock והגנתי על פעולת read-modify-write באמצעות `synchronized`.

**Result:** כל reader רואה snapshot שלם והעדכונים אינם הולכים לאיבוד.

**Reflection:** החלפה של value object שלם פשוטה יותר מסנכרון של כמה שדות mutable.

## STAR 4: תכנון לכשל של מקור חיצוני

**Situation:** מקור Oref אינו רשמי ועלול להחזיר שגיאה, גוף לא צפוי או להיות לא זמין.

**Task:** למנוע מצב שבו כשל מוצג כשקט.

**Action:** הפרדתי client, parser ו־adapter; הוספתי `UNKNOWN`,‏ `lastKnownStatus`, timestamps ו־staleness; ובחרתי simulated כברירת מחדל.

**Result:** כשל חיצוני אינו מפיל את מנוע הניתוב ואינו מוצג כאילו אין התרעה.

**Reflection:** במערכת רגישה, unknown הוא מצב עסקי בפני עצמו ולא exception שצריך להסתיר.

## STAR 5: Resilience באמצעות Offline

**Situation:** בזמן עומס או חירום ייתכן שהשרת או הרשת אינם זמינים.

**Task:** לאפשר ניווט לאחר אובדן תקשורת.

**Action:** שמרתי app shell ונתוני חירום באמצעות Service Worker והוספתי OfflineRouter שמטפל בבקשות GET שנכשלו.

**Result:** לאחר ביקור מקוון ראשון ניתן לחשב מסלול מקומי ללא Backend.

**Reflection:** resilience יצרה מחיר תחזוקה משמעותי: שני מימושי אלגוריתם שצריך לשמור עקביים.

---

# 13. שאלות ביקורת ומלכודות

## שאלה: מה הדבר הראשון שהיית משנה בפרויקט?

**תשובה:**

הייתי מוסיפה contract tests מלאים בין Java ל־OfflineRouter, כי כפילות האלגוריתם היא הסיכון ההנדסי הבולט ביותר. לאחר מכן הייתי מוסיפה observability ומאחדת versioning של ה־API.

## שאלה: איפה עשית overengineering?

**תשובה:**

יש מקומות שבהם אפשר לטעון שהפרדת interfaces ו־layers רחבה יחסית לפרויקט קטן. עשיתי זאת כדי לתרגל design ולהקל על החלפה ובדיקות, אבל ביישום מסחרי קטן הייתי בוחנת כל abstraction לפי שינוי צפוי ולא מוסיפה אותו אוטומטית.

## שאלה: איזה חלק אינו production-ready?

**תשובה:**

מקור ההתראות, endpoint הסימולציה, תהליך עדכון הנתונים, אבטחה, ניטור, deployment ו־offline consistency. בנוסף הספים והנוסחה לא עברו אימות של מומחי דומיין.

## שאלה: האם השם “מסלול בטוח” מוצדק?

**תשובה:**

לא כהבטחת בטיחות מוחלטת. האלגוריתם ממזער metric שהגדרתי לקרבה למקלטים. נכון יותר לומר "מסלול שמעדיף קרבה למקלטים לפי המודל". ב־production הייתי עובדת עם אנשי מוצר ודומיין על ניסוח שאינו מטעה.

## שאלה: מה יקרה אם הנתונים משתנים בזמן בקשה?

**תשובה:**

במימוש הנוכחי הגרף ו־ShelterMap אינם מתעדכנים בזמן ריצה, ולכן כל בקשה רואה snapshot יציב. אם אוסיף עדכונים, אבנה graph snapshot חדש בצד ואחליף reference אטומית רק לאחר שהגרף ו־ShelterMap תואמים לאותה version.

## שאלה: האם nearest-junction בסריקה לינארית הוא bottleneck?

**תשובה:**

בכמה אלפי צמתים זו פעולה פשוטה שרצה פעם אחת לבחירת נקודה, ולא בתוך לולאת Dijkstra. לא הוספתי spatial index בלי מדידה. אם היקף הנתונים יגדל, k-d tree, R-tree או grid index יהיו מועמדים.

## שאלה: איך תטפלי בפרטיות מיקום?

**תשובה:**

כרגע המיקום נשלח כ־query parameters, שיכולים להופיע בלוגים ובהיסטוריה. לפני production הייתי מצמצמת logging, מגדירה retention, בוחנת POST אם נדרש לצמצום חשיפה ב־URLs, משתמשת ב־TLS, לא שומרת מיקומים ללא צורך ומבצעת privacy review.

## שאלה: אם היית מתחילה מחדש, מה היית עושה אחרת?

**תשובה:**

הייתי מגדירה מוקדם יותר מקור אמת יחיד למדידת חשיפה, חוזה מפורש בין online ל־offline ותוכנית מדידה. יחד עם זאת, גילוי הבעיות דרך פיתוח ומדידה הוא חלק משמעותי ממה שלמדתי מהפרויקט.

---

# תרגול מומלץ

לכל שאלה במסמך:

1. לענות בקול ללא קריאה במשך 60–90 שניות.
2. לצייר את הזרימה על דף.
3. להסביר החלטה אחת וחלופה אחת.
4. לציין מגבלה בלי להתנצל.
5. להיות מסוגלת להצביע על המחלקות הרלוונטיות בקוד.

הסדר המומלץ לתרגול:

1. הצגת הפרויקט.
2. זרימת מסלול רגיל.
3. Dijkstra ו־ShelterMap.
4. סיפור הביצועים.
5. ארכיטקטורה ושכבות.
6. concurrency של ההתראות.
7. ביקורת עצמית ו־production.

---

# 14. מפת המחלקות — מי אחראית על מה

## Bootstrap ו־Configuration

| מחלקה | אחריות | למה היא נפרדת |
|---|---|---|
| `FireRouteApplication` | נקודת הכניסה של Spring Boot והפעלת scheduling | שומרת את bootstrap מינימלי; ה־wiring אינו מעורבב עם `main` |
| `AppConfig` | טעינת graph ומקלטים, יצירת beans של ה־Domain וחיבור `PathFinder` | משמשת composition root ומונעת annotations של Spring בתוך ה־Domain |
| `AlertConfig` | בחירת מקור התראות, יצירת `Clock` ו־`AlertMonitoringService` | מפרידה configuration של אינטגרציה חיצונית מ־configuration של הניתוב |

## API Controllers

| מחלקה | אחריות | מה היא אינה עושה |
|---|---|---|
| `RoutingController` | מקבלת בקשות למסלול רגיל וחירום, מפעילה mapper ו־service | אינה מכירה את פרטי Dijkstra ואינה בונה JSON ידנית |
| `JunctionController` | מתרגמת GPS לבקשה לאיתור הצומת הקרוב | אינה מחשבת מרחק בעצמה |
| `SheltersController` | מחזירה רשימת מקלטים או מקלט לפי מזהה | אינה יודעת אם הנתונים מ־JSON או database |
| `AlertController` | מחזירה snapshot נוכחי של מצב ההתרעות | אינה מבצעת polling בעצמה |
| `AlertSimulationController` | משנה את המקור המדומה דרך endpoint לפיתוח ודמו | אינה כותבת ישירות ל־`AlertState`; העדכון עובר בזרימה האמיתית במחזור הבא |

## API DTOs

| מחלקה | אחריות |
|---|---|
| `RouteRequest` | חוזה query parameters למסלול רגיל, כולל source ו־destination |
| `EmergencyRouteRequest` | חוזה למסלול חירום ללא destination |
| `RouteOptions` | interface משותף לאפשרויות קצב, פחד ומגבלת מקלט |
| `RouteResponse` | התשובה המלאה שהלקוח מקבל על מסלול |
| `RoutePoint` | נקודה במסלול בפורמט API: id, latitude ו־longitude |
| `JunctionResponse` | הצומת הקרוב שהוחזר עבור GPS |
| `ShelterResponse` | פרטי מקלט להצגה ללקוח |
| `AlertStatusResponse` | סטטוס התרעה, last-known, staleness ו־timestamps |
| `WalkingPaceDto` | ערכי קצב הליכה בחוזה ה־API |
| `RouteType` | הבחנה בין מסלול `NORMAL` ל־`EMERGENCY` |
| `RouteFailureReason` | סיבת כשל עסקית כאשר החישוב תקין אך לא נמצא מסלול |

## API Mappers וטיפול בשגיאות

| מחלקה | אחריות | החלטה חשובה |
|---|---|---|
| `RouteMapper` | ממירה request ל־`RouteParams` ו־`PathResult` ל־`RouteResponse` | ממלאת defaults במקום אחד ואינה חושפת `totalCost` |
| `ShelterMapper` | ממירה `Shelter` ל־DTO חיצוני | מונעת זליגת Domain object לחוזה HTTP |
| `AlertStatusMapper` | בונה response מ־`AlertState` ומחשב staleness | מרכז את משמעות ה־API של מצב התרעה |
| `ApiExceptionHandler` | ממפה exceptions ל־ProblemDetail ולקודי HTTP | שומר stack traces ב־log ולא מחזיר אותם ללקוח |

## Application Layer

| מחלקה | אחריות | משתפי פעולה |
|---|---|---|
| `RoutingService` | use case של מסלול רגיל וחירום: validation עסקי, איתור צמתים והפעלת `PathFinder` | `Graph`,‏ `PathFinder` |
| `JunctionLocator` | snap של GPS לצומת הקרוב והגבלת אזור הכיסוי | `Graph` |
| `ShelterService` | use cases של קריאת מקלטים וטיפול במזהה חסר | `ShelterRepository` |
| `AlertMonitoringService` | מבצע ניסיון קריאה אחד, מתרגם כשל ל־`UNKNOWN` ומעדכן state | `AlertSource`,‏ `AlertState`,‏ `Clock` |
| `AlertSource` | port שמגדיר כיצד application מקבלת סטטוס התרעה | ממומש על ידי simulated או Oref |
| `JunctionNotFoundException` | שגיאת application עבור מזהה צומת חסר | ממופה ל־404 |
| `ShelterNotFoundException` | שגיאת application עבור מזהה מקלט חסר | ממופה ל־404 |
| `LocationOutsideCoverageException` | GPS תקין שנמצא רחוק מדי מהגרף | ממופה ל־422 וכולל מרחק וסף |

## Routing Domain

| מחלקה | אחריות | פרטי מימוש חשובים |
|---|---|---|
| `PathFinder` | חוזה למציאת מסלול רגיל ומסלול למקלט | מבודד את application מהאלגוריתם |
| `DijkstraPathFinder` | מימוש Dijkstra, strict/relaxed search ושחזור מסלול | כל מצב החיפוש מקומי לקריאה |
| `RouteCostCalculator` | חישוב מחיר קשת מזמן הליכה ומחשיפה | משתמש באותה `ShelterMap` כמו האילוץ |
| `ShelterMap` | חישוב מראש של מרחק, מקלט וצעד הבא לכל צומת | Dijkstra רב־מקורי על גרף הפוך |
| `RouteParams` | value object immutable של פרמטרי בקשה | מחשב pace multiplier ושומר invariants |
| `WalkingPace` | enum שממפה קצב לשעת הליכה בקמ״ש | מונע strings שרירותיים בליבה |
| `PathResult` | תוצאת Domain: נתיב, זמן, עלות, חשיפה והאם האילוץ התקיים | מבצע defensive copy לרשימת הנתיב |

## Graph Domain

| מחלקה | אחריות | פרטי מימוש חשובים |
|---|---|---|
| `Graph` | אינדקס צמתים ובניית קשתות מכוונות | `HashMap` לפי id ורשימות adjacency בתוך `Junction` |
| `Junction` | צומת, קואורדינטות, סימון מקלט וקשתות נכנסות/יוצאות | שדות הזהות final, אך רשימות הקשתות mutable בזמן build |
| `RoadSegment` | קשת מכוונת וזמן הליכה | immutable לאחר construction |
| `GeoPoint` | value object של קואורדינטות וחישובי מרחק | record עם value semantics |

## Shelter Domain ו־Infrastructure

| מחלקה | אחריות |
|---|---|
| `Shelter` | ישות מקלט immutable: id, כתובת, מיקום ונגישות |
| `ShelterRepository` | חוזה לקריאה, איתור וחיפוש מקלטים |
| `InMemoryShelterRepository` | מימוש read-only שמאנדקס מקלטים לפי id |
| `ShelterLoader` | טעינת GeoJSON של מקלטים והמרתו ל־repository |
| `JsonDataLoader` | טעינת map JSON ובניית `Graph`,‏ `Junction` ו־`RoadSegment` |

## Alert Domain ו־Infrastructure

| מחלקה | אחריות | היבט concurrency |
|---|---|---|
| `AlertStatus` | enum של `ACTIVE`,‏ `QUIET`,‏ `UNKNOWN` | immutable |
| `AlertSnapshot` | תמונה עקבית ובלתי משתנה של כל מצב ההתראה | publication בטוח דרך reference מסוג `volatile` |
| `AlertState` | מחזיקה את ה־snapshot האחרון ומיישמת מדיניות last-known | writer מסונכרן ו־readers ללא lock |
| `AlertPoller` | מפעילה בדיקה מחזורית באמצעות `@Scheduled` | רצה ב־scheduler thread ולא ב־HTTP request thread |
| `SimulatedAlertSource` | מקור ידני לדמו ולבדיקות | `volatile AlertStatus` בין request writer ל־scheduler reader |
| `OrefAlertClient` | מבצע בקשת HTTP למקור ההתראות | אינו מחזיק state עסקי משתנה בין קריאות |
| `OrefAlertParser` | מפרש payload ומסנן אזורים נתמכים | רצוי stateless; קל לבדיקה עם fixtures |
| `OrefAlertSource` | adapter שמחבר client ו־parser לחוזה `AlertSource` | ממיר כשלים ל־`UNKNOWN` ומשחזר interrupt flag |

## Frontend — רכיבים שאינם מחלקות Java

| רכיב | אחריות |
|---|---|
| `app.js` | state של UI, בחירת נקודות, קריאות API, ציור מסלול ו־polling להתראות |
| `offline-routing.js` | מימוש מקומי של heap, Dijkstra, snap ומסלול חירום |
| `service-worker.js` | caching של app shell ונתוני חירום וניהול גרסאות cache |
| `index.html` | מבנה ה־UI |
| `app.css` | עיצוב ומצבי רגיל/חירום |

---

# 15. Multithreading, נכונות ובטיחות

## אילו threads קיימים במערכת?

ב־Backend יש לפחות שני מקורות עבודה מקביליים:

1. **HTTP request threads** של שרת ה־Servlet/Tomcat. כמה משתמשים יכולים לחשב מסלולים, לקרוא סטטוס או להפעיל simulation במקביל.
2. **Scheduler thread** שמפעיל את `AlertPoller` לפי `fixedDelay`.

בנוסף, `OrefAlertClient` מבצע I/O מתוך ה־scheduler thread. בצד הדפדפן JavaScript משתמשת בעיקר ב־event loop יחיד, אך פעולות `fetch` אסינכרוניות יכולות להסתיים בסדר שונה מסדר ההתחלה וליצור logical races.

## מהו ה־shared state?

| State | מי קורא | מי כותב | ההגנה |
|---|---|---|---|
| `Graph` ו־Junction adjacency | request threads | רק בזמן startup | safe-by-lifecycle: נבנה לפני publication ואינו אמור להשתנות לאחר מכן |
| `ShelterMap` | request threads | רק `compute()` בזמן startup | נבנית לפני הזרקה; לאחר מכן read-only לפי convention |
| `InMemoryShelterRepository` | request threads | constructor בלבד | unmodifiable map ו־immutable shelters |
| `AlertState.snapshot` | HTTP readers וה־scheduler | scheduler דרך `record` | `volatile` reference + `synchronized` writer |
| `SimulatedAlertSource.reading` | scheduler | HTTP simulation request | `volatile` enum reference |
| Dijkstra maps ו־queue | thread של אותה בקשה בלבד | אותו thread | thread confinement, אין צורך ב־lock |

## תרחיש מסוכן 1: Torn logical state

מימוש מסוכן היה נראה כך:

```java
volatile AlertStatus status;
volatile Instant lastSuccessfulReadAt;
volatile Instant lastChangedAt;
```

גם אם כל שדה `volatile`, reader יכול לקרוא `status` אחרי עדכון ואת timestamp לפני עדכון. כל קריאה בודדת בטוחה, אבל ה־snapshot הלוגי אינו עקבי.

**הפתרון במימוש:** כל השדות נארזים ב־immutable `AlertSnapshot`, ואז מתבצעת החלפה אחת של reference.

## תרחיש מסוכן 2: Lost update בין writers

נניח ששני writers מבצעים יחד:

```text
Writer A קורא snapshot S0
Writer B קורא snapshot S0
Writer A בונה S1 וכותב
Writer B בונה S2 וכותב על S1
```

אם S2 התבסס על S0, שינוי של A עלול ללכת לאיבוד.

**הפתרון במימוש:** `record` היא `synchronized`, ולכן רק writer אחד מבצע את כל רצף read-modify-write בכל רגע.

## תרחיש מסוכן 3: Visibility של snapshot חדש

ללא `volatile`, scheduler יכול לכתוב reference חדש אך request thread אחר עשוי להמשיך לראות reference ישן בגלל cache ואופטימיזציות מותרות של Java Memory Model.

**הפתרון במימוש:** כתיבה לשדה `volatile snapshot` happens-before קריאה מאוחרת של אותו volatile. reader מקבל את ה־snapshot שפורסם ואת מצב האובייקט שנבנה לפני הכתיבה.

## תרחיש מסוכן 4: Simulation שאינה נראית ל־scheduler

`POST /alerts/simulate` רץ ב־request thread וכותב ל־`SimulatedAlertSource.reading`; ה־scheduler קורא אותו מאוחר יותר. ללא synchronization, הוא עלול להמשיך לראות ערך ישן.

**הפתרון במימוש:** `reading` היא `volatile`. מדובר בערך יחיד שאינו תלוי בשדה נוסף, ולכן אין צורך ב־lock. הכתיבה האחרונה מנצחת.

## תרחיש מסוכן 5: כתיבה ישירה ל־AlertState בדמו

אם endpoint הסימולציה היה משנה את `AlertState` ישירות, מחזור polling הבא היה יכול לדרוס אותו מיד בתוצאה מהמקור. בנוסף הדמו לא היה בודק את הזרימה האמיתית.

**הפתרון במימוש:** ה־Controller משנה את `SimulatedAlertSource`. במחזור הבא ה־scheduler קורא אותו ומעביר את הערך דרך `AlertMonitoringService` ו־`AlertState`.

המשמעות היא שהתגובה ל־POST עדיין יכולה להציג את המצב הישן עד מחזור ה־polling הבא. זו eventual consistency מכוונת, לא בהכרח bug.

## תרחיש מסוכן 6: שתי בקשות Dijkstra במקביל

אם `dist`,‏ `previous` או `PriorityQueue` היו שדות singleton של `DijkstraPathFinder`, שתי בקשות היו משנות אותם במקביל ויוצרות נתיב משולב או corruption.

**הפתרון במימוש:** כל מבני החיפוש הם local variables בתוך `search`. כל request מקבלת graph search state נפרד. `PriorityQueue` אינה thread-safe, אבל היא thread-confined ולכן אין צורך שתהיה.

## תרחיש מסוכן 7: שינוי Graph בזמן חיפוש

`Graph` משתמשת ב־`HashMap` ו־`Junction` מחזיקה `ArrayList`; הם אינם thread-safe. אם thread אחד יוסיף קשת בזמן ש־Dijkstra סורקת, עלולים להתקבל `ConcurrentModificationException`, תוצאה לא עקבית או visibility לא מוגדרת.

**הפתרון הנוכחי:** lifecycle discipline — הגרף נבנה במלואו בזמן startup, Spring מפרסמת אותו לאחר סיום הבנייה, ואין קוד request שמשנה אותו.

**מגבלת המימוש:** `Junction.getOutGoingRoads()` מחזירה את הרשימה mutable עצמה. לכן ההגנה היא convention ולא immutability חזקה. hardening אפשרי:

- builder נפרד לבניית הגרף;
- freeze לאחר build;
- החזרת unmodifiable lists;
- graph snapshot immutable שמוחלף אטומית בעת עדכון.

זו נקודת ביקורת טובה להעלות בעצמך בראיון.

## תרחיש מסוכן 8: קריאה ל־ShelterMap בזמן compute

אם request תגיע בזמן ש־`compute()` עדיין משנה את המפות, היא עלולה לראות state חלקי.

**למה זה לא קורה בזרימה הנוכחית:** `compute()` רצה בתוך יצירת bean בזמן startup. `PathFinder` ו־Controllers נוצרים ומתחילים לשרת רק לאחר שה־bean הוחזר. `isComputed` גם מונע שימוש מוקדם באותה instance.

**אם יתווספו עדכונים runtime:** אין להריץ `compute()` על אותה instance חיה. צריך לבנות `ShelterMap` חדשה ולפרסם graph+shelter map תואמים יחד כ־snapshot אחד.

## תרחיש מסוכן 9: Polls חופפים

`fixedDelay` מתזמן את ההפעלה הבאה לאחר סיום הקודמת, ולכן אותה משימה אינה אמורה לחפוף לעצמה בתצורה הרגילה. בנוסף, ברירת המחדל של Spring scheduling לרוב פשוטה ומוגבלת. עם scheduler מותאם או הפעלות ידניות עדיין ייתכנו writers נוספים; `AlertState.record` נשאר מוגן.

אם קריאת Oref איטית, `fixedDelay` מגדיל את הזמן האמיתי בין ניסיונות במקום לצבור invocations. צריך גם timeout ב־HTTP client כדי שקריאה תקועה לא תעצור polling לזמן בלתי מוגבל.

## תרחיש מסוכן 10: InterruptedException

אם thread שמבצע קריאת Oref מופרע, בליעת `InterruptedException` הייתה מוחקת את בקשת הביטול.

**הפתרון במימוש:** `OrefAlertSource` קוראת `Thread.currentThread().interrupt()` לפני שהיא מחזירה `UNKNOWN`, וכך משחזרת את interrupt flag.

## תרחיש מסוכן 11: מרוץ אסינכרוני בדפדפן

למרות ש־JavaScript פועלת ב־event loop, שתי בקשות יכולות להיות in-flight:

```text
המשתמשת מבקשת מסלול רגיל
מתקבלת התרעה ונשלחת בקשת חירום
בקשת החירום חוזרת ראשונה ומוצגת
הבקשה הרגילה חוזרת אחריה ודורסת את התצוגה
```

המימוש הנוכחי אינו מבטל במפורש בקשה קודמת ואינו משתמש במספר גרסה. זהו logical race אפשרי ב־UI.

**שיפור מוצע:**

- `AbortController` לביטול בקשה קודמת;
- request generation counter ובדיקה לפני `show()`;
- עדיפות מוחלטת למסלול חירום כאשר `state.alert` פעיל.

זו דוגמה טובה לכך ש־race condition אינה דורשת בהכרח שני OS threads; מספיקות פעולות אסינכרוניות שמשנות אותו state.

## תרחיש מסוכן 12: כמה לחיצות simulation

שתי בקשות POST מקבילות יכולות לכתוב `ACTIVE` ו־`QUIET`. `volatile` מונעת visibility problem, אבל אינה מגדירה ordering עסקי מעבר ל־last write wins. אם צריך סדר מובטח, יש להוסיף version/timestamp או להעביר commands דרך queue.

## האם כל המערכת thread-safe?

**תשובה מקצועית:**

לא נכון לטעון שכל מחלקה thread-safe בפני עצמה. הבטיחות נובעת מכמה אסטרטגיות שונות:

1. **Immutability:** `AlertSnapshot`,‏ `RouteParams`,‏ `RoadSegment`,‏ `Shelter`.
2. **Thread confinement:** מצב החיפוש של Dijkstra מקומי ל־request.
3. **Safe publication וללא mutation לאחר startup:** `Graph` ו־`ShelterMap`.
4. **Visibility:** `volatile` עבור snapshot ו־simulated reading.
5. **Mutual exclusion:** `synchronized record` עבור read-modify-write.

יש גם סיכונים שנותרו: רשימות adjacency חשופות לשינוי ומרוצי async אפשריים ב־Frontend. הצגה כנה של הגבולות חזקה יותר מהצהרה כללית ש"הכול thread-safe".

## שאלות follow-up על concurrency

### למה `synchronized` וגם `volatile`?

`synchronized` מגן על writers בזמן העדכון המורכב. readers אינם נכנסים ל־monitor, ולכן הם צריכים `volatile` כדי לקבל visibility. אם גם `snapshot()` הייתה מסונכרנת על אותו monitor, volatile לא הייתה נדרשת, אך הקריאות היו ננעלות ללא צורך.

### האם assignment של reference אטומי?

כן, אבל atomic assignment לבדו אינו פותר visibility ואינו מגן על read-modify-write. לכן עדיין נדרשים כללי publication וסנכרון.

### למה immutable snapshot חשוב אם reference volatile?

אם האובייקט עצמו היה mutable, writer היה יכול לשנות את תוכנו לאחר publication וקוראים היו רואים עדכונים חלקיים. immutable snapshot אינו משתנה לאחר הפרסום.

### האם `final` מספק thread safety?

לא לבדו. הוא מונע החלפת reference ומספק semantics מועילים של safe initialization, אבל אובייקט mutable שמוחזק בשדה final עדיין יכול להשתנות באופן לא בטוח.

### האם ConcurrentHashMap נחוצה ל־dist?

לא. כל `dist` שייכת לחיפוש יחיד ול־thread יחיד. שימוש ב־ConcurrentHashMap רק היה מוסיף overhead ומסתיר את העובדה שה־state אינו אמור להיות משותף.

### מה יקרה אם נרצה parallel Dijkstra?

Dijkstra הרגילה סדרתית מטבעה סביב בחירת המינימום. קיימים אלגוריתמים מקביליים וקירובים, אבל בגרף הנוכחי עדיף להריץ בקשות שונות במקביל ולשמור כל חיפוש חד־threaded. שינוי כזה מוצדק רק לאחר profiling.
