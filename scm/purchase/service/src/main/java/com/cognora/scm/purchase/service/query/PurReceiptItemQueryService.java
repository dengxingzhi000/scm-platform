package com.cognora.scm.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.purchase.domain.entity.PurReceiptItem;
import com.cognora.scm.purchase.mapper.PurReceiptItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurReceiptItemQueryService {

    private final PurReceiptItemMapper purReceiptItemMapper;

    @Slave
    public PurReceiptItem getById(String id) {
        return purReceiptItemMapper.selectById(id);
    }

    @Slave
    public List<PurReceiptItem> listAll() {
        return purReceiptItemMapper.selectList(null);
    }

    @Slave
    public Page<PurReceiptItem> pageQuery(Page<PurReceiptItem> page, Wrapper<PurReceiptItem> wrapper) {
        return purReceiptItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurReceiptItem> listByReceiptId(String receiptId) {
        LambdaQueryWrapper<PurReceiptItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurReceiptItem::getReceiptId, receiptId);
        wrapper.orderByAsc(PurReceiptItem::getCreateTime);
        return purReceiptItemMapper.selectList(wrapper);
    }
}
