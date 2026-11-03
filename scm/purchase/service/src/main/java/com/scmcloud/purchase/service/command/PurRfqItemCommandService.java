package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurRfqItem;
import com.scmcloud.purchase.mapper.PurRfqItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRfqItemCommandService {

    private final PurRfqItemMapper purRfqItemMapper;

    @Master(reason = "保存询价单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurRfqItem entity) {
        return purRfqItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新询价单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurRfqItem entity) {
        return purRfqItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除询价单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purRfqItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据询价单ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRfqId(String rfqId) {
        LambdaUpdateWrapper<PurRfqItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurRfqItem::getRfqId, rfqId);
        return purRfqItemMapper.delete(wrapper) > 0;
    }
}
