package com.scmcloud.common.license;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnterpriseFeature {
    String module() default "";
    LicenseType minLicenseType() default LicenseType.STANDARD;
    UnavailableAction unavailableAction() default UnavailableAction.THROW_EXCEPTION;

    enum UnavailableAction {
        THROW_EXCEPTION,
        RETURN_NULL,
        RETURN_DEFAULT
    }
}
