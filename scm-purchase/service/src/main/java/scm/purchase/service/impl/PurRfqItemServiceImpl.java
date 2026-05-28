package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.purchase.domain.entity.PurRfqItem;
import scm.purchase.mapper.PurRfqItemMapper;
import scm.purchase.service.IPurRfqItemService;

import java.util.List;

@Slf4j
@Service
public class PurRfqItemServiceImpl extends ServiceImpl<PurRfqItemMapper, PurRfqItem> implements IPurRfqItemService {

    @Override
    public List<PurRfqItem> listByRfqId(String rfqId) {
        return lambdaQuery()
                .eq(PurRfqItem::getRfqId, rfqId)
                .orderByAsc(PurRfqItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRfqId(String rfqId) {
        return lambdaUpdate()
                .eq(PurRfqItem::getRfqId, rfqId)
                .remove();
    }
}
