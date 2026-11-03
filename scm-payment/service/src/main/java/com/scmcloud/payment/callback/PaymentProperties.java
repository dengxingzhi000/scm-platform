package com.scmcloud.payment.callback;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Channel credentials required to authenticate asynchronous payment callbacks.
 * Nothing here has a production default: when a key is blank the matching
 * callback endpoint rejects the request instead of trusting the payload.
 */
@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private Alipay alipay = new Alipay();
    private Wechat wechat = new Wechat();

    @Data
    public static class Alipay {
        /** Alipay open-platform public key (PEM or raw Base64 X.509). */
        private String publicKey;
        /** The app_id this platform registered with Alipay. */
        private String appId;
        /** Optional seller_id, checked only when configured. */
        private String sellerId;
        /** Charset used to build the signed content, UTF-8 by default. */
        private String charset = "UTF-8";
        /**
         * Timezone Alipay renders {@code notify_time} in. Alipay sends that
         * field in the merchant's local time, which is Asia/Shanghai for a
         * mainland merchant; the replay window is checked against it.
         */
        private String timezone = "Asia/Shanghai";
    }

    @Data
    public static class Wechat {
        /** WeChat Pay platform certificate (PEM) or its public key. */
        private String platformCertificate;
        /** Optional Wechatpay-Serial value expected on callbacks. */
        private String platformCertificateSerial;
        /** 32-byte API v3 key used to decrypt resource.ciphertext. */
        private String apiV3Key;
        /** The merchant id this platform registered with WeChat Pay. */
        private String mchId;
    }
}
