package com.company.log.operation;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体（落库字段）。为不强绑 MyBatis-Plus，此处为纯 POJO；
 * 业务侧可继承并加 @TableName/@TableId 映射到 sys_operation_log。
 */
@Data
public class OperationLogEntity {

    private Long id;
    private String module;
    private String type;
    private String content;
    private String method;
    private String requestUri;
    private String requestMethod;
    private String params;
    private String result;
    private String operatorId;
    private String operatorName;
    private String ip;
    private String traceId;
    private Integer status;
    private String errorMsg;
    private Long costMs;
    private LocalDateTime createTime;
}
