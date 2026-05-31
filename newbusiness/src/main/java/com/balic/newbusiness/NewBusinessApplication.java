package com.balic.newbusiness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
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
 */
@SpringBootApplication
@EnableRetry
@EnableAsync
public class NewBusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewBusinessApplication.class, args);
    }
}
