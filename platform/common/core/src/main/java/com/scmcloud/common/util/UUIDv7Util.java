package com.scmcloud.common.util;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.util.UuidUtil;

import java.util.UUID;

/**
 * UUID v7鐗堟湰宸ュ叿锟?
 * 鎻愪緵鍩轰簬鏃堕棿鎴崇殑UUID v7鐢熸垚鍜岃В鏋愬姛锟?
 *
 * @author Deng
 * createData 2025/10/17 14:35
 * @version 1.0
 */
public class UUIDv7Util {
    /**
     * 鐢熸垚UUID
     * 鍩轰簬褰撳墠鏃堕棿鎴崇敓鎴愭湁搴忕殑UUID锛岄€傜敤浜庢暟鎹簱涓婚敭绛夊満锟?
     * 
     * @return UUID 瀹炰緥
     */
    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /**
     * 鐢熸垚UUID 瀛楃锟?
     * 
     * @return 鏍囧噯鏍煎紡鐨刄UID瀛楃涓诧紙鍖呭惈杩炲瓧绗︼級
     */
    public static String generateString() {
        return generate().toString();
    }

    /**
     * 鐢熸垚UUID瀛楃涓诧紙鏃犺繛瀛楃锟?
     * 
     * @return 绱у噾鏍煎紡鐨刄UID瀛楃涓诧紙涓嶅惈杩炲瓧绗︼級
     */
    public static String generateCompact() {
        return generateString().replace("-", "");
    }

    /**
     * 浠嶶UID鎻愬彇鏃堕棿锟?
     * 娉ㄦ剰锛氭鏂规硶浠呴€傜敤浜嶶UID鏍煎紡锛屽鍏朵粬鐗堟湰UUID鍙兘鎶涘嚭寮傚父
     * 
     * @param uuid UUID 瀹炰緥
     * @return 鏃堕棿鎴筹紙姣锟?
     */
    public static long extractTimestamp(UUID uuid) {
        return UuidUtil.getInstant(uuid).toEpochMilli();
    }
}
