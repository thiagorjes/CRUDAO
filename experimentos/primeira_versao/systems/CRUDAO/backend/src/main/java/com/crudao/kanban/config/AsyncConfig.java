package com.crudao.kanban.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor dedicado ao cálculo assíncrono do dashboard (ADR-005) — isolado do processamento de
 * eventos de tempo real (que roda em thread própria em {@code PostgresNotificationListener}), para
 * que uma agregação pesada não compita por threads com a entrega de eventos do board.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  @Override
  @Bean(name = "dashboardExecutor")
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("dashboard-async-");
    executor.initialize();
    return executor;
  }
}
