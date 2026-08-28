package fireroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the FireRoute service.
 *
 * This class deliberately contains nothing but the bootstrap call. It sits
 * directly in the root package because @SpringBootApplication scans for
 * components starting from its own package and downward — placing it any
 * deeper would leave api, application and config invisible to the container.
 *
 * Object wiring lives in {@link fireroute.config.AppConfig}, not here.
 *
 * @EnableScheduling is what makes @Scheduled methods run at all; without it
 * Spring ignores the annotation silently and the alert poller never fires.
 */
@SpringBootApplication
@EnableScheduling
public class FireRouteApplication {

    public static void main(String[] args) {
        SpringApplication.run(FireRouteApplication.class, args);
    }
}
