package com.cognora.lms.logistics.domain.exceptioncase;

/**
 * 理赔单据状态机（批次 3）。
 *
 * <p>DRAFT → SUBMITTED → APPROVED / REJECTED；APPROVED → PAID。
 * REJECTED 与 PAID 为终态。</p>
 */
public final class ClaimOrderStatus {

    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String PAID = "PAID";

    private ClaimOrderStatus() {
    }

    public static boolean isKnown(String status) {
        return DRAFT.equals(status) || SUBMITTED.equals(status) || APPROVED.equals(status)
                || REJECTED.equals(status) || PAID.equals(status);
    }

    public static boolean isAllowed(String from, String to) {
        if (!isKnown(from) || !isKnown(to) || from.equals(to)) {
            return false;
        }
        return switch (from) {
            case DRAFT -> SUBMITTED.equals(to);
            case SUBMITTED -> APPROVED.equals(to) || REJECTED.equals(to);
            case APPROVED -> PAID.equals(to);
            default -> false;
        };
    }

    public static void assertAllowed(String from, String to) {
        if (!isKnown(from) || !isKnown(to)) {
            throw new IllegalArgumentException("未知的理赔单状态: " + from + " -> " + to);
        }
        if (from.equals(to)) {
            return;
        }
        if (REJECTED.equals(from) || PAID.equals(from)) {
            throw new IllegalStateException("理赔单已终态，不可再流转: " + to);
        }
        if (!isAllowed(from, to)) {
            throw new IllegalStateException("非法理赔单状态流转: " + from + " -> " + to);
        }
    }
}
