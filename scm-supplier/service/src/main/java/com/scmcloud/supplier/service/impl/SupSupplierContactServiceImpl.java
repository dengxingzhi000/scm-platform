package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierContact;
import com.scmcloud.supplier.mapper.SupSupplierContactMapper;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import com.scmcloud.supplier.service.ISupSupplierContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierContactServiceImpl
        extends ServiceImpl<SupSupplierContactMapper, SupSupplierContact>
        implements ISupSupplierContactService {

    private static final Set<String> ROLES = Set.of("SALES", "FINANCE", "LOGISTICS", "TECH", "OTHER");

    private final SupSupplierMapper supplierMapper;

    @Override
    public List<SupSupplierContact> listBySupplierId(String supplierId) {
        requireSupplier(supplierId);
        return lambdaQuery()
                .eq(SupSupplierContact::getSupplierId, supplierId)
                .orderByDesc(SupSupplierContact::getIsPrimary)
                .orderByAsc(SupSupplierContact::getSortOrder)
                .orderByAsc(SupSupplierContact::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierContact create(String supplierId, SupSupplierContact contact) {
        requireSupplier(supplierId);
        validate(contact, true);

        boolean firstContact = lambdaQuery()
                .eq(SupSupplierContact::getSupplierId, supplierId)
                .count() == 0;
        if (contact.getIsPrimary() == null) {
            contact.setIsPrimary(firstContact);
        }
        if (contact.getEnabled() == null) {
            contact.setEnabled(true);
        }
        if (contact.getSortOrder() == null) {
            contact.setSortOrder(0);
        }
        if (!StringUtils.hasText(contact.getContactRole())) {
            contact.setContactRole("OTHER");
        }

        contact.setId(UUID.randomUUID().toString());
        contact.setSupplierId(supplierId);
        contact.setTenantId(null);
        contact.setCreateBy(null);
        contact.setUpdateBy(null);
        contact.setDeleted(false);
        contact.setCreateTime(LocalDateTime.now());
        contact.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(contact.getIsPrimary())) {
            clearOtherPrimary(supplierId, contact.getId());
        }
        save(contact);
        syncPrimaryContact(supplierId);
        log.info("新增供应商联系人 supplierId={}, contactId={}", supplierId, contact.getId());
        return contact;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierContact update(String id, SupSupplierContact contact) {
        SupSupplierContact existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "联系人不存在");
        }
        validate(contact, false);
        contact.setId(id);
        contact.setSupplierId(existing.getSupplierId());
        contact.setTenantId(null);
        contact.setCreateBy(null);
        contact.setCreateTime(null);
        contact.setUpdateBy(null);
        contact.setDeleted(null);
        contact.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(contact.getIsPrimary())) {
            clearOtherPrimary(existing.getSupplierId(), id);
        }
        updateById(contact);
        syncPrimaryContact(existing.getSupplierId());
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        SupSupplierContact existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        removeById(id);
        syncPrimaryContact(existing.getSupplierId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setPrimary(String id) {
        SupSupplierContact existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "联系人不存在");
        }
        clearOtherPrimary(existing.getSupplierId(), id);
        SupSupplierContact update = new SupSupplierContact()
                .setId(id)
                .setIsPrimary(true)
                .setEnabled(true)
                .setUpdateTime(LocalDateTime.now());
        updateById(update);
        syncPrimaryContact(existing.getSupplierId());
        return true;
    }

    private void syncPrimaryContact(String supplierId) {
        SupSupplierContact primary = lambdaQuery()
                .eq(SupSupplierContact::getSupplierId, supplierId)
                .eq(SupSupplierContact::getIsPrimary, true)
                .one();

        if (primary == null) {
            SupSupplierContact candidate = lambdaQuery()
                    .eq(SupSupplierContact::getSupplierId, supplierId)
                    .eq(SupSupplierContact::getEnabled, true)
                    .orderByAsc(SupSupplierContact::getSortOrder)
                    .orderByAsc(SupSupplierContact::getCreateTime)
                    .last("LIMIT 1")
                    .one();
            if (candidate != null) {
                candidate.setIsPrimary(true);
                candidate.setUpdateTime(LocalDateTime.now());
                updateById(candidate);
                primary = candidate;
            }
        }

        LambdaUpdateWrapper<SupSupplier> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SupSupplier::getId, supplierId);
        wrapper.set(SupSupplier::getContactName, primary == null ? null : primary.getContactName());
        wrapper.set(SupSupplier::getContactPhone, primary == null ? null : primary.getContactPhone());
        wrapper.set(SupSupplier::getContactEmail, primary == null ? null : primary.getContactEmail());
        wrapper.set(SupSupplier::getContactAddress, primary == null ? null : primary.getContactAddress());
        wrapper.set(SupSupplier::getUpdateTime, LocalDateTime.now());
        supplierMapper.update(null, wrapper);
    }

    private void clearOtherPrimary(String supplierId, String contactId) {
        lambdaUpdate()
                .eq(SupSupplierContact::getSupplierId, supplierId)
                .eq(SupSupplierContact::getIsPrimary, true)
                .ne(SupSupplierContact::getId, contactId)
                .set(SupSupplierContact::getIsPrimary, false)
                .set(SupSupplierContact::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private void validate(SupSupplierContact contact, boolean requireName) {
        if (requireName && !StringUtils.hasText(contact.getContactName())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "联系人姓名不能为空");
        }
        if (StringUtils.hasText(contact.getContactRole()) && !ROLES.contains(contact.getContactRole())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "联系人角色仅支持 SALES/FINANCE/LOGISTICS/TECH/OTHER");
        }
        if (StringUtils.hasText(contact.getContactEmail()) && !contact.getContactEmail().contains("@")) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "联系人邮箱格式不正确");
        }
        if (StringUtils.hasText(contact.getContactPhone()) && contact.getContactPhone().length() > 32) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "联系人电话长度不能超过 32");
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
