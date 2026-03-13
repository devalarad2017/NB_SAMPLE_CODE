package com.insurance.newbusiness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AppConfig — all shared infrastructure beans.
 *
 * ── WHY ONE CONFIG CLASS ─────────────────────────────────────────────────────
 * All beans that are reused across the application (RestTemplate, ObjectMapper,
 * ThreadPool, Swagger) live here. This avoids duplicate bean definitions and
 * makes it easy to find and change infrastructure config in one place.
 *
 * ── OCP NOTE ─────────────────────────────────────────────────────────────────
 * Pool sizes and timeouts are read from application.properties which are
 * populated by OCP ConfigMap at deployment time. No hardcoded values here.
 */
@Configuration
public class AppConfig {

    @Value("${journey.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${journey.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${journey.executor.queue-capacity:100}")
    private int queueCapacity;

    @Value("${api.client.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${api.client.read-timeout-ms:30000}")
    private int readTimeoutMs;

    // ── Thread Pool ───────────────────────────────────────────────────────────
    // Used for two purposes:
    //   1. processJourney() @Async — each inbound request runs its journey here
    //   2. Parallel scoring stage — EDC, PASA, TASA run as CompletableFutures here
    //
    // Sizing:
    //   core = normal concurrent journey count
    //   max  = burst (3 parallel scoring threads × concurrent requests)
    //   CallerRunsPolicy: if all 50 threads and 100 queue slots are full,
    //   the caller (HTTP thread) runs the task — slowdown instead of rejection.
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
    // Single shared instance across all ApiClient beans.
    // Uses connection pooling via PoolingHttpClientConnectionManager so
    // connections to downstream APIs are reused across requests.
    //
    // OCP: downstream services are on the same cluster network.
    // Typical response times are <100ms. 5s connect + 30s read covers most SLAs.
    @Bean
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);          // max total open connections across all routes
        cm.setDefaultMaxPerRoute(20); // max connections per downstream service

        CloseableHttpClient client = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return new RestTemplate(factory);
    }

    // ── ObjectMapper ─────────────────────────────────────────────────────────
    // Shared across all ApiClient beans and MappingService.
    //
    // JavaTimeModule: serialises LocalDate as "1985-03-12" (ISO format) not array.
    // WRITE_DATES_AS_TIMESTAMPS=false: ensures dates go as strings not epoch millis.
    //
    // CRITICAL for hybrid architecture:
    //   objectMapper.convertValue(resolvedMap, SomeRequest.class)
    //   This is the bridge between DB-resolved Map and typed POJO.
    //   Field name in Map must exactly match POJO field name.
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ── Swagger / OpenAPI ────────────────────────────────────────────────────
    // Access at: http://localhost:8080/swagger-ui.html
    // JSON spec: http://localhost:8080/v3/api-docs  (import into Postman)
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Insurance New Business Platform API")
                        .description("Receives insurance applications. Orchestrates eligibility, " +
                                "scoring (parallel), medical, KYC, premium, underwriting, " +
                                "document, proposal, PAS submission, and reverse feed.")
                        .version("1.0.0")
                        .contact(new Contact().name("Insurance Platform Team")));
    }
}
