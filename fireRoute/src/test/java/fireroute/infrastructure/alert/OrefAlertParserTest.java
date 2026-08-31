package fireroute.infrastructure.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import fireroute.domain.alert.AlertStatus;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrefAlertParserTest {

    private static final String SUPPORTED_AREA = "תל אביב - מרכז העיר";

    private final OrefAlertParser parser =
            new OrefAlertParser(new ObjectMapper(), Set.of(SUPPORTED_AREA));

    @Test
    void constructorRejectsInvalidDependencies() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrefAlertParser(null, Set.of(SUPPORTED_AREA)));
        assertThrows(IllegalArgumentException.class,
                () -> new OrefAlertParser(new ObjectMapper(), Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrefAlertParser(new ObjectMapper(), Set.of(" ")));
    }

    @Test
    void emptyBodyMeansQuiet() {
        assertEquals(AlertStatus.QUIET, parser.parse(null));
        assertEquals(AlertStatus.QUIET, parser.parse("  "));
    }

    @Test
    void supportedAreaMeansActive() {
        String body = """
                {
                  "data": ["חיפה - כרמל", " תל אביב - מרכז העיר "],
                  "cat": "1"
                }
                """;

        assertEquals(AlertStatus.ACTIVE, parser.parse(body));
    }

    @Test
    void supportedAliasMeansActive() {
        OrefAlertParser parserWithAliases = new OrefAlertParser(
                new ObjectMapper(),
                Set.of(SUPPORTED_AREA, "תל אביב-יפו")
        );

        assertEquals(
                AlertStatus.ACTIVE,
                parserWithAliases.parse("{\"data\": [\"תל אביב-יפו\"]}")
        );
    }

    @Test
    void areaNameNormalizationHandlesWhitespaceUnicodeAndDashVariants() {
        OrefAlertParser parserWithIrregularConfiguration = new OrefAlertParser(
                new ObjectMapper(),
                Set.of("  תל  אביב – מרכז העיר  ")
        );

        assertEquals(
                AlertStatus.ACTIVE,
                parserWithIrregularConfiguration.parse(
                        "{\"data\": [\"תל\\u00A0אביב - מרכז העיר\"]}"
                )
        );
    }

    @Test
    void otherAreasMeanQuiet() {
        String body = """
                {"data": ["חיפה - כרמל", "ירושלים - מרכז"]}
                """;

        assertEquals(AlertStatus.QUIET, parser.parse(body));
    }

    @Test
    void emptyDataArrayMeansQuiet() {
        assertEquals(AlertStatus.QUIET, parser.parse("{\"data\": []}"));
    }

    @Test
    void unknownShapeMeansUnknown() {
        assertEquals(AlertStatus.UNKNOWN, parser.parse("[]"));
        assertEquals(AlertStatus.UNKNOWN, parser.parse("{}"));
        assertEquals(AlertStatus.UNKNOWN, parser.parse("{\"data\": \"Tel Aviv\"}"));
        assertEquals(AlertStatus.UNKNOWN, parser.parse("{\"data\": [7]}"));
    }

    @Test
    void malformedJsonMeansUnknown() {
        assertEquals(AlertStatus.UNKNOWN, parser.parse("{broken"));
    }

    @Test
    void supportedAreaFixtureMeansActive() throws IOException {
        assertEquals(
                AlertStatus.ACTIVE,
                parser.parse(readFixture("oref/active-supported-area.json"))
        );
    }

    @Test
    void otherAreaFixtureMeansQuiet() throws IOException {
        assertEquals(
                AlertStatus.QUIET,
                parser.parse(readFixture("oref/active-other-area.json"))
        );
    }

    private static String readFixture(String resourceName) throws IOException {
        try (var input = OrefAlertParserTest.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing test fixture: " + resourceName);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
