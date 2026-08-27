package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.mapper.OrdRefundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

/**
 * 退款单明细写入服务（{@code @Master} + 事务）。
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdRefundItemCommandService {

    private final OrdRefundItemMapper ordRefundItemMapper;

    /**
     * 批量保存退款明细行。
     *
     * <p>调用方需保证 {@link OrdRefundItem#getRefundId()} 已在事务外或同一事务内赋值。</p>
     */
    @Master(reason = "写入退款明细")
    @Transactional(rollbackFor = Exception.class)
    public int saveBatch(List<OrdRefundItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }
        for (OrdRefundItem item : items) {
            ordRefundItemMapper.insert(item);
        }
        log.debug("Saved {} refund items", items.size());
        return items.size();
    }
}
