package scm.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scm.purchase.domain.entity.PurContract;
import scm.purchase.mapper.PurContractMapper;
import scm.purchase.service.IPurContractService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurContractServiceImpl extends ServiceImpl<PurContractMapper, PurContract> implements IPurContractService {

    @Override
    public PurContract getByContractNo(String contractNo) {
        return lambdaQuery()
                .eq(PurContract::getContractNo, contractNo)
                .eq(PurContract::getDeleted, false)
                .one();
    }

    @Override
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
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurContract> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurContract::getStatus, status)
                .eq(PurContract::getDeleted, false)
                .orderByDesc(PurContract::getCreateTime)
                .list();
    }

    @Override
    public List<PurContract> listBySupplierId(String supplierId) {
        return lambdaQuery()
                .eq(PurContract::getSupplierId, supplierId)
                .eq(PurContract::getDeleted, false)
                .orderByDesc(PurContract::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sign(String id, String signedBy, String signedByName) {
        PurContract contract = getById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("合同不存在: " + id);
        }
        if (contract.getStatus() != 1) {
            throw new IllegalStateException("只有待签署的合同才能签署");
        }
        contract.setStatus(2);
        contract.setSignedBy(signedBy);
        contract.setSignedByName(signedByName);
        contract.setSignedAt(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        return updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminate(String id) {
        PurContract contract = getById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("合同不存在: " + id);
        }
        if (contract.getStatus() == 4) {
            throw new IllegalStateException("合同已终止");
        }
        if (contract.getStatus() == 0) {
            throw new IllegalStateException("草稿合同不能终止，请直接删除");
        }
        contract.setStatus(4);
        contract.setUpdateTime(LocalDateTime.now());
        return updateById(contract);
    }
}
