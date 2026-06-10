package com.scmcloud.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebAuthn 鍑瘉瀹炰綋锟?
 * <p>
 * 鍙傝€冩爣鍑嗭細
 * - W3C Web Authentication API Level 2
 * - FIDO2 CTAP2 Protocol
 * - Google Passkey Implementation

 * 瀹夊叏鐗规€э細
 * - 鍑瘉ID鍏ㄥ眬鍞竴锛岄槻姝㈠嚟璇佺锟?
 * - 绛惧悕璁℃暟鍣ㄩ槻閲嶆斁鏀诲嚮
 * - 鍏挜闅旂瀛樺偍锛岄檷浣庢硠闇查锟?
 * - 鏀寔澶氳澶囩粦瀹氾紝鎻愬崌鐢ㄦ埛浣撻獙

 * 涓氬姟澶栭敭: user_id -> sys_user.user_id (搴旂敤灞備繚璇佸畬鏁达拷
 *
 * @author system
 * @since 2025-11-27
 */
@Data
@EqualsAndHashCode(callSuper = false, of = {"credentialId"})
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(
        value = "webauthn_credential",
        autoResultMap = true
)
public class WebauthnCredential implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 涓婚敭 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private UUID id;

    /**
     * WebAuthn鍑瘉ID (Credential ID)
     * Base64URL缂栫爜鐨勫嚟璇佹爣璇嗙锛岀敱璁よ瘉鍣ㄧ敓鎴愶紝鍏ㄥ眬鍞竴
     * 鐢ㄤ簬瀹㈡埛绔煡鎵惧搴旂殑绉侀挜杩涜绛惧悕
     */
    @TableField(value = "credential_id")
    @NotBlank(message = "Credential ID cannot be empty")
    @Size(min = 16, max = 1024, message = "Credential ID length must be between 16-1024")
    private String credentialId;

    /**
     * 鐢ㄦ埛ID
     * 涓氬姟澶栭敭锛屽叧鑱斿埌sys_user锟?
     */
    @TableField(value = "user_id")
    @NotBlank(message = "User ID cannot be empty")
    @Size(max = 64, message = "User ID length cannot exceed 64")
    private UUID userId;

    /**
     * 鍏挜 (Public Key)
     * 瀛樺偍PEM鏍煎紡鐨勫叕閽ワ紝鐢ㄤ簬楠岃瘉璁よ瘉鍣ㄧ锟?
     * 寤鸿锛氱敓浜х幆澧冨簲鍔犲瘑瀛樺偍鎴栦娇鐢℉SM
     */
    @TableField(value = "public_key_pem")
    @JsonIgnore // 鏁忔劅淇℃伅锛屼笉鍦↗SON鍝嶅簲涓毚锟?
    @NotBlank(message = "Public key cannot be empty")
    @Size(max = 2048, message = "Public key length cannot exceed 2048")
    private String publicKeyPem;

    /**
     * 绛惧悕绠楁硶 (COSE Algorithm)
     * 甯歌鍊硷細
     * - ES256 (-7): ECDSA with SHA-256
     * - RS256 (-257): RSASSA-PKCS1-v1_5 with SHA-256
     * - EdDSA (-8): EdDSA
     */
    @TableField(value = "alg")
    @NotBlank(message = "Signature algorithm cannot be empty")
    @Pattern(regexp = "^(ES256|ES384|ES512|RS256|RS384|RS512|PS256|PS384|PS512|EdDSA)$",
             message = "Unsupported signature algorithm")
    private String alg;

    /**
     * 绛惧悕璁℃暟锟?Signature Counter)
     * 闃查噸鏀炬敾鍑荤殑鍏抽敭鏈哄埗
     * - 姣忔璁よ瘉鍚庡繀椤婚€掑
     * - 濡傛灉璁℃暟鍣ㄤ笉閫掑鎴栧洖閫€锛岃鏄庡彲鑳藉瓨鍦ㄥ厠闅嗘敾锟?
     * - 鍒濆鍊奸€氬父锟?
     */
    @TableField(value = "sign_count")
    @NotNull(message = "Signature counter cannot be null")
    @Min(value = 0, message = "Signature counter cannot be negative")
    private Long signCount;

    /**
     * 璁惧鍚嶇О (鐢ㄦ埛鑷畾锟?
     * 甯姪鐢ㄦ埛璇嗗埆涓嶅悓鐨勮璇佸櫒
     */
    @TableField(value = "device_name")
    @Size(max = 100, message = "Device name length cannot exceed 100")
    private String deviceName;

    /**
     * aaGUID (Authenticator Attestation GUID)
     * 璇嗗埆璁よ瘉鍣ㄥ瀷鍙风殑鍞竴鏍囪瘑锟?
     * 鏍煎紡锛歎UID (渚嬪: 08987058-cadc-4b81-b6e1-30de50dcbe96)
     */
    @TableField(value = "aaguid")
    private UUID aaguid;

    /**
     * 浼犺緭鏂瑰紡 (Transports)
     * 璁よ瘉鍣ㄦ敮鎸佺殑閫氫俊鍗忚
     * 鍙€夊€硷細usb, nfc, ble, internal, hybrid
     * 瀛樺偍鏍煎紡锛歅ostgreSQL TEXT[] 鏁扮粍
     */
    @TableField(value = "transports", typeHandler = com.scmcloud.common.mybatisPlus.handler.StringArrayTypeHandler.class)
    private String[] transports;

    /**
     * 鍑瘉鐘讹拷
     * - true: 婵€娲伙紝鍙敤浜庤锟?
     * - false: 鍋滅敤锛岀敤鎴蜂富鍔ㄧ鐢ㄦ垨绯荤粺妫€娴嬪埌寮傚父
     */
    @TableField(value = "is_active")
    @NotNull(message = "Active status cannot be null")
    private Boolean isActive;

    /**
     * 鏈€鍚庝娇鐢ㄦ椂锟?
     * 鐢ㄤ簬:
     * - 娓呯悊闀挎湡鏈娇鐢ㄧ殑鍑瘉
     * - 妫€娴嬪紓甯哥櫥褰曟ā锟?
     * - 鐢熸垚鐢ㄦ埛娲昏穬搴︽姤锟?
     */
    @TableField(value = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * 鍒涘缓鏃堕棿
     * 鑷姩濉厖锛岀敤浜庡璁″拰缁熻
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 鏇存柊鏃堕棿
     * 鑷姩濉厖锛岃褰曟渶鍚庝慨鏀规椂锟?
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 妫€鏌ュ嚟璇佹槸鍚﹀彲锟?
     *
     * @return true if credential is active and not expired
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(this.isActive);
    }

    /**
     * 妫€鏌ョ鍚嶈鏁板櫒鏄惁鏈夋晥
     * 鐢ㄤ簬妫€娴嬪厠闅嗘敾锟?
     *
     * @param newCounter 鏂扮殑璁℃暟鍣拷
     * @return true if counter is valid (increasing)
     */
    public boolean isCounterValid(Long newCounter) {
        if (newCounter == null || this.signCount == null) {
            return false;
        }
        // 璁℃暟鍣ㄥ繀椤讳弗鏍奸€掑 (闄ら潪璁惧涓嶆敮鎸佽鏁板櫒锛屾鏃跺缁堜负0)
        return newCounter > this.signCount || (newCounter == 0 && this.signCount == 0);
    }

    /**
     * 鏇存柊鏈€鍚庝娇鐢ㄦ椂闂村拰绛惧悕璁℃暟锟?
     *
     * @param newCounter 鏂扮殑绛惧悕璁℃暟鍣拷
     */
    public void updateUsage(Long newCounter) {
        this.lastUsedAt = LocalDateTime.now();
        this.signCount = newCounter;
    }

    /**
     * 鍋滅敤鍑瘉
     * 鐢ㄤ簬鐢ㄦ埛涓诲姩鍒犻櫎鎴栫郴缁熸娴嬪埌寮傚父
     */
    public void deactivate() {
        this.isActive = false;
        this.updateTime = LocalDateTime.now();
    }
}
