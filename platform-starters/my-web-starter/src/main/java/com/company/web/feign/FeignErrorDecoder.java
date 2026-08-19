package com.company.web.feign;

import cn.hutool.core.util.StrUtil;
import com.company.common.exception.BizException;
import com.company.common.exception.CommonErrorCode;
import com.company.common.response.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * Feign 错误传递：把下游返回的业务错误 body 还原为 BizException，避免吞掉下游真实错误。
 * 熔断/超时/连接失败属基础设施故障，走 Fallback（SERVICE_UNAVAILABLE），不在此处理。
 */
@Slf4j
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.body() == null) {
            return new BizException(
                    CommonErrorCode.SERVICE_UNAVAILABLE.getCode(),
                    "下游无响应体: " + methodKey,
                    HttpStatus.valueOf(response.status()),
                    null);
        }
        try {
            Result<?> result = objectMapper.readValue(response.body().asInputStream(), Result.class);
            HttpStatus status = HttpStatus.resolve(response.status());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            int code = result.getCode() == 0 ? CommonErrorCode.SYSTEM_ERROR.getCode() : result.getCode();
            String message = StrUtil.blankToDefault(result.getMessage(), "下游调用失败");
            return new BizException(code, message, status, result.getData());
        } catch (IOException e) {
            log.error("解析 Feign 错误体失败, method={}", methodKey, e);
            return defaultDecoder.decode(methodKey, response);
        }
    }
}
