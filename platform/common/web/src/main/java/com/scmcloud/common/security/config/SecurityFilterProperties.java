package com.scmcloud.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 瀹夊叏杩囨护鐩稿叧閰嶇疆
 */
@Component
@ConfigurationProperties(prefix = "frog.security.sql-filter")
@Data
public class SecurityFilterProperties {

    /** 鏄惁鍚敤 SQL/XSS 杩囨护 */
    private boolean enabled = true;

    /** SQL/XSS 杩囨护鍔ㄤ綔锛欰LERT 浠呭憡璀︼紝BLOCK 鎷︽埅 */
    private SqlFilterMode mode = SqlFilterMode.ALERT;

    private boolean xssEnabled = true;

    public boolean isXssEnabled() {
        return xssEnabled;
    }

    /** 鎸夎矾寰勬帓闄わ紙Ant 琛ㄨ揪寮忥級锛屼緥濡傦細/api/public/** */
    private List<String> excludePaths = new ArrayList<>();

    public enum SqlFilterMode {
        ALERT,
        BLOCK
    }
}
