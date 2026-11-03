package com.scmcloud.gateway.util;

import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 瀹炵幇锛屽寘鎷鑼冨寲涓讳綋浠ュ疄鐜版洿楂樼殑闃茬鏀硅兘鍔涳拷
 */
@Component
public class RequestSignatureCalculator extends AbstractHmacSignatureAlgorithm {
    private static final String VERSION = "HMAC-SHA256-V2";

    @Override
    public String version() {
        return VERSION;
    }
}