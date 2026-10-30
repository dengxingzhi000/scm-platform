package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierCertificate;
import com.scmcloud.supplier.mapper.SupSupplierCertificateMapper;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import com.scmcloud.supplier.service.ISupSupplierCertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierCertificateServiceImpl
        extends ServiceImpl<SupSupplierCertificateMapper, SupSupplierCertificate>
        implements ISupSupplierCertificateService {

    /** 到期预警窗口（天）。 */
    public static final int EXPIRY_WARNING_DAYS = 30;

    private final SupSupplierMapper supplierMapper;

    @Override
    public List<SupSupplierCertificate> listBySupplierId(String supplierId) {
        requireSupplier(supplierId);
        List<SupSupplierCertificate> certificates = lambdaQuery()
                .eq(SupSupplierCertificate::getSupplierId, supplierId)
                .orderByAsc(SupSupplierCertificate::getValidTo)
                .orderByDesc(SupSupplierCertificate::getCreateTime)
                .list();
        certificates.forEach(this::fillDerivedFields);
        return certificates;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierCertificate create(String supplierId, SupSupplierCertificate certificate) {
        requireSupplier(supplierId);
        validate(certificate);
        certificate.setId(UUID.randomUUID().toString());
        certificate.setSupplierId(supplierId);
        certificate.setTenantId(null);
        certificate.setCreateBy(null);
        certificate.setUpdateBy(null);
        if (certificate.getStatus() == null) {
            certificate.setStatus(resolveStatus(certificate.getValidTo()));
        }
        certificate.setDeleted(false);
        certificate.setCreateTime(LocalDateTime.now());
        certificate.setUpdateTime(LocalDateTime.now());
        save(certificate);
        log.info("新增供应商资质 supplierId={}, certificateId={}", supplierId, certificate.getId());
        return certificate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierCertificate update(String id, SupSupplierCertificate certificate) {
        SupSupplierCertificate existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "资质证照不存在");
        }
        validate(certificate);
        certificate.setId(id);
        certificate.setSupplierId(existing.getSupplierId());
        certificate.setTenantId(null);
        certificate.setCreateBy(null);
        certificate.setCreateTime(null);
        certificate.setUpdateBy(null);
        certificate.setDeleted(null);
        certificate.setUpdateTime(LocalDateTime.now());
        if (certificate.getStatus() == null && certificate.getValidTo() != null) {
            certificate.setStatus(resolveStatus(certificate.getValidTo()));
        }
        updateById(certificate);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        SupSupplierCertificate existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        removeById(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verify(String id, boolean approved, String operatorId, String operatorName, String remark) {
        SupSupplierCertificate certificate = getById(id);
        if (certificate == null || Boolean.TRUE.equals(certificate.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "资质证照不存在");
        }
        certificate.setStatus(approved ? resolveStatus(certificate.getValidTo()) : 4);
        certificate.setVerifiedBy(operatorId);
        certificate.setVerifiedAt(LocalDateTime.now());
        if (StringUtils.hasText(remark)) {
            certificate.setRemark(remark);
        }
        certificate.setUpdateTime(LocalDateTime.now());
        updateById(certificate);
    }

    @Override
    public Page<SupSupplierCertificate> pageExpiring(int page, int size, int days) {
        LocalDate today = LocalDate.now();
        int boundedDays = Math.max(0, Math.min(days, 365));
        Page<SupSupplierCertificate> result = lambdaQuery()
                .ge(SupSupplierCertificate::getValidTo, today)
                .le(SupSupplierCertificate::getValidTo, today.plusDays(boundedDays))
                .ne(SupSupplierCertificate::getStatus, 4)
                .orderByAsc(SupSupplierCertificate::getValidTo)
                .page(new Page<>(Math.max(page, 1), Math.max(size, 1)));

        List<SupSupplierCertificate> records = result.getRecords();
        records.forEach(this::fillDerivedFields);
        Set<String> supplierIds = records.stream()
                .map(SupSupplierCertificate::getSupplierId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!supplierIds.isEmpty()) {
            Map<String, SupSupplier> suppliers = supplierMapper.selectBatchIds(supplierIds).stream()
                    .collect(Collectors.toMap(SupSupplier::getId, Function.identity()));
            records.forEach(certificate -> {
                SupSupplier supplier = suppliers.get(certificate.getSupplierId());
                if (supplier != null) {
                    certificate.setSupplierName(supplier.getSupplierName());
                }
            });
        }
        return result;
    }

    @Override
    public long countExpiring(int days) {
        LocalDate today = LocalDate.now();
        int boundedDays = Math.max(0, Math.min(days, 365));
        return lambdaQuery()
                .ge(SupSupplierCertificate::getValidTo, today)
                .le(SupSupplierCertificate::getValidTo, today.plusDays(boundedDays))
                .ne(SupSupplierCertificate::getStatus, 4)
                .count();
    }

    private void fillDerivedFields(SupSupplierCertificate certificate) {
        if (certificate.getValidTo() == null) {
            certificate.setDaysToExpiry(null);
            return;
        }
        certificate.setDaysToExpiry((int) ChronoUnit.DAYS.between(LocalDate.now(), certificate.getValidTo()));
        Integer status = certificate.getStatus();
        // 待审核(0)与人工驳回(4)保持原状态，其余状态按有效期实时刷新
        if (status != null && status != 0 && status != 4) {
            certificate.setStatus(resolveStatus(certificate.getValidTo()));
        }
    }

    private int resolveStatus(LocalDate validTo) {
        if (validTo == null) {
            return 1;
        }
        LocalDate today = LocalDate.now();
        if (validTo.isBefore(today)) {
            return 3;
        }
        if (!validTo.isAfter(today.plusDays(EXPIRY_WARNING_DAYS))) {
            return 2;
        }
        return 1;
    }

    private void validate(SupSupplierCertificate certificate) {
        if (!StringUtils.hasText(certificate.getCertificateType())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "证照类型不能为空");
        }
        if (!StringUtils.hasText(certificate.getCertificateName())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "证照名称不能为空");
        }
        if (certificate.getValidFrom() != null && certificate.getValidTo() != null
                && certificate.getValidTo().isBefore(certificate.getValidFrom())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "证照有效期结束日期不能早于开始日期");
        }
        if (certificate.getStatus() != null && (certificate.getStatus() < 0 || certificate.getStatus() > 4)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "证照状态不合法");
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
