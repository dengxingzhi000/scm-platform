package com.scmcloud.common.security.util;

/**
 * 鏁版嵁鑴辨晱宸ュ叿锟?
 *
 * @author Deng
 * createData 2025/10/15 14:42
 * @version 1.0
 */
public class DesensitizeUtils {

    /**
     * 鎵嬫満鍙疯劚锟?
     * 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 韬唤璇佽劚锟?
     * 110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 閭鑴辨晱
     * abc****@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 3) {
            return email;
        }
        return username.substring(0, 3) + "****@" + parts[1];
    }

    /**
     * 閾惰鍗¤劚锟?
     * 6222 **** **** 1234
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 16) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 濮撳悕鑴辨晱
     * 锟?
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "**";
    }

    /**
     * 鍦板潃鑴辨晱
     * 淇濈暀鐪佸競锛岃缁嗗湴鍧€鑴辨晱
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }

    /**
     * 鏁忔劅淇℃伅鑴辨晱
     */
    public static String desensitize(String content) {
        if (content == null) return null;

        // 鑴辨晱瀵嗙爜瀛楁
        content = content.replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        // 鑴辨晱韬唤锟?
        content = content.replaceAll("(\"idCard\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
        // 鑴辨晱鎵嬫満锟?
        content = content.replaceAll("(\"phone\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");

        return content;
    }
}

