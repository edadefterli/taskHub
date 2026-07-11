package com.taskhub.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServicePortConfigTests {

    @Value("${server.port}")
    private String serverPort;

    @Test
    void should_configureDistinctPortFromTaskService() {
        assertThat(serverPort).isEqualTo("8081");
    }
}
