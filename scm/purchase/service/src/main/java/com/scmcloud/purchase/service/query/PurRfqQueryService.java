package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurRfq;
import com.scmcloud.purchase.mapper.PurRfqMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRfqQueryService {

    private final PurRfqMapper purRfqMapper;

    @Slave
    public PurRfq getById(String id) {
        return purRfqMapper.selectById(id);
    }

    @Slave
    public List<PurRfq> listAll() {
        return purRfqMapper.selectList(null);
    }

    @Slave
    public Page<PurRfq> pageQuery(Page<PurRfq> page, Wrapper<PurRfq> wrapper) {
        return purRfqMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurRfq getByRfqNo(String rfqNo) {
        LambdaQueryWrapper<PurRfq> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurRfq::getRfqNo, rfqNo);
        wrapper.eq(PurRfq::getDeleted, false);
        return purRfqMapper.selectOne(wrapper);
    }

    @Slave
    public Page<PurRfq> pageQuery(int page, int size, Integer status, Integer rfqType, String keyword) {
        LambdaQueryWrapper<PurRfq> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurRfq::getStatus, status);
        }
        if (rfqType != null) {
            wrapper.eq(PurRfq::getRfqType, rfqType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurRfq::getRfqNo, keyword)
                    .or()
                    .like(PurRfq::getRfqTitle, keyword));
        }
        wrapper.eq(PurRfq::getDeleted, false);
        wrapper.orderByDesc(PurRfq::getCreateTime);
        return purRfqMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurRfq> listByStatus(Integer status) {
        LambdaQueryWrapper<PurRfq> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurRfq::getStatus, status);
        wrapper.eq(PurRfq::getDeleted, false);
        wrapper.orderByDesc(PurRfq::getCreateTime);
        return purRfqMapper.selectList(wrapper);
    }
}
