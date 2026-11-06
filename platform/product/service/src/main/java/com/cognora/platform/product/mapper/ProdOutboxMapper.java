package com.cognora.platform.product.mapper;

import com.cognora.platform.product.outbox.ProdOutboxEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * prod_outbox 读写。
 *
 * <p>不使用 MyBatis-Plus 的 BaseMapper：outbox 表只有一个用途（先插入、后投递），
 * 手写 SQL 可以把「未发布」「重试上限」这些条件直接写进语句，也避免自动填充
 * 覆盖调用方显式设置的 tenant_id。</p>
 */
@Mapper
public interface ProdOutboxMapper {

    @Insert("""
            INSERT INTO prod_outbox
                (id, tenant_id, aggregate_id, event_type, payload, created_at, retry_count)
            VALUES
                (#{id}, #{tenantId}, #{aggregateId}, #{eventType}, #{payload}::jsonb, #{createdAt}, #{retryCount})
            """)
    int insert(ProdOutboxEvent event);

    /**
     * 取未发布且未超过重试上限的行。按 created_at 升序，保证同一商品的事件顺序。
     */
    @Select("""
            SELECT id, tenant_id, aggregate_id, event_type, payload, created_at, published_at, retry_count
            FROM prod_outbox
            WHERE published_at IS NULL
              AND retry_count < #{maxRetries}
            ORDER BY created_at ASC
            LIMIT #{batchSize}
            """)
    List<ProdOutboxEvent> findPublishable(@Param("maxRetries") int maxRetries,
                                          @Param("batchSize") int batchSize);

    /**
     * 投递成功后回填 published_at。条件里的 published_at IS NULL 让重复投递保持幂等。
     */
    @Update("UPDATE prod_outbox SET published_at = now() WHERE id = #{id} AND published_at IS NULL")
    int markPublished(@Param("id") String id);

    /**
     * 投递失败只累加重试次数，published_at 保持 NULL，行仍然可见。
     */
    @Update("UPDATE prod_outbox SET retry_count = retry_count + 1 WHERE id = #{id} AND published_at IS NULL")
    int incrementRetryCount(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM prod_outbox WHERE published_at IS NULL")
    long countUnpublished();
}
