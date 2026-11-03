package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurContract;
import com.scmcloud.purchase.mapper.PurContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurContractQueryService {

    private final PurContractMapper purContractMapper;

    @Slave
    public PurContract getById(String id) {
        return purContractMapper.selectById(id);
    }

    @Slave
    public List<PurContract> listAll() {
        return purContractMapper.selectList(null);
    }

    @Slave
    public Page<PurContract> pageQuery(Page<PurContract> page, Wrapper<PurContract> wrapper) {
        return purContractMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurContract getByContractNo(String contractNo) {
        LambdaQueryWrapper<PurContract> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurContract::getContractNo, contractNo);
        wrapper.eq(PurContract::getDeleted, false);
        return purContractMapper.selectOne(wrapper);
    }

    @Slave
    public Page<PurContract> pageQuery(int page, int size, Integer status, Integer contractType, String supplierId, String keyword) {
        LambdaQueryWrapper<PurContract> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurContract::getStatus, status);
        }
        if (contractType != null) {
            wrapper.eq(PurContract::getContractType, contractType);
        }
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(PurContract::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurContract::getContractNo, keyword)
                    .or()
                    .like(PurContract::getContractName, keyword)
                    .or()
                    .like(PurContract::getSupplierName, keyword));
        }
        wrapper.eq(PurContract::getDeleted, false);
        wrapper.orderByDesc(PurContract::getCreateTime);
        return purContractMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurContract> listByStatus(Integer status) {
        LambdaQueryWrapper<PurContract> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurContract::getStatus, status);
        wrapper.eq(PurContract::getDeleted, false);
        wrapper.orderByDesc(PurContract::getCreateTime);
        return purContractMapper.selectList(wrapper);
    }

    @Slave
    public List<PurContract> listBySupplierId(String supplierId) {
        LambdaQueryWrapper<PurContract> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurContract::getSupplierId, supplierId);
        wrapper.eq(PurContract::getDeleted, false);
        wrapper.orderByDesc(PurContract::getCreateTime);
        return purContractMapper.selectList(wrapper);
    }
}
