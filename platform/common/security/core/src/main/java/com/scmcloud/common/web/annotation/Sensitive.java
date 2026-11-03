package com.scmcloud.common.web.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scmcloud.common.web.enums.SensitiveType;
import com.scmcloud.common.web.serializer.SensitiveJsonSerializer;

import java.lang.annotation.*;

/**
 * 敏感数据脱敏注解
 *
 * @author Deng
 * @since 2025/10/30
 * @version 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive {

    /** 脱敏类型 */
    SensitiveType type() default SensitiveType.MOBILE;

    /** 是否启用脱敏 */
    boolean enabled() default true;
}
