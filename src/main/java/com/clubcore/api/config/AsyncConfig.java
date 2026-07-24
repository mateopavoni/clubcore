package com.clubcore.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * Habilita el procesamiento asíncrono ({@code @Async}) usado por
 * {@link com.clubcore.api.service.NotificationDispatchService}.
 *
 * <p>El pool de threads es intencionalmente pequeño para una carga de portfolio.
 * En producción real se escalaría según el volumen de notificaciones.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("clubcore-async-");
        executor.initialize();
        return executor;
    }

    /** RestTemplate para el disparo de webhooks salientes. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
