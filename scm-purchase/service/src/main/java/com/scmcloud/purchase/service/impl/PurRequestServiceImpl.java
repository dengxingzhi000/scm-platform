package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.mapper.PurRequestMapper;
import com.scmcloud.purchase.service.IPurRequestService;

import java.util.List;

@Slf4j
@Service
public class PurRequestServiceImpl extends ServiceImpl<PurRequestMapper, PurRequest> implements IPurRequestService {

    @Override
    public PurRequest getByRequestNo(String requestNo) {
        return lambdaQuery()
                .eq(PurRequest::getRequestNo, requestNo)
                .eq(PurRequest::getDeleted, false)
                .one();
    }

    @Override
    public Page<PurRequest> pageQuery(int page, int size, Integer status, Integer requestType, String keyword) {
        LambdaQueryWrapper<PurRequest> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurRequest::getStatus, status);
        }
        if (requestType != null) {
            wrapper.eq(PurRequest::getRequestType, requestType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurRequest::getRequestNo, keyword)
                    .or()
                    .like(PurRequest::getRequesterName, keyword)
                    .or()
                    .like(PurRequest::getPurpose, keyword));
        }
        wrapper.eq(PurRequest::getDeleted, false);
        wrapper.orderByDesc(PurRequest::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurRequest> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurRequest::getStatus, status)
                .eq(PurRequest::getDeleted, false)
                .orderByDesc(PurRequest::getCreateTime)
                .list();
    }
}
