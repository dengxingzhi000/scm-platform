package com.scmcloud.common.security.util;

import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
/**
 * TOTP 鍙屽洜绱犺璇佸伐鍏风被
 *
 * @author Deng
 * createData 2025/11/5 17:20
 * @version 1.0
 */
@Component
@Slf4j
public class TotpUtils {
    private static final int SECRET_SIZE = 20; // 160 bits
    private static final String ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP = 30; // 30绉掓椂闂寸獥锟?
    private static final int DIGITS = 6; // 6浣嶉獙璇佺爜
    private static final int WINDOW = 1; // 鍏佽鍓嶅悗1涓椂闂寸獥鍙ｏ紙闃叉鏃堕棿璇樊锟?

    /**
     * 鐢熸垚瀵嗛挜锛圔ase32缂栫爜锟?
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        return BaseEncoding.base32().encode(bytes);
    }

    /**
     * 鐢熸垚浜岀淮鐮乁RL锛堢敤浜嶨oogle Authenticator鎵弿锟?
     *
     * @param account 璐︽埛鍚嶏紙濡傞偖绠辨垨鐢ㄦ埛鍚嶏級
     * @param issuer 鍙戣鑰咃紙搴旂敤鍚嶇О锟?
     * @param secret Base32缂栫爜鐨勫瘑锟?
     */
    public String generateQrCodeUrl(String account, String issuer, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, account, secret, issuer, DIGITS, TIME_STEP
        );
    }

    /**
     * 楠岃瘉 TOTP楠岃瘉锟?
     *
     * @param secret Base32缂栫爜鐨勫瘑锟?
     * @param code 鐢ㄦ埛杈撳叆锟戒綅楠岃瘉鐮?
     * @return 楠岃瘉鏄惁閫氳繃
     */
    public boolean verifyCode(String secret, String code) {
        if (code == null || code.length() != DIGITS) {
            return false;
        }

        try {
            long currentTime = Instant.now().getEpochSecond() / TIME_STEP;

            // 妫€鏌ュ綋鍓嶆椂闂寸獥鍙ｅ拰鍓嶅悗锟絎INDOW涓獥锟?
            for (int i = -WINDOW; i <= WINDOW; i++) {
                long time = currentTime + i;
                String generatedCode = generateCode(secret, time);

                if (code.equals(generatedCode)) {
                    log.debug("TOTP verification successful, time offset: {}", i);
                    return true;
                }
            }

            log.warn("TOTP verification failed");
            return false;
        } catch (Exception e) {
            log.error("TOTP verification exception", e);
            return false;
        }
    }

    /**
     * 鐢熸垚鎸囧畾鏃堕棿鐨勯獙璇佺爜
     */
    private String generateCode(String secret, long timeCounter)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = BaseEncoding.base32().decode(secret);

        // 鏃堕棿杞崲锟藉瓧鑺傛暟缁?
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(timeCounter);
        byte[] timeBytes = buffer.array();

        // HMAC-SHA1
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key, ALGORITHM));
        byte[] hash = mac.doFinal(timeBytes);

        // 鍔ㄦ€佹埅锟?
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, DIGITS);

        // 鏍煎紡鍖栦负6浣嶅瓧绗︿覆锛堝墠瀵奸浂琛ラ綈锟?
        return String.format("%0" + DIGITS + "d", otp);
    }

    /**
     * 鑾峰彇褰撳墠楠岃瘉鐮侊紙鐢ㄤ簬娴嬭瘯锟?
     */
    public String getCurrentCode(String secret) {
        try {
            long currentTime = Instant.now().getEpochSecond() / TIME_STEP;
            return generateCode(secret, currentTime);
        } catch (Exception e) {
            log.error("Failed to generate verification code", e);
            return null;
        }
    }
}
