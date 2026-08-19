package com.company.common.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * 枚举反序列化：支持传 20 / "20" / {"code":20}，最终映射为 BaseEnum。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum> implements ContextualDeserializer {

    private Class<? extends Enum> enumType;

    public BaseEnumDeserializer() {
    }

    public BaseEnumDeserializer(Class<? extends Enum> enumType) {
        this.enumType = enumType;
    }

    @Override
    public BaseEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        Integer code = resolveCode(node);
        return (BaseEnum) BaseEnum.of((Class) enumType, code);
    }

    private Integer resolveCode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject() && node.has("code")) {
            return node.get("code").asInt();
        }
        if (node.isTextual()) {
            return Integer.valueOf(node.asText());
        }
        return node.asInt();
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        Class<?> raw = property.getType().getRawClass();
        return new BaseEnumDeserializer((Class<? extends Enum>) raw);
    }
}
