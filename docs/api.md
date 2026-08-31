# FireRoute API

כתובת בסיס מקומית: `http://localhost:8080`  
פורמט תשובות: JSON. חישוב מסלול הוא פעולת קריאה ולכן ה־API משתמש ב־`GET` עם query parameters.

אפשר לפתוח ולהפעיל את כל ה־endpoints גם דרך Swagger UI ב־`/swagger-ui/index.html`.

## זרימת שימוש רגילה

1. הלקוח מקבל GPS מהמכשיר.
2. הוא קורא ל־`/api/junctions/nearest` ומקבל `id` של צומת בגרף.
3. במסלול רגיל הוא שולח source ו־destination ל־`/api/routes`.
4. בחירום הוא שולח רק source ל־`/api/routes/emergency`; השרת בוחר מקלט.

## איתור צומת לפי GPS

```http
GET /api/junctions/nearest?lat=32.078&lon=34.774
```

| פרמטר | חובה | משמעות |
|---|---:|---|
| `lat` | כן | קו רוחב, בין ‎-90 ל־90 |
| `lon` | כן | קו אורך, בין ‎-180 ל־180 |

המערכת מסרבת להצמיד מיקום שנמצא יותר מ־250 מטר מהגרף, כדי לא להחזיר צומת תל־אביבי למשתמש שנמצא מחוץ לאזור השירות.

```json
{
  "id": "junction-id",
  "latitude": 32.078,
  "longitude": 34.774,
  "shelter": false
}
```

## מסלול רגיל

```http
GET /api/routes?sourceId=A&destinationId=B&walkingPace=AVERAGE&fearFactor=1&maxShelterMinutes=7
```

| פרמטר | חובה | ברירת מחדל | משמעות |
|---|---:|---|---|
| `sourceId` | כן | — | צומת התחלה |
| `destinationId` | כן | — | צומת יעד |
| `walkingPace` | לא | `AVERAGE` | קצב הליכה |
| `fearFactor` | לא | `1` | משקל הסיכון בחישוב, ערך שאינו שלילי |
| `maxShelterMinutes` | לא | `7` | מרחק הליכה מרבי ממקלט לאורך הנתיב |

```json
{
  "found": true,
  "routeType": "NORMAL",
  "failureReason": "NONE",
  "totalTravelTime": 4.2,
  "pathPoints": [
    { "id": "A", "latitude": 32.078, "longitude": 34.774 }
  ],
  "maxMinutesToShelter": 3.1,
  "shelterConstraintSatisfied": true,
  "destinationShelter": null,
  "alreadyAtShelter": false
}
```

## מסלול חירום

```http
GET /api/routes/emergency?sourceId=A
```

הפרמטרים האופציונליים זהים למסלול רגיל, אך אין `destinationId`. בחירום בחירת המקלט היא החלטת השרת, לא החלטת הלקוח.

```json
{
  "found": true,
  "routeType": "EMERGENCY",
  "failureReason": "NONE",
  "totalTravelTime": 2.4,
  "pathPoints": [],
  "maxMinutesToShelter": 2.4,
  "shelterConstraintSatisfied": true,
  "destinationShelter": {
    "id": "shelter-id",
    "address": "כתובת המקלט",
    "latitude": 32.08,
    "longitude": 34.77,
    "accessible": true
  },
  "alreadyAtShelter": false
}
```

אם המשתמש כבר נמצא בצומת מקלט, מוחזרים `found=true` ו־`alreadyAtShelter=true`. אם אין מקלט שניתן להגיע אליו דרך הגרף, מוחזרים HTTP 200, ‏`found=false` ו־`failureReason=NO_REACHABLE_SHELTER`. בקשה תקינה שחושבה אך לא מצאה מסלול היא תוצאה עסקית; מזהה צומת לא קיים הוא שגיאת HTTP 404.

סיבות הכשל הקיימות:

| ערך | משמעות |
|---|---|
| `NONE` | נמצא מסלול |
| `NO_ROUTE_EXISTS` | אין דרך המחברת בין צמתי המסלול הרגיל |
| `NO_REACHABLE_SHELTER` | אין מקלט נגיש מצומת ההתחלה |

## מקלטים

כל המקלטים:

```http
GET /api/v1/shelters
```

מקלט לפי מזהה:

```http
GET /api/v1/shelters/{id}
```

מזהה שאינו קיים מחזיר 404.

## מצב התרעה

```http
GET /api/alerts/status
```

```json
{
  "areaId": "tel-aviv-center",
  "status": "QUIET",
  "lastKnownStatus": "QUIET",
  "alertActive": false,
  "stale": false,
  "lastAttemptAt": "2026-08-29T09:00:00Z",
  "lastSuccessfulReadAt": "2026-08-29T09:00:00Z",
  "lastChangedAt": "2026-08-29T08:55:00Z"
}
```

- `status` הוא תוצאת הניסיון האחרון: `ACTIVE`, `QUIET` או `UNKNOWN`.
- `lastKnownStatus` הוא המצב התקין האחרון, ונשמר גם כשקריאה חדשה נכשלת.
- `stale=true` אומר שהמידע התקין האחרון ישן מדי ואין להתייחס אליו כמצב עדכני.
- `alertActive` נשמר לנוחות הלקוח, אבל אסור לפרש `UNKNOWN` כאילו הוא שקט ודאי.

## הדמיית התרעה

במצב ברירת המחדל `simulated`:

```http
POST /api/alerts/simulate?active=true
POST /api/alerts/simulate?active=false
```

השינוי נכנס דרך אותו מסלול ניטור שבו משתמש מקור אמיתי ויופיע ב־status לאחר מחזור polling. כאשר `fireroute.alerts.source=oref`, endpoint זה אינו נוצר.

## שגיאות

שגיאות HTTP מוחזרות כ־`application/problem+json`. הקודים הפעילים הם:

- `400` — פרמטר חסר, malformed או Validation שנכשל.
- `404` — צומת או מקלט שאינו קיים.
- `422` — GPS חוקי שנמצא מחוץ לכיסוי.
- `500` — תקלה בלתי צפויה, בלי חשיפת stack trace ללקוח.

דוגמאות מלאות נמצאות ב־[api-errors.md](api-errors.md).
