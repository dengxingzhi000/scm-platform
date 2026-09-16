package com.scmcloud.common.mybatisPlus.context;

/**
 * 鏁版嵁鏉冮檺涓婁笅锟?
 */
public class DataScopeContextHolder {

    private static final ThreadLocal<DataScopeFilter> CONTEXT = new ThreadLocal<>();

    public static void set(DataScopeFilter filter) {
        CONTEXT.set(filter);
    }

    public static DataScopeFilter get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
