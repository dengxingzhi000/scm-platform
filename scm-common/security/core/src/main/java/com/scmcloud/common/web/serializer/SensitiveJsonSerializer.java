package com.scmcloud.common.web.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.scmcloud.common.web.annotation.Sensitive;
import com.scmcloud.common.web.enums.SensitiveType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 敏感数据序列化器，JSON 序列化时自动脱敏
 *
 * @author Deng
 * @since 2025/10/30
 * @version 1.1
 * @apiNote 1.1 精简序列化分支，修复乱码注释，移除热路径日志
 */
@Slf4j
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private SensitiveType type;
    private boolean enabled = true;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty() || !enabled || type == null) {
            gen.writeString(value);
            return;
        }

        try {
            gen.writeString(type.desensitize(value));
        } catch (Exception e) {
            log.error("Failed to desensitize with type {}: {}", type, e.getMessage());
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }

        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive == null) {
            return prov.findValueSerializer(property.getType(), property);
        }

        SensitiveJsonSerializer serializer = new SensitiveJsonSerializer();
        serializer.type = sensitive.type();
        serializer.enabled = sensitive.enabled();
        return serializer;
    }
}
