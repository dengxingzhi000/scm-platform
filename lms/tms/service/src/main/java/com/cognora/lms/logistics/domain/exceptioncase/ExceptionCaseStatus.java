package com.cognora.lms.logistics.domain.exceptioncase;

/**
 * 异常件处理状态机（批次 3）。
 *
 * <p>NEW → CLAIMED → PROCESSING → RESOLVED → CLOSED；NEW/CLAIMED/PROCESSING 可直接关闭。
 * CLOSED 为终态，不可逆。认领与流转均按租户隔离（跨租户视为越权）。</p>
 */
public final class ExceptionCaseStatus {

    public static final String NEW = "NEW";
    public static final String CLAIMED = "CLAIMED";
    public static final String PROCESSING = "PROCESSING";
    public static final String RESOLVED = "RESOLVED";
    public static final String CLOSED = "CLOSED";

    private ExceptionCaseStatus() {
    }

    public static boolean isKnown(String status) {
        return NEW.equals(status) || CLAIMED.equals(status) || PROCESSING.equals(status)
                || RESOLVED.equals(status) || CLOSED.equals(status);
    }

    public static boolean isAllowed(String from, String to) {
        if (!isKnown(from) || !isKnown(to) || from.equals(to)) {
            return false;
        }
        return switch (from) {
            case NEW -> CLAIMED.equals(to) || CLOSED.equals(to);
            case CLAIMED -> PROCESSING.equals(to) || CLOSED.equals(to);
            case PROCESSING -> RESOLVED.equals(to) || CLOSED.equals(to);
            case RESOLVED -> CLOSED.equals(to);
            default -> false;
        };
    }

    public static void assertAllowed(String from, String to) {
        if (!isKnown(from) || !isKnown(to)) {
            throw new IllegalArgumentException("未知的异常件状态: " + from + " -> " + to);
        }
        if (from.equals(to)) {
            return;
        }
        if (CLOSED.equals(from)) {
            throw new IllegalStateException("异常件已关闭，不可再流转: " + to);
        }
        if (!isAllowed(from, to)) {
            throw new IllegalStateException("非法异常件状态流转: " + from + " -> " + to);
        }
    }
}
