package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.mapper.PurRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRequestQueryService {

    private final PurRequestMapper purRequestMapper;

    @Slave
    public PurRequest getById(String id) {
        return purRequestMapper.selectById(id);
    }

    @Slave
    public List<PurRequest> listAll() {
        return purRequestMapper.selectList(null);
    }

    @Slave
    public Page<PurRequest> pageQuery(Page<PurRequest> page, Wrapper<PurRequest> wrapper) {
        return purRequestMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurRequest getByRequestNo(String requestNo) {
        LambdaQueryWrapper<PurRequest> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurRequest::getRequestNo, requestNo);
        wrapper.eq(PurRequest::getDeleted, false);
        return purRequestMapper.selectOne(wrapper);
    }

    @Slave
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
        return purRequestMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurRequest> listByStatus(Integer status) {
        LambdaQueryWrapper<PurRequest> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurRequest::getStatus, status);
        wrapper.eq(PurRequest::getDeleted, false);
        wrapper.orderByDesc(PurRequest::getCreateTime);
        return purRequestMapper.selectList(wrapper);
    }
}
