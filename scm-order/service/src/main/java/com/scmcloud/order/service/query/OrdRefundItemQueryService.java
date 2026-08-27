package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.mapper.OrdRefundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 退款单明细查询服务（{@code @Slave} 路由到只读副本）。
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdRefundItemQueryService {

    private final OrdRefundItemMapper ordRefundItemMapper;

    /**
     * 按退款单 ID 查所有明细行。
     */
    @Slave
    public List<OrdRefundItem> listByRefundId(UUID refundId) {
        if (refundId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OrdRefundItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdRefundItem::getRefundId, refundId)
                .orderByAsc(OrdRefundItem::getId);
        return ordRefundItemMapper.selectList(wrapper);
    }
}
