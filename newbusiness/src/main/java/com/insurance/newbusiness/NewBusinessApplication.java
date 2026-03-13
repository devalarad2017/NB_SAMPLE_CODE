package com.insurance.newbusiness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry point.
 *
 * @EnableRetry  — activates @Retryable on all ApiClient beans.
 *                 Without this annotation, @Retryable is ignored silently.
 *
 * @EnableAsync  — activates @Async on NewBusinessService.processJourney().
 *                 Without this, processJourney() runs synchronously on the
 *                 HTTP thread and the partner waits for the full journey.
 *
 * OCP Deployment:
 *   Build: mvn clean package -DskipTests
 *   Image: FROM registry.access.redhat.com/ubi8/openjdk-8:latest
 *          COPY target/newbusiness-1.0.0-SNAPSHOT.jar app.jar
 *          ENTRYPOINT ["java", "-jar", "app.jar"]
 *   Liveness probe:  GET /actuator/health/liveness
 *   Readiness probe: GET /actuator/health/readiness
 */
@SpringBootApplication
@EnableRetry
@EnableAsync
public class NewBusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewBusinessApplication.class, args);
    }
}
