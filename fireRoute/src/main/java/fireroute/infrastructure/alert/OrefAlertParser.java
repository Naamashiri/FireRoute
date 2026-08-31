package fireroute.infrastructure.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fireroute.domain.alert.AlertStatus;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Translates a successful Oref JSON response into FireRoute's alert status. */
public class OrefAlertParser {

    private final ObjectMapper objectMapper;
    private final Set<String> supportedAreas;

    public OrefAlertParser(
            ObjectMapper objectMapper,
            Collection<String> supportedAreas
    ) {
        if (objectMapper == null) {
            throw new IllegalArgumentException(
                    "objectMapper must not be null"
            );
        }

        if (supportedAreas == null || supportedAreas.isEmpty()) {
            throw new IllegalArgumentException(
                    "supportedAreas must not be null or empty"
            );
        }

        this.objectMapper = objectMapper;
        this.supportedAreas = supportedAreas.stream()
                .map(OrefAlertParser::normalizeAreaName)
                .filter(area -> !area.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        if (this.supportedAreas.isEmpty()) {
            throw new IllegalArgumentException(
                    "supportedAreas must contain at least one non-blank area"
            );
        }
    }

    public AlertStatus parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return AlertStatus.QUIET;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root == null || !root.isObject()) {
                return AlertStatus.UNKNOWN;
            }

            JsonNode data = root.get("data");

            if (data == null || !data.isArray()) {
                return AlertStatus.UNKNOWN;
            }

            for (JsonNode areaNode : data) {
                if (!areaNode.isTextual()) {
                    return AlertStatus.UNKNOWN;
                }

                String areaName = normalizeAreaName(areaNode.asText());

                if (supportedAreas.contains(areaName)) {
                    return AlertStatus.ACTIVE;
                }
            }

            return AlertStatus.QUIET;

        } catch (Exception exception) {
            return AlertStatus.UNKNOWN;
        }
    }

    /**
     * Oref area names are human-readable text and occasionally differ only in
     * Unicode form, repeated whitespace or the visual kind of dash used.
     */
    private static String normalizeAreaName(String areaName) {
        if (areaName == null) {
            return "";
        }

        String normalized = Normalizer.normalize(areaName, Normalizer.Form.NFKC)
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replace('\u2012', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2212', '-');

        StringBuilder result = new StringBuilder(normalized.length());
        boolean previousWasSpace = false;

        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);

            if (Character.getType(character) == Character.FORMAT) {
                continue;
            }

            if (Character.isWhitespace(character) || Character.isSpaceChar(character)) {
                if (!previousWasSpace && !result.isEmpty()) {
                    result.append(' ');
                }
                previousWasSpace = true;
            } else {
                result.append(character);
                previousWasSpace = false;
            }
        }

        return result.toString().trim();
    }
}
