package com.cognora.scm.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.purchase.domain.entity.PurRequestItem;
import com.cognora.scm.purchase.mapper.PurRequestItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRequestItemQueryService {

    private final PurRequestItemMapper purRequestItemMapper;

    @Slave
    public PurRequestItem getById(String id) {
        return purRequestItemMapper.selectById(id);
    }

    @Slave
    public List<PurRequestItem> listAll() {
        return purRequestItemMapper.selectList(null);
    }

    @Slave
    public Page<PurRequestItem> pageQuery(Page<PurRequestItem> page, Wrapper<PurRequestItem> wrapper) {
        return purRequestItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurRequestItem> listByRequestId(String requestId) {
        LambdaQueryWrapper<PurRequestItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurRequestItem::getRequestId, requestId);
        wrapper.orderByAsc(PurRequestItem::getCreateTime);
        return purRequestItemMapper.selectList(wrapper);
    }
}
