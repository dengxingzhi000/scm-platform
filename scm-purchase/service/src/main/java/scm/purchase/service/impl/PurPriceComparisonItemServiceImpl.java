package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.purchase.domain.entity.PurPriceComparisonItem;
import scm.purchase.mapper.PurPriceComparisonItemMapper;
import scm.purchase.service.IPurPriceComparisonItemService;

import java.util.List;

@Slf4j
@Service
public class PurPriceComparisonItemServiceImpl extends ServiceImpl<PurPriceComparisonItemMapper, PurPriceComparisonItem> implements IPurPriceComparisonItemService {

    @Override
    public List<PurPriceComparisonItem> listByComparisonId(String comparisonId) {
        return lambdaQuery()
                .eq(PurPriceComparisonItem::getComparisonId, comparisonId)
                .orderByAsc(PurPriceComparisonItem::getRank)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByComparisonId(String comparisonId) {
        return lambdaUpdate()
                .eq(PurPriceComparisonItem::getComparisonId, comparisonId)
                .remove();
    }
}
