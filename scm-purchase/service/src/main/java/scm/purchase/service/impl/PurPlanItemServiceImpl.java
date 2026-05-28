package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.purchase.domain.entity.PurPlanItem;
import scm.purchase.mapper.PurPlanItemMapper;
import scm.purchase.service.IPurPlanItemService;

import java.util.List;

@Slf4j
@Service
public class PurPlanItemServiceImpl extends ServiceImpl<PurPlanItemMapper, PurPlanItem> implements IPurPlanItemService {

    @Override
    public List<PurPlanItem> listByPlanId(String planId) {
        return lambdaQuery()
                .eq(PurPlanItem::getPlanId, planId)
                .orderByAsc(PurPlanItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPlanId(String planId) {
        return lambdaUpdate()
                .eq(PurPlanItem::getPlanId, planId)
                .remove();
    }
}
