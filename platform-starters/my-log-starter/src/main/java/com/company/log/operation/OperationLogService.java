package com.company.log.operation;

/**
 * 审计日志写入契约。默认实现仅打日志；业务侧可覆盖为异步落库 / 发 MQ。
 * 异步失败要有本地补偿或告警，审计不可静默丢失。
 */
public interface OperationLogService {

    void saveAsync(OperationLogEntity entity);
}
