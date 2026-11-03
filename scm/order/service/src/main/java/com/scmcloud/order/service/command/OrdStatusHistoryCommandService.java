package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.mapper.OrdStatusHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 状态历史写入服务（{@code @Master} + 事务）。
 *
 * <p><b>append-only 审计日志</b>：仅由 {@code OrdOrderCommandService} 在状态流转同一事务内写入。
 * 本服务<b>不暴露公共写入 / 删除方法</b>（包级私有），controller 只能读不能写。</p>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
class OrdStatusHistoryCommandService {

    private final OrdStatusHistoryMapper ordStatusHistoryMapper;

    @Master(reason = "写入状态历史(由 OrdOrderCommandService 调用)")
    @Transactional(rollbackFor = Exception.class)
    int save(OrdStatusHistory history) {
        return ordStatusHistoryMapper.insert(history);
    }
}
