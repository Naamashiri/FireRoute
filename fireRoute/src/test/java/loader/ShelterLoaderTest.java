package loader;

import org.junit.jupiter.api.Test;
import geom.GeoPoint;
import shelters.Shelter;
import shelters.ShelterRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShelterLoaderTest {

    @Test
    void testLoadSheltersAndFindNearest() throws Exception {
        // 1. טעינת המאגר מהקובץ
        ShelterLoader loader = new ShelterLoader();
        ShelterRepository repository = loader.loadFromResources("shelters.json");

        // 2. בדיקה שהמאגר טען מקלטים (בודקים שגודל הרשימה שונה מ-0)
        // אפשר גם לבדוק מול מספר המקלטים המדויק בקובץ אם ידוע
        int actualCount = repository.getAllShelters().size();
        assertEquals(true, actualCount > 0);

        // 3. בדיקת שליפת מקלט קרוב לנקודה מוגדרת
        GeoPoint dizengoff = new GeoPoint(34.7740, 32.0780);
        Shelter nearest = repository.findNearestShelter(dizengoff);

        // בדיקה שאכן חזר מקלט ולא null (על ידי השוואת שדה קיים)
        assertEquals(true, nearest != null);
        System.out.println("Total shelters loaded: " + actualCount);
        System.out.println("Nearest shelter address: " + nearest.getAddress());
    }

    @Test
    void testLoadNonExistingFileThrowsException() {
        // בדיקה שטעינת קובץ שלא קיים זורקת שגיאה כמצופה
        ShelterLoader loader = new ShelterLoader();
        assertThrows(IllegalArgumentException.class, () -> {
            loader.loadFromResources("non_existing_file.json");

        });
    }
}