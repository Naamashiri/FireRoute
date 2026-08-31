package fireroute.infrastructure.offline;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineAssetsTest {

    @Test
    void buildContainsTheOfflineApplicationAndEmergencyData() throws IOException {
        assertResourceExists("static/index.html");
        assertResourceExists("static/offline-routing.js");
        assertResourceExists("static/service-worker.js");
        assertResourceExists("static/manifest.webmanifest");
        assertResourceExists("static/pwa.css");
        assertResourceExists("static/icons/fireroute-app-icon.png");
        assertResourceExists("static/icons/icon-192.png");
        assertResourceExists("static/icons/icon-512.png");
        assertResourceExists("static/icons/icon-maskable-512.png");
        assertResourceExists("static/icons/apple-touch-icon.png");
        assertResourceExists("static/data/map.json");
        assertResourceExists("static/data/shelters.json");
    }

    @Test
    void serviceWorkerInstallsBothEmergencyDataFiles() throws IOException {
        String serviceWorker = readResource("static/service-worker.js");

        assertThat(serviceWorker)
                .contains("/data/map.json")
                .contains("/data/shelters.json")
                .contains("/offline-routing.js")
                .contains("/icons/icon-192.png")
                .contains("/icons/icon-maskable-512.png");
    }

    private void assertResourceExists(String path) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(resource).as("classpath resource %s", path).isNotNull();
            assertThat(resource.read()).as("non-empty resource %s", path).isNotEqualTo(-1);
        }
    }

    private String readResource(String path) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(resource).as("classpath resource %s", path).isNotNull();
            return new String(resource.readAllBytes());
        }
    }
}
