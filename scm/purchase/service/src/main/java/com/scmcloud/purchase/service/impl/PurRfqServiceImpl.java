package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.purchase.domain.entity.PurRfq;
import com.scmcloud.purchase.mapper.PurRfqMapper;
import com.scmcloud.purchase.service.IPurRfqService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurRfqServiceImpl extends ServiceImpl<PurRfqMapper, PurRfq> implements IPurRfqService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    public PurRfq getByRfqNo(String rfqNo) {
        return lambdaQuery()
                .eq(PurRfq::getRfqNo, rfqNo)
                .eq(PurRfq::getDeleted, false)
                .one();
    }

    @Override
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
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurRfq> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurRfq::getStatus, status)
                .eq(PurRfq::getDeleted, false)
                .orderByDesc(PurRfq::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(String id) {
        PurRfq rfq = getById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "DRAFT", "PENDING_APPROVAL");
        rfq.setStatus(1); // PENDING_APPROVAL
        rfq.setUpdateTime(LocalDateTime.now());
        return updateById(rfq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRfq rfq = getById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        String fromStatus;
        switch (rfq.getStatus()) {
            case 0: fromStatus = "DRAFT"; break;
            case 1: fromStatus = "PENDING_APPROVAL"; break;
            case 2: fromStatus = "APPROVED"; break;
            case 3: fromStatus = "REJECTED"; break;
            case 4: fromStatus = "CANCELLED"; break;
            default: throw new IllegalStateException("未知状态: " + rfq.getStatus());
        }
        statusValidator.validateTransition("PURCHASE", fromStatus, "CANCELLED");
        rfq.setStatus(4); // CANCELLED
        rfq.setUpdateTime(LocalDateTime.now());
        return updateById(rfq);
    }
}
