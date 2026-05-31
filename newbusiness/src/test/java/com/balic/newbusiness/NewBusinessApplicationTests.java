package com.balic.newbusiness;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the full Spring application context starts successfully.
 *
 * This is the runtime check for the constructor-injection refactor: if any bean
 * (the @Lazy self-reference in NewBusinessService, the @Qualifier journeyTaskExecutor
 * in JourneyOrchestrator, all RestClient-based API clients, RestTemplate/RestClient
 * beans, JPA repositories, etc.) failed to wire, this test would fail to load the
 * context. Runs against in-memory H2 (see application-test.properties) so no external
 * PostgreSQL is required.
 */
@SpringBootTest
@ActiveProfiles("test")
class NewBusinessApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — success means the entire bean graph wired and started.
    }
}
