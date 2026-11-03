package com.scmcloud.common.mybatisPlus.handler;

import com.scmcloud.common.domain.Quantity;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis type handler for {@link Quantity} value object.
 * Maps Quantity ↔ INT in the database.
 */
@MappedTypes(Quantity.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class QuantityTypeHandler extends BaseTypeHandler<Quantity> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Quantity parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.toInt());
    }

    @Override
    public Quantity getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : Quantity.of(value);
    }

    @Override
    public Quantity getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : Quantity.of(value);
    }

    @Override
    public Quantity getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : Quantity.of(value);
    }
}
