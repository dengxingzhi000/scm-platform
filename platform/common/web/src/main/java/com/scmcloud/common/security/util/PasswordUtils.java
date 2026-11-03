package com.scmcloud.common.security.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 瀵嗙爜宸ュ叿锟?
 *
 * @author Deng
 * createData 2025/10/15 14:41
 * @version 1.0
 */
public class PasswordUtils {
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "@#$%^&+=!";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 鐢熸垚闅忔満瀵嗙爜
     */
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8;
        }

        StringBuilder password = new StringBuilder(length);
        String allChars = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS;

        // 纭繚鑷冲皯鍖呭惈姣忕绫诲瀷鐨勫瓧锟?
        password.append(UPPER_CASE.charAt(RANDOM.nextInt(UPPER_CASE.length())));
        password.append(LOWER_CASE.charAt(RANDOM.nextInt(LOWER_CASE.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(RANDOM.nextInt(SPECIAL_CHARS.length())));

        // 濉厖鍓╀綑闀垮害
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(RANDOM.nextInt(allChars.length())));
        }

        // 鎵撲贡椤哄簭
        return shuffleString(password.toString());
    }

    /**
     * 楠岃瘉瀵嗙爜寮哄害
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 鑾峰彇瀵嗙爜寮哄害绛夌骇 (0-4)
     * 0: 寰堝急
     * 1: 锟?
     * 2: 涓瓑
     * 3: 锟?
     * 4: 寰堝己
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int strength = 0;

        // 闀垮害妫€锟?
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;

        // 鍖呭惈灏忓啓瀛楁瘝
        if (password.matches(".*[a-z].*")) strength++;

        // 鍖呭惈澶у啓瀛楁瘝
        if (password.matches(".*[A-Z].*")) strength++;

        // 鍖呭惈鏁板瓧
        if (password.matches(".*\\d.*")) strength++;

        // 鍖呭惈鐗规畩瀛楃
        if (password.matches(".*[@#$%^&+=!].*")) strength++;

        return Math.min(strength / 2, 4);
    }

    private static String shuffleString(String string) {
        char[] chars = string.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}

