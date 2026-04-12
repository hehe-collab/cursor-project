package com.drama.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 * 支持多线程池策略：
 * - taskExecutor：通用异步任务
 * - emailExecutor：邮件发送专用
 * - syncExecutor：数据同步专用
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 通用异步任务线程池
     * 适用于：发送钉钉通知、日志处理等一般异步任务
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU 密集型建议核心数+1，IO 密集型建议 2*核心数
        executor.setCorePoolSize(10);

        // 最大线程数
        executor.setMaxPoolSize(50);

        // 队列容量：等待队列满时启用 maxPoolSize
        executor.setQueueCapacity(200);

        // 线程名前缀
        executor.setThreadNamePrefix("async-task-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：由调用线程执行（保证任务不丢失）
        executor.setRejectedExecutionHandler(callerRunsPolicy());

        // 等待所有任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        log.info("通用异步任务线程池初始化完成：核心={}, 最大={}, 队列={}",
                10, 50, 200);
        return executor;
    }

    /**
     * 邮件发送专用线程池
     * 适用于：发送验证码、通知邮件等
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(callerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("邮件发送线程池初始化完成：核心={}, 最大={}, 队列={}", 5, 10, 100);
        return executor;
    }

    /**
     * 数据同步专用线程池
     * 适用于：TikTok 数据同步、报表生成等较重的后台任务
     */
    @Bean(name = "syncExecutor")
    public Executor syncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sync-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(callerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        log.info("数据同步线程池初始化完成：核心={}, 最大={}, 队列={}", 3, 10, 50);
        return executor;
    }

    /**
     * 拒绝策略：由调用线程执行
     * 优点：保证任务不丢失，防止异步任务被拒绝导致数据不一致
     */
    private RejectedExecutionHandler callerRunsPolicy() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }
}
