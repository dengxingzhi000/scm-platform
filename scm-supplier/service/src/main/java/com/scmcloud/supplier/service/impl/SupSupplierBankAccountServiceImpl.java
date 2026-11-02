package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierBankAccount;
import com.scmcloud.supplier.mapper.SupSupplierBankAccountMapper;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import com.scmcloud.supplier.service.ISupSupplierBankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierBankAccountServiceImpl
        extends ServiceImpl<SupSupplierBankAccountMapper, SupSupplierBankAccount>
        implements ISupSupplierBankAccountService {

    private final SupSupplierMapper supplierMapper;

    @Override
    public List<SupSupplierBankAccount> listBySupplierId(String supplierId) {
        requireSupplier(supplierId);
        return lambdaQuery()
                .eq(SupSupplierBankAccount::getSupplierId, supplierId)
                .orderByDesc(SupSupplierBankAccount::getIsDefault)
                .orderByAsc(SupSupplierBankAccount::getSortOrder)
                .orderByAsc(SupSupplierBankAccount::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierBankAccount create(String supplierId, SupSupplierBankAccount account) {
        requireSupplier(supplierId);
        validate(account, true);

        boolean firstAccount = lambdaQuery()
                .eq(SupSupplierBankAccount::getSupplierId, supplierId)
                .count() == 0;
        if (account.getIsDefault() == null) {
            account.setIsDefault(firstAccount);
        }
        if (account.getEnabled() == null) {
            account.setEnabled(true);
        }
        if (account.getSortOrder() == null) {
            account.setSortOrder(0);
        }
        if (!StringUtils.hasText(account.getCurrency())) {
            account.setCurrency("CNY");
        }
        account.setCurrency(account.getCurrency().toUpperCase());

        account.setId(UUID.randomUUID().toString());
        account.setSupplierId(supplierId);
        account.setTenantId(null);
        account.setCreateBy(null);
        account.setUpdateBy(null);
        account.setDeleted(false);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            clearOtherDefault(supplierId, account.getId());
        }
        save(account);
        syncDefaultAccount(supplierId);
        log.info("新增供应商银行账户 supplierId={}, accountId={}", supplierId, account.getId());
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierBankAccount update(String id, SupSupplierBankAccount account) {
        SupSupplierBankAccount existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "银行账户不存在");
        }
        validate(account, false);
        if (StringUtils.hasText(account.getCurrency())) {
            account.setCurrency(account.getCurrency().toUpperCase());
        }
        account.setId(id);
        account.setSupplierId(existing.getSupplierId());
        account.setTenantId(null);
        account.setCreateBy(null);
        account.setCreateTime(null);
        account.setUpdateBy(null);
        account.setDeleted(null);
        account.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            clearOtherDefault(existing.getSupplierId(), id);
        }
        updateById(account);
        syncDefaultAccount(existing.getSupplierId());
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        SupSupplierBankAccount existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        removeById(id);
        syncDefaultAccount(existing.getSupplierId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(String id) {
        SupSupplierBankAccount existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "银行账户不存在");
        }
        clearOtherDefault(existing.getSupplierId(), id);
        SupSupplierBankAccount update = new SupSupplierBankAccount()
                .setId(id)
                .setIsDefault(true)
                .setEnabled(true)
                .setUpdateTime(LocalDateTime.now());
        updateById(update);
        syncDefaultAccount(existing.getSupplierId());
        return true;
    }

    private void syncDefaultAccount(String supplierId) {
        SupSupplierBankAccount defaultAccount = lambdaQuery()
                .eq(SupSupplierBankAccount::getSupplierId, supplierId)
                .eq(SupSupplierBankAccount::getIsDefault, true)
                .one();

        if (defaultAccount == null) {
            SupSupplierBankAccount candidate = lambdaQuery()
                    .eq(SupSupplierBankAccount::getSupplierId, supplierId)
                    .eq(SupSupplierBankAccount::getEnabled, true)
                    .orderByAsc(SupSupplierBankAccount::getSortOrder)
                    .orderByAsc(SupSupplierBankAccount::getCreateTime)
                    .last("LIMIT 1")
                    .one();
            if (candidate != null) {
                candidate.setIsDefault(true);
                candidate.setUpdateTime(LocalDateTime.now());
                updateById(candidate);
                defaultAccount = candidate;
            }
        }

        LambdaUpdateWrapper<SupSupplier> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SupSupplier::getId, supplierId);
        wrapper.set(SupSupplier::getBankName, defaultAccount == null ? null : defaultAccount.getBankName());
        wrapper.set(SupSupplier::getBankAccount, defaultAccount == null ? null : defaultAccount.getBankAccount());
        wrapper.set(SupSupplier::getBankAccountName, defaultAccount == null ? null : defaultAccount.getAccountName());
        wrapper.set(SupSupplier::getUpdateTime, LocalDateTime.now());
        supplierMapper.update(null, wrapper);
    }

    private void clearOtherDefault(String supplierId, String accountId) {
        lambdaUpdate()
                .eq(SupSupplierBankAccount::getSupplierId, supplierId)
                .eq(SupSupplierBankAccount::getIsDefault, true)
                .ne(SupSupplierBankAccount::getId, accountId)
                .set(SupSupplierBankAccount::getIsDefault, false)
                .set(SupSupplierBankAccount::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private void validate(SupSupplierBankAccount account, boolean requireRequired) {
        if (requireRequired) {
            if (!StringUtils.hasText(account.getBankName())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "开户银行不能为空");
            }
            if (!StringUtils.hasText(account.getBankAccount())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "银行账号不能为空");
            }
            if (!StringUtils.hasText(account.getAccountName())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "账户名称不能为空");
            }
        }
        if (StringUtils.hasText(account.getCurrency()) && account.getCurrency().length() != 3) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "币种必须为 3 位 ISO 代码");
        }
        if (StringUtils.hasText(account.getBankAccount()) && account.getBankAccount().length() > 64) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "银行账号长度不能超过 64");
        }
    }

    private SupSupplier requireSupplier(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商ID不能为空");
        }
        SupSupplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || Boolean.TRUE.equals(supplier.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "供应商不存在");
        }
        return supplier;
    }
}
