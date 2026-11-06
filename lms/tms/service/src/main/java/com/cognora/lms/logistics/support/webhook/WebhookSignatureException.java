package com.cognora.lms.logistics.support.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Webhook 验签失败（签名错误/时间窗超限/nonce 重放/密钥缺失）。
 *
 * <p>统一映射为 HTTP 401，调用方据此区分「未授权回调」。</p>
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message) {
        super(message);
    }
}
