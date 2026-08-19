package com.company.common.enums;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 枚举序列化：输出 {"code":20,"desc":"已支付"}。
 */
public class BaseEnumSerializer extends JsonSerializer<BaseEnum> {

    @Override
    public void serialize(BaseEnum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("code", value.getCode());
        gen.writeStringField("desc", value.getDesc());
        gen.writeEndObject();
    }
}
