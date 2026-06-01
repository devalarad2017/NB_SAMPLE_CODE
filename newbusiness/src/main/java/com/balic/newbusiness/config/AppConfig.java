package com.balic.newbusiness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AppConfig — all shared infrastructure beans in one place.
 *
 * Pool sizes and timeouts are read from application.properties
 * which OCP ConfigMap populates at deployment time.
 */
@Configuration
public class AppConfig {

    @Value("${journey.executor.core-pool-size:15}")
    private int corePoolSize;

    @Value("${journey.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${journey.executor.queue-capacity:200}")
    private int queueCapacity;

    @Value("${api.client.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${api.client.read-timeout-ms:30000}")
    private int readTimeoutMs;

    // ── Thread Pool ───────────────────────────────────────────────────────────
    // Used for:
    //   1. processJourney() @Async — each inbound request runs its journey here
    //   2. Parallel scoring stage — EDC, PASA, TASA as CompletableFutures
    // CallerRunsPolicy: if pool + queue are full, HTTP thread runs the task itself — graceful slowdown instead of rejection.
    @Bean(name = "journeyTaskExecutor")
    public Executor journeyTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("journey-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // ── RestTemplate ─────────────────────────────────────────────────────────
    // Simple factory with connect + read timeouts. No connection pooling for now.
    //
    // Connection pooling commented out below — re-enable when moving to production
    // with high concurrency and multiple downstream services.
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    // ── RestClient (Spring 6.1+, the modern fluent HTTP client) ───────────────
    // Provided alongside RestTemplate using the SAME connect/read timeouts so the
    // integration clients can be migrated to it gradually, one at a time, without
    // changing existing RestTemplate-based behaviour. Nothing is forced to switch.
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        // Register JavaTimeModule so LocalDate / LocalDateTime serialize correctly
        // (otherwise ErrorResponse.timestamp etc. throw InvalidDefinitionException on Boot 3.x).
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Swagger / OpenAPI ────────────────────────────────────────────────────
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bajajlife New Business Platform API")
                        .description("Receives NB applications. Orchestrates CIBIL, " +
                                "scoring (parallel), medical, KYC, premium, underwriting, " +
                                "document, proposal, PAS submission, and reverse feed.")
                        .version("1.0.0")
                        .contact(new Contact().name("BajajLife New Business Team")));
    }
}
