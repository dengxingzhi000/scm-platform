package com.scmcloud.common.mybatisPlus.handler;

import com.scmcloud.common.domain.TenantId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * MyBatis type handler for {@link TenantId} value object.
 * Maps TenantId ↔ UUID column in the database (PostgreSQL native UUID).
 *
 * <p>TenantId is a thin immutable wrapper around {@link UUID} that provides
 * compile-time distinction from other UUID fields (user IDs, role IDs, etc.).
 * This handler preserves that identity by always using the raw UUID value at
 * the JDBC boundary.</p>
 *
 * @author Deng
 * @since 2026-07-10
 */
@MappedTypes(TenantId.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class TenantIdTypeHandler extends BaseTypeHandler<TenantId> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TenantId parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter.toUUID(), Types.OTHER);
    }

    @Override
    public TenantId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toTenantId(rs.getObject(columnName));
    }

    @Override
    public TenantId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toTenantId(rs.getObject(columnIndex));
    }

    @Override
    public TenantId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toTenantId(cs.getObject(columnIndex));
    }

    private static TenantId toTenantId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return TenantId.from(uuid);
        }
        return TenantId.fromString(value.toString());
    }
}