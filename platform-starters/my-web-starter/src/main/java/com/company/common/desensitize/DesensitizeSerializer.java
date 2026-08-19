package com.company.common.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 脱敏序列化器：读取字段上的 @Desensitize，输出打码后的字符串。
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;
    private int prefixKeep;
    private int suffixKeep;

    public DesensitizeSerializer() {
    }

    public DesensitizeSerializer(DesensitizeType type, int prefixKeep, int suffixKeep) {
        this.type = type;
        this.prefixKeep = prefixKeep;
        this.suffixKeep = suffixKeep;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(DesensitizeUtil.desensitize(value, type, prefixKeep, suffixKeep));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return this;
        }
        Desensitize ann = property.getAnnotation(Desensitize.class);
        if (ann == null) {
            return prov.findValueSerializer(String.class, property);
        }
        return new DesensitizeSerializer(ann.type(), ann.prefixKeep(), ann.suffixKeep());
    }
}
