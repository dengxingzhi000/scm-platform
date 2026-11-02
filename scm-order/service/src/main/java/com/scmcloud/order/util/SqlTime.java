package com.scmcloud.order.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Time helpers for SQL against {@code timestamptz} columns.
 *
 * <p>{@code ord_order.create_time} is {@code timestamptz} while the database session runs in
 * UTC and the services run in Asia/Shanghai. Binding a bare {@code LocalDateTime} lets the
 * session zone decide the instant, so "today" shifts by the UTC offset; every time predicate
 * and bucket expression therefore goes through the service zone explicitly.</p>
 */
public final class SqlTime {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private SqlTime() {
    }

    /** Quoted zone literal for {@code at time zone} expressions. */
    public static String zoneLiteral() {
        return "'" + ZONE.getId() + "'";
    }

    /** Offset-aware ISO literal; cast it with {@code ::timestamptz} in SQL. */
    public static String absolute(LocalDateTime value) {
        return value.atZone(ZONE).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
