package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurRfqItem;
import com.scmcloud.purchase.mapper.PurRfqItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRfqItemQueryService {

    private final PurRfqItemMapper purRfqItemMapper;

    @Slave
    public PurRfqItem getById(String id) {
        return purRfqItemMapper.selectById(id);
    }

    @Slave
    public List<PurRfqItem> listAll() {
        return purRfqItemMapper.selectList(null);
    }

    @Slave
    public Page<PurRfqItem> pageQuery(Page<PurRfqItem> page, Wrapper<PurRfqItem> wrapper) {
        return purRfqItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurRfqItem> listByRfqId(String rfqId) {
        LambdaQueryWrapper<PurRfqItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurRfqItem::getRfqId, rfqId);
        wrapper.orderByAsc(PurRfqItem::getCreateTime);
        return purRfqItemMapper.selectList(wrapper);
    }
}
