package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.mapper.OrdOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 订单明细命令服务（{@code @Master} + 事务）。
 *
 * <p>明细通过 {@link com.scmcloud.order.controller.OrdOrderController#createOrder}
 * 在创建订单时一次性写入；本服务仅暴露 {@code saveBatch}，
 * <b>不提供</b>任意更新 / 删除入口。</p>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderItemCommandService {

    private final OrdOrderItemMapper ordOrderItemMapper;

    @Master(reason = "批量创建订单明细")
    @Transactional(rollbackFor = Exception.class)
    public int saveBatch(List<OrdOrderItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }
        for (OrdOrderItem item : items) {
            ordOrderItemMapper.insert(item);
        }
        log.debug("Saved {} order items", items.size());
        return items.size();
    }
}
