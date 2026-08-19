package com.company.log.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

/**
 * 默认审计实现：异步打结构化日志。生产可用自定义 Bean 覆盖为落库 / 发 MQ。
 */
@Slf4j
public class LoggingOperationLogService implements OperationLogService {

    @Override
    @Async("bizAsyncExecutor")
    public void saveAsync(OperationLogEntity entity) {
        log.info("OPERATION_LOG module={} type={} content={} operator={} status={} cost={}ms traceId={}",
                entity.getModule(), entity.getType(), entity.getContent(),
                entity.getOperatorId(), entity.getStatus(), entity.getCostMs(), entity.getTraceId());
    }
}
