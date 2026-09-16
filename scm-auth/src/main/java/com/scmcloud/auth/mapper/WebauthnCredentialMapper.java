package com.scmcloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.auth.domain.entity.WebauthnCredential;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * WebAuthn 鍑瘉Mapper鎺ュ彛
 *
 * <p>璇ユ帴鍙f彁渚涗簡瀵筗ebAuthn鍑瘉鏁版嵁鐨勫熀鏈搷浣滐紝鍖呮嫭锟?p>
 * <ul>
 *   <li>鏌ヨ鐢ㄦ埛鐨勬椿璺冨嚟锟?li>
 *   <li>鏍规嵁鐢ㄦ埛 ID鍜屽嚟锟絀D鏌ヨ鐗瑰畾鍑瘉</li>
 *   <li>鏇存柊鍑瘉鐨勭鍚嶈鏁板拰浣跨敤鏃堕棿</li>
 *   <li>鏇存柊鍑瘉鍏宠仈鐨勮澶囧悕锟?li>
 *   <li>绂佺敤鎴栧垹闄ゅ嚟锟?li>
 * </ul>
 *
 * <p>WebAuthn鏄竴绉嶇幇浠g殑韬唤楠岃瘉鏍囧噯锛屽厑璁哥敤鎴蜂娇鐢ㄧ敓鐗╄瘑鍒€佸畨鍏ㄥ瘑閽ョ瓑杩涜鏃犲瘑鐮佺櫥褰曪拷/p>
 */
@Mapper
public interface WebauthnCredentialMapper extends BaseMapper<WebauthnCredential> {
    /**
     * 鏍规嵁鐢ㄦ埛 ID鏌ヨ鎵€鏈夋椿璺冨嚟锟?
     *
     * <p>姝ゆ柟娉曠敤浜庤幏鍙栨寚瀹氱敤鎴风殑鎵€鏈夋湭琚鐢ㄧ殑WebAuthn鍑瘉锟?
     * 缁撴灉鎸夌収鍒涘缓鏃堕棿鍊掑簭鎺掑垪锛堟渶鏂扮殑鍦ㄥ墠锛夛拷/p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @return 鐢ㄦ埛鐨勬墍鏈夋椿璺冨嚟璇佸垪琛紝濡傛灉涓嶅瓨鍦ㄥ垯杩斿洖绌哄垪锟?
     */
    @Select("""
            SELECT * FROM webauthn_credential
            WHERE user_id = #{userId} AND is_active = TRUE
            ORDER BY created_time DESC
            """)
    List<WebauthnCredential> findByUserId(@Param("userId") UUID userId);

    /**
     * 鏍规嵁鐢ㄦ埛 ID鍜屽嚟锟絀D鏌ヨ鍗曚釜鍑瘉
     *
     * <p>姝ゆ柟娉曠敤浜庣簿纭煡鎵句竴涓壒瀹氱殑WebAuthn鍑瘉锟?
     * 鍙湁娲昏穬鐘舵€侊紙鏈绂佺敤锛夌殑鍑瘉鎵嶄細琚繑鍥烇拷/p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @param credentialId 鍑瘉ID锛屼笉鑳戒负锟?
     * @return 鍖归厤鐨勫嚟璇佷俊鎭紝濡傛灉涓嶅瓨鍦ㄥ垯杩斿洖null
     */
    @Select("""
            SELECT * FROM webauthn_credential
            WHERE user_id = #{userId} AND credential_id = #{credentialId} AND is_active = TRUE
            LIMIT 1
            """)
    WebauthnCredential findByUserIdAndCredId(@Param("userId") UUID userId,
                                             @Param("credentialId") String credentialId);

    /**
     * 鏇存柊绛惧悕璁℃暟鍣ㄥ拰鏈€鍚庝娇鐢ㄦ椂锟?
     *
     * <p>姣忔鎴愬姛浣跨敤鍑瘉杩涜韬唤楠岃瘉鍚庤皟鐢ㄦ鏂规硶锟?
     * 鐢ㄤ簬鏇存柊鍑瘉鐨勭鍚嶈鏁板拰鏈€鍚庝娇鐢ㄦ椂闂存埑锟?p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @param credentialId 鍑瘉ID锛屼笉鑳戒负锟?
     * @param signCount 鏂扮殑绛惧悕璁℃暟锛岄€氬父姣斾箣鍓嶇殑鍊煎ぇ
     */
    @Update("""
            UPDATE webauthn_credential
            SET sign_count = #{signCount}, last_used_at = NOW()
            WHERE user_id = #{userId} AND credential_id = #{credentialId}
            """)
    void updateSignCount(@Param("userId") UUID userId,
                         @Param("credentialId") String credentialId,
                         @Param("signCount") Long signCount);

    /**
     * 鏇存柊璁惧鍚嶇О
     *
     * <p>鍏佽鐢ㄦ埛涓哄叾娉ㄥ唽鐨勫嚟璇佽缃槗浜庤瘑鍒殑璁惧鍚嶇О锟?p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @param credentialId 鍑瘉ID锛屼笉鑳戒负锟?
     * @param deviceName 鏂扮殑璁惧鍚嶇О锛屼笉鑳戒负锟?
     * @return 褰卞搷鐨勮鏁帮紝姝e父鎯呭喌涓嬪簲锟?
     */
    @Update("""
            UPDATE webauthn_credential
            SET device_name = #{deviceName}
            WHERE user_id = #{userId} AND credential_id = #{credentialId}
            """)
    int updateDeviceName(@Param("userId") UUID userId,
                         @Param("credentialId") String credentialId,
                         @Param("deviceName") String deviceName);

    /**
     * 绂佺敤鍑瘉(杞垹锟?
     *
     * <p>褰撶敤鎴锋兂瑕佸仠鐢ㄦ煇涓嚟璇佽€屼笉瀹屽叏鍒犻櫎瀹冩椂浣跨敤锟?
     * 姝ゆ搷浣滃皢鍑瘉鏍囪涓洪潪娲昏穬鐘舵€侊紝浣垮叾鏃犳硶鍐嶇敤浜庤韩浠介獙璇侊拷/p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @param credentialId 鍑瘉ID锛屼笉鑳戒负锟?
     * @return 褰卞搷鐨勮鏁帮紝姝e父鎯呭喌涓嬪簲锟?
     */
    @Update("""
            UPDATE webauthn_credential
            SET is_active = FALSE
            WHERE user_id = #{userId} AND credential_id = #{credentialId}
            """)
    int disableCredential(@Param("userId") UUID userId,
                          @Param("credentialId") String credentialId);

    /**
     * 鍒犻櫎鍑瘉(纭垹锟?
     *
     * <p>浠庢暟鎹簱涓案涔呭垹闄ゆ寚瀹氱殑鍑瘉璁板綍锟?
     * 娉ㄦ剰锛氳繖鏄竴涓笉鍙€嗙殑鎿嶄綔锟?p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @param credentialId 鍑瘉ID锛屼笉鑳戒负锟?
     * @return 褰卞搷鐨勮鏁帮紝姝e父鎯呭喌涓嬪簲锟?
     */
    @Delete("""
            DELETE FROM webauthn_credential
            WHERE user_id = #{userId} AND credential_id = #{credentialId}
            """)
    int deleteByUserIdAndCredId(@Param("userId") UUID userId,
                                @Param("credentialId") String credentialId);

    /**
     * 鍒楀嚭鐢ㄦ埛鎵€鏈夋椿璺冨嚟锟芥寜鏈€鍚庝娇鐢ㄦ椂闂存帓锟?
     *
     * <p>鑾峰彇鐢ㄦ埛鐨勬墍鏈夋椿璺冨嚟璇侊紝骞舵寜鐓ф渶鍚庝娇鐢ㄦ椂闂村€掑簭鎺掑垪锟?
     * 鏈€杩戜娇鐢ㄧ殑鍑瘉鎺掑湪鍓嶉潰锛屼粠鏈娇鐢ㄨ繃鐨勫嚟璇佹寜鍒涘缓鏃堕棿鍊掑簭鎺掑垪锟?p>
     *
     * @param userId 鐢ㄦ埛ID锛屼笉鑳戒负锟?
     * @return 鐢ㄦ埛鐨勬墍鏈夋椿璺冨嚟璇佸垪琛紝鎸変娇鐢ㄩ鐜囨帓搴忥紝濡傛灉涓嶅瓨鍦ㄥ垯杩斿洖绌哄垪锟?
     */
    @Select("""
            SELECT * FROM webauthn_credential
            WHERE user_id = #{userId} AND is_active = TRUE
            ORDER BY last_used_at DESC NULLS LAST, created_time DESC
            """)
    List<WebauthnCredential> listActiveCredentials(@Param("userId") UUID userId);
}
