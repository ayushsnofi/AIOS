package com.aios;

import com.aios.config.TestMemoryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMemoryConfig.class)
class AiosApplicationTests {

    @Test
    void contextLoads() {
    }
}
