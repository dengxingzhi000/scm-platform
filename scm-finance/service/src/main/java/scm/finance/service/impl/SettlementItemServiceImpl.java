package scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import scm.finance.domain.entity.SettlementItem;
import scm.finance.mapper.SettlementItemMapper;
import scm.finance.service.ISettlementItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SettlementItemServiceImpl extends ServiceImpl<SettlementItemMapper, SettlementItem>
        implements ISettlementItemService {

    @Override
    public List<SettlementItem> listBySettlementId(String settlementId) {
        log.debug("查询结算明细: settlementId={}", settlementId);
        LambdaQueryWrapper<SettlementItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SettlementItem::getSettlementId, settlementId)
                .orderByDesc(SettlementItem::getDocumentDate);
        return list(wrapper);
    }
}
