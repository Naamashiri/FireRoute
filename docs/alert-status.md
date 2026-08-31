# FireRoute — מצב ההתראה

## למה boolean אינו מספיק

`alertActive=false` לבדו אינו אומר אם פיקוד העורף דיווח שהכול שקט או שהחיבור נכשל. לכן המערכת שומרת snapshot מלא ואינה הופכת תקלה ל־"אין התרעה".

## משמעות השדות

- `status` — תוצאת ניסיון הקריאה האחרון: `ACTIVE`, `QUIET` או `UNKNOWN`.
- `lastKnownStatus` — התוצאה התקינה האחרונה. בזמן `UNKNOWN` היא נשמרת ולא משתנה.
- `alertActive` — נגזר מ־`lastKnownStatus`; הוא true רק כאשר המצב התקין האחרון היה `ACTIVE`.
- `lastAttemptAt` — מתי ניסינו לקרוא לאחרונה, גם אם הניסיון נכשל.
- `lastSuccessfulReadAt` — מתי התקבלה לאחרונה תשובת `ACTIVE` או `QUIET`.
- `lastChangedAt` — מתי המצב התקין השתנה בפעם האחרונה.
- `stale` — true כאשר לא התקבלה תשובה תקינה במשך הזמן המוגדר.

הזמן מוגדר ב־`application.properties`:

```properties
fireroute.alerts.stale-after-seconds=10
```

## דוגמה בזמן תקלה

```json
{
  "areaId": "tel-aviv-center",
  "status": "UNKNOWN",
  "lastKnownStatus": "ACTIVE",
  "alertActive": true,
  "stale": true
}
```

המשמעות: הקריאה האחרונה נכשלה, אבל ההתרעה התקינה האחרונה הייתה פעילה. לכן המערכת ממשיכה להתייחס למצב כחירום ומציגה למשתמש שהמידע אינו עדכני.

## חלוקת האחריות

- `AlertSource` קורא מקור חיצוני ומחזיר סטטוס אחד.
- `AlertMonitoringService` מחליט כיצד לעדכן את המצב במקרה של הצלחה או כשל.
- `AlertState` שומר snapshot אטומי ו־thread-safe.
- `AlertPoller` אחראי רק להפעיל את השירות לפי לוח זמנים.
- `AlertStatusMapper` מחשב `stale` ומכין את תשובת ה־API.
