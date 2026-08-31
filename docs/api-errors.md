# FireRoute — שגיאות API

כל שגיאת API מוחזרת בפורמט `ProblemDetail`. כך הלקוח מקבל מבנה קבוע ולא צריך לנחש לפי טקסט חופשי.

## קודי השגיאה הפעילים

| קוד | משמעות | דוגמה |
|---|---|---|
| `400 Bad Request` | הבקשה עצמה לא תקינה | שדה חסר, מספר שלילי או `lat=abc` |
| `404 Not Found` | המשאב המבוקש לא קיים | מזהה צומת או מקלט לא מוכר |
| `422 Unprocessable Entity` | הערכים חוקיים, אבל אינם מתאימים לאזור השירות | GPS שנמצא רחוק ממפת תל אביב |
| `500 Internal Server Error` | תקלה לא צפויה בשרת | שגיאת תכנות או מצב פנימי לא תקין |

`409 Conflict` יתווסף כאשר BE-18 תאכוף מעבר למסלול חירום בזמן התרעה. `503 Service Unavailable` יתווסף כאשר מודל טריות ההתרעות יוכל לקבוע שהשירות החיצוני אינו זמין. לא נוצרו חריגות שאף תרחיש שימוש עדיין אינו זורק.

## דוגמאות

Validation מחזיר פירוט לפי שדה:

```json
{
  "status": 400,
  "title": "Validation Failed",
  "detail": "One or more request fields are invalid",
  "errors": {
    "sourceId": "sourceId must not be blank"
  }
}
```

משאב חסר:

```json
{
  "status": 404,
  "title": "Junction Not Found",
  "detail": "Junction not found: does-not-exist"
}
```

השרת לעולם לא מחזיר stack trace ללקוח. פרטי שגיאת `500` נכתבים ללוג של השרת בלבד.
