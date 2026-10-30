package com.scmcloud.common.mybatisPlus.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Maps {@link LocalDateTime} through {@link ResultSet#getTimestamp}, which works for both
 * {@code timestamp} and {@code timestamptz} columns.
 *
 * <p>PostgreSQL's driver refuses {@code getObject(column, LocalDateTime.class)} on a timestamptz
 * column ("Cannot convert the column of type TIMESTAMPTZ to requested type LocalDateTime"), and
 * several databases still define these columns as timestamptz while the entities use
 * {@link LocalDateTime}.
 */
@MappedTypes(LocalDateTime.class)
public class TimestampLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setTimestamp(i, Timestamp.valueOf(parameter));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value != null ? value.toLocalDateTime() : null;
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp value = rs.getTimestamp(columnIndex);
        return value != null ? value.toLocalDateTime() : null;
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp value = cs.getTimestamp(columnIndex);
        return value != null ? value.toLocalDateTime() : null;
    }
}
