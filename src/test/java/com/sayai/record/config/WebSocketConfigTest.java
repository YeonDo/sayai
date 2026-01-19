package com.sayai.record.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketConfigTest {

    @Test
    void webSocketConfig_shouldLoad() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebSocketConfig.class);
        assertThat(context.getBean(WebSocketConfig.class)).isNotNull();
        context.close();
    }
}
