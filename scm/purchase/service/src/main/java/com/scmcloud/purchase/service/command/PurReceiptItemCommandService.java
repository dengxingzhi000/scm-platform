package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurReceiptItem;
import com.scmcloud.purchase.mapper.PurReceiptItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurReceiptItemCommandService {

    private final PurReceiptItemMapper purReceiptItemMapper;

    @Master(reason = "保存入库单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurReceiptItem entity) {
        return purReceiptItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新入库单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurReceiptItem entity) {
        return purReceiptItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除入库单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purReceiptItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据入库单ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByReceiptId(String receiptId) {
        LambdaUpdateWrapper<PurReceiptItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurReceiptItem::getReceiptId, receiptId);
        return purReceiptItemMapper.delete(wrapper) > 0;
    }
}
