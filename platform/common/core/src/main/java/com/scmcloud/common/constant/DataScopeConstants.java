package com.scmcloud.common.constant;

/**
 * 鏁版嵁鏉冮檺鑼冨洿甯搁噺锟?
 * 瀹氫箟绯荤粺涓殑鏁版嵁鏉冮檺鑼冨洿绫诲瀷
 *
 * @author Claude Code
 * @since 2025-01-15
 */
public final class DataScopeConstants {

    private DataScopeConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    /**
     * 鍏ㄩ儴鏁版嵁鏉冮檺
     * 鍙互鏌ョ湅鎵€鏈夋暟锟?
     */
    public static final int SCOPE_ALL = 1;

    /**
     * 鑷畾涔夋暟鎹潈锟?
     * 鍙互鏌ョ湅鎸囧畾閮ㄩ棬鐨勬暟鎹紙閫氳繃 custom_dept_ids 鎸囧畾锟?
     */
    public static final int SCOPE_CUSTOM = 2;

    /**
     * 鏈儴闂ㄦ暟鎹潈锟?
     * 鍙兘鏌ョ湅鏈儴闂ㄧ殑鏁版嵁
     */
    public static final int SCOPE_DEPT = 3;

    /**
     * 鏈儴闂ㄥ強浠ヤ笅鏁版嵁鏉冮檺
     * 鍙互鏌ョ湅鏈儴闂ㄥ強鍏朵笅绾ч儴闂ㄧ殑鏁版嵁
     */
    public static final int SCOPE_DEPT_AND_CHILD = 4;

    /**
     * 浠呮湰浜烘暟鎹潈锟?
     * 鍙兘鏌ョ湅鑷繁鍒涘缓鐨勬暟锟?
     */
    public static final int SCOPE_SELF = 5;
}