package com.scmcloud.auth.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.auth.domain.dto.WebauthnAuthenticationRequest;
import com.scmcloud.auth.domain.dto.WebauthnCredentialDTO;
import com.scmcloud.auth.domain.dto.WebauthnRegistrationRequest;
import com.scmcloud.auth.domain.entity.WebauthnCredential;
import com.scmcloud.common.dto.auth.TokenUpgradeResponse;
import com.scmcloud.common.dto.auth.WebAuthnChallengeResponse;
import com.scmcloud.common.dto.auth.WebAuthnRegisterChallengeResponse;

import java.util.List;
import java.util.UUID;

/**
 * WebAuthn 鍑瘉鏈嶅姟鎺ュ彛
 *
 * @author Deng
 * createData 2025/11/13 10:46
 * @version 1.0
 */
public interface IWebauthnCredentialService extends IService<WebauthnCredential> {

    /**
     * 鐢熸垚娉ㄥ唽鎸戞垬
     * <p>
     * 鍙傝€冿細WebAuthn Level 2 - navigator.credentials.create()
     *
     * @param userId   鐢ㄦ埛 ID
     * @param username 鐢ㄦ埛锟?
     * @param deviceId 璁惧 ID
     * @param rpId     渚濊禆鏂笽D (鍩熷悕)
     * @return 娉ㄥ唽鎸戞垬鍝嶅簲
     */
    WebAuthnRegisterChallengeResponse generateRegistrationChallenge(
            UUID userId, String username, String deviceId, String rpId);

    /**
     * 楠岃瘉骞舵敞鍐屽嚟锟?
     * <p>
     * 瀹夊叏妫€鏌ワ細
     * - 楠岃瘉鎸戞垬鏄惁鏈夋晥涓旀湭杩囨湡
     * - 楠岃瘉璇佹槑(attestation)绛惧悕
     * - 妫€鏌ュ嚟璇両D鏄惁宸插瓨锟?
     * - 楠岃瘉RP ID鍜孫rigin
     *
     * @param userId  鐢ㄦ埛 ID
     * @param request 娉ㄥ唽璇锋眰
     * @return 娉ㄥ唽鐨勫嚟锟紻TO
     */
    WebauthnCredentialDTO registerCredential(UUID userId, WebauthnRegistrationRequest request);

    /**
     * 鐢熸垚璁よ瘉鎸戞垬
     * <p>
     * 鍙傝€冿細WebAuthn Level 2 - navigator.credentials.get()
     *
     * @param userId   鐢ㄦ埛 ID
     * @param username 鐢ㄦ埛锟?
     * @param deviceId 璁惧 ID
     * @param rpId     渚濊禆锟絀D
     * @return 璁よ瘉鎸戞垬鍝嶅簲
     */
    WebAuthnChallengeResponse generateAuthenticationChallenge(
            UUID userId, String username, String deviceId, String rpId);

    /**
     * 楠岃瘉璁よ瘉璇锋眰骞跺崌锟絋oken
     * <p>
     * 瀹夊叏妫€鏌ワ細
     * - 楠岃瘉鎸戞垬鏄惁鏈夋晥涓旀湭杩囨湡
     * - 楠岃瘉绛惧悕璁℃暟鍣ㄩ槻姝㈠厠闅嗘敾锟?
     * - 楠岃瘉鏂█(assertion)绛惧悕
     * - 妫€鏌ュ嚟璇佹槸鍚︽縺锟?
     *
     * @param userId    鐢ㄦ埛 ID
     * @param username  鐢ㄦ埛锟?
     * @param request   璁よ瘉璇锋眰
     * @param deviceId  璁惧 ID
     * @param ipAddress IP 鍦板潃
     * @return Token 鍗囩骇鍝嶅簲
     */
    TokenUpgradeResponse authenticateAndUpgradeToken(
            UUID userId, String username, WebauthnAuthenticationRequest request,
            String deviceId, String ipAddress);

    /**
     * 鑾峰彇鐢ㄦ埛鎵€鏈夋縺娲荤殑鍑瘉
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鍑瘉 DTO鍒楄〃
     */
    List<WebauthnCredentialDTO> listActiveCredentials(UUID userId);

    /**
     * 鏇存柊鍑瘉璁惧鍚嶇О
     *
     * @param userId       鐢ㄦ埛 ID
     * @param credentialId 鍑瘉 ID
     * @param deviceName   鏂拌澶囧悕锟?
     * @return 鏇存柊鍚庣殑鍑瘉 DTO
     */
    WebauthnCredentialDTO updateDeviceName(UUID userId, String credentialId, String deviceName);

    /**
     * 鍋滅敤鍑瘉
     *
     * @param userId       鐢ㄦ埛 ID
     * @param credentialId 鍑瘉 ID
     */
    void deactivateCredential(UUID userId, String credentialId);

    /**
     * 鍒犻櫎鍑瘉
     *
     * @param userId       鐢ㄦ埛 ID
     * @param credentialId 鍑瘉 ID
     */
    void deleteCredential(UUID userId, String credentialId);

    /**
     * 妫€鏌ュ嚟璇佸仴搴风姸锟?
     * <p>
     * 妫€娴嬮」锟?
     * - 绛惧悕璁℃暟鍣ㄥ紓甯革紙鍙兘鐨勫厠闅嗘敾鍑伙級
     * - 闀挎湡鏈娇鐢ㄧ殑鍑瘉
     * - 寮傚父璁よ瘉妯″紡
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 寮傚父鍑瘉鍒楄〃
     */
    List<WebauthnCredentialDTO> checkCredentialHealth(UUID userId);

    /**
     * 璁板綍璁よ瘉灏濊瘯
     *
     * @param userId       鐢ㄦ埛 ID
     * @param credentialId 鍑瘉 ID
     * @param success      鏄惁鎴愬姛
     * @param ipAddress    IP 鍦板潃
     * @param userAgent    鐢ㄦ埛浠g悊
     */
    void logAuthenticationAttempt(UUID userId, String credentialId,
                                   boolean success, String ipAddress, String userAgent);
}
