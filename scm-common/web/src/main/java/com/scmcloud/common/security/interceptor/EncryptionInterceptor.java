package com.scmcloud.common.security.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.scmcloud.common.security.annotation.EncryptField;
import com.scmcloud.common.security.crypto.AESEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 *鏁忔劅瀛楁鍔犲瘑鎷︽埅锟?
 *
 * @author Deng
 * createData 2025/10/24 15:09
 * @version 1.0
 */
@Component
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        ),
        @Signature(
                type = ResultSetHandler.class,
                method = "handleResultSets",
                args = {Statement.class}
        )
})
@RequiredArgsConstructor
@Slf4j
public class EncryptionInterceptor implements Interceptor {
    private final AESEncryptor aesEncryptor;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();

        // 鎻掑叆/鏇存柊鏃跺姞锟?
        if (target instanceof StatementHandler handler) {
            BoundSql boundSql = handler.getBoundSql();
            Object parameterObject = boundSql.getParameterObject();

            if (parameterObject != null) {
                PluginUtils.MPStatementHandler mpStatementHandler = PluginUtils.mpStatementHandler(handler);
                MappedStatement mappedStatement = mpStatementHandler.mappedStatement();
                SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

                if (sqlCommandType == SqlCommandType.INSERT ||
                        sqlCommandType == SqlCommandType.UPDATE) {
                    encryptFields(parameterObject);
                }
            }
        }

        // 鏌ヨ鏃惰В锟?
        if (target instanceof ResultSetHandler ) {
            Object result = invocation.proceed();
            if (result instanceof List<?> list) {
                for (Object obj : list) {
                    decryptFields(obj);
                }
            } else if (result != null) {
                decryptFields(result);
            }
            return result;
        }

        return invocation.proceed();
    }

    /**
     * 鍔犲瘑瀛楁
     */
    private void encryptFields(Object obj) throws IllegalAccessException {
        if (obj == null) return;

        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(EncryptField.class)) {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (value instanceof String str) {
                    String encrypted = aesEncryptor.encrypt(str);
                    field.set(obj, encrypted);
                }
            }
        }
    }

    /**
     * 瑙e瘑瀛楁
     */
    private void decryptFields(Object obj) throws IllegalAccessException {
        if (obj == null) return;

        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(EncryptField.class)) {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (value instanceof String str) {
                    String decrypted = aesEncryptor.decrypt(str);
                    field.set(obj, decrypted);
                }
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
