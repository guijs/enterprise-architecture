package com.company.web.async;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池：用于需要并发上限 / 背压 / CPU 密集的任务。
 * 不指定执行器的 @Async 仍走全局虚拟线程；通过 @Async("bizAsyncExecutor") 显式选用本池。
 * TTL 包装保证 @Async 时自动复制用户上下文。
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    @Bean("bizAsyncExecutor")
    public Executor bizAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("biz-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return TtlExecutors.getTtlExecutorService(executor.getThreadPoolExecutor());
    }
}
