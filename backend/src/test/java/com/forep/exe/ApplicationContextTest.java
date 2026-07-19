package com.forep.exe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "forep.ai.service-url=",
        "spring.task.scheduling.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ApplicationContextTest {
    @Test
    void applicationContextStartsWithAllRepositories() {
    }
}
