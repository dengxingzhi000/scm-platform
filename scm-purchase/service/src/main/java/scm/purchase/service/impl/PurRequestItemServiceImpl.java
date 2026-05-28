package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.purchase.domain.entity.PurRequestItem;
import scm.purchase.mapper.PurRequestItemMapper;
import scm.purchase.service.IPurRequestItemService;

import java.util.List;

@Slf4j
@Service
public class PurRequestItemServiceImpl extends ServiceImpl<PurRequestItemMapper, PurRequestItem> implements IPurRequestItemService {

    @Override
    public List<PurRequestItem> listByRequestId(String requestId) {
        return lambdaQuery()
                .eq(PurRequestItem::getRequestId, requestId)
                .orderByAsc(PurRequestItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRequestId(String requestId) {
        return lambdaUpdate()
                .eq(PurRequestItem::getRequestId, requestId)
                .remove();
    }
}
