package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单本地事件溯源记录（{@code ord_order_event} 表）。
 *
 * <p><b>职责</b>：append-only 事件流，由 {@code OrderEventStore.append()} 写入；唯一约束
 * {@code uk_ord_order_event_event_id} 保证幂等。供 {@code OrderAggregate.rehydrate()}
 * 重放重建聚合使用，也可被 scm-message 等下游订阅消费。</p>
 *
 * <p><b>与 {@link OrdStatusHistory} 的关系</b>：两者并存，记录不同维度，<b>不冗余</b>：</p>
 * <ul>
 *   <li>本表：事件 payload（{@code eventData} JSON 全字段）+ {@code eventId} 幂等键 + {@code eventType}</li>
 *   <li>{@link OrdStatusHistory}：操作者审计（{@code operatorId} / {@code operatorName} /
 *       {@code operatorType} / {@code remark} / {@code extraData}）</li>
 * </ul>
 *
 * <p><b>写入路径</b>：所有状态变更由 {@code OrdOrderCommandService} 同一事务内调用
 * {@code OrderEventStore.append(...)} 写入本表。<b>不要</b>在外部手动插入，避免与状态机校验脱节。</p>
 *
 * @author deng
 */
@Data
@TableName("ord_order_event")
public class OrdOrderEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private UUID eventId;

    @TableField("tenant_id")
    private UUID tenantId;

    private UUID orderId;

    @TableField("order_no")
    private String orderNo;

    private String eventType;

    private String eventData;

    @TableField("create_time")
    private LocalDateTime createTime;
}
