package com.cognora.scm.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.scm.purchase.domain.entity.PurRequestItem;
import com.cognora.scm.purchase.mapper.PurRequestItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRequestItemCommandService {

    private final PurRequestItemMapper purRequestItemMapper;

    @Master(reason = "保存采购申请明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurRequestItem entity) {
        return purRequestItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新采购申请明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurRequestItem entity) {
        return purRequestItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除采购申请明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purRequestItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据申请ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRequestId(String requestId) {
        LambdaUpdateWrapper<PurRequestItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurRequestItem::getRequestId, requestId);
        return purRequestItemMapper.delete(wrapper) > 0;
    }
}
