package com.scmcloud.common.constant;

/**
 * 瑙掕壊甯搁噺锟?
 * 瀹氫箟绯荤粺涓殑瑙掕壊绫诲瀷鍜岃鑹蹭唬鐮佸父锟?
 *
 * @author Claude Code
 * @since 2025-01-15
 */
public final class RoleConstants {

    private RoleConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // ==================== 瑙掕壊绫诲瀷 ====================

    /**
     * 骞冲彴瑙掕壊绫诲瀷
     * 璺ㄦ墍鏈夌鎴凤紝鍙湁骞冲彴绠$悊鍛樺彲浠ョ锟?
     */
    public static final String ROLE_TYPE_PLATFORM = "PLATFORM_ROLE";

    /**
     * 绉熸埛瑙掕壊绫诲瀷
     * 绉熸埛鍐呰鑹诧紝绉熸埛绠$悊鍛樺彲浠ョ锟?
     */
    public static final String ROLE_TYPE_TENANT = "TENANT_ROLE";

    // ==================== 瑙掕壊浠g爜锛圫pring Security 瑙掕壊鍚嶇О锟?==================

    /**
     * 瓒呯骇绠$悊鍛樿锟?
     * 鎷ユ湁绯荤粺鏈€楂樻潈锟?
     */
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    /**
     * 骞冲彴绠$悊鍛樿锟?
     * 鍙互绠$悊鎵€鏈夌鎴峰拰骞冲彴绾ц祫锟?
     */
    public static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    /**
     * 绉熸埛绠$悊鍛樿锟?
     * 鍙互绠$悊鏈鎴峰唴鐨勮祫婧愬拰鐢ㄦ埛
     */
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    /**
     * 绉熸埛鏅€氱敤鎴疯锟?
     */
    public static final String ROLE_TENANT_USER = "ROLE_TENANT_USER";

    // ==================== 瑙掕壊鍒嗙被 ====================

    /**
     * 涓氬姟瑙掕壊
     */
    public static final String ROLE_CATEGORY_BUSINESS = "BUSINESS";

    /**
     * 鑱岃兘瑙掕壊
     */
    public static final String ROLE_CATEGORY_FUNCTIONAL = "FUNCTIONAL";

    /**
     * 鑷畾涔夎锟?
     */
    public static final String ROLE_CATEGORY_CUSTOM = "CUSTOM";

    // ==================== 鐢ㄦ埛绫诲瀷锛堝吋瀹规棫浠g爜锟?==================

    /**
     * 骞冲彴绠$悊鍛樼敤鎴风被锟?
     */
    public static final String USER_TYPE_PLATFORM_ADMIN = "PLATFORM_ADMIN";

    /**
     * 绉熸埛绠$悊鍛樼敤鎴风被锟?
     */
    public static final String USER_TYPE_TENANT_ADMIN = "TENANT_ADMIN";

    /**
     * 绉熸埛鏅€氱敤鎴风被锟?
     */
    public static final String USER_TYPE_TENANT_USER = "TENANT_USER";
}