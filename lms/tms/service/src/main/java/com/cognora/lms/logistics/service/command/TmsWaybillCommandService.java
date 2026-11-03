package com.cognora.lms.logistics.service.command;

import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.common.status.StatusValidator;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsWaybillCommandService {

    private final TmsWaybillMapper tmsWaybillMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "Save waybill")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsWaybill entity) {
        return tmsWaybillMapper.insert(entity) > 0;
    }

    @Master(reason = "Update waybill")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsWaybill entity) {
        return tmsWaybillMapper.updateById(entity) > 0;
    }

    @Master(reason = "Delete waybill")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsWaybillMapper.deleteById(id) > 0;
    }

    @Master(reason = "Create waybill")
    @Transactional(rollbackFor = Exception.class)
    public TmsWaybill createWaybill(TmsWaybill waybill) {
        log.info("Create waybill: orderNo={}, carrierId={}", waybill.getOrderNo(), waybill.getCarrierId());
        if (waybill.getId() == null) {
            waybill.setId(java.util.UUID.randomUUID().toString());
        }
        if (waybill.getWaybillNo() == null) {
            waybill.setWaybillNo(generateWaybillNo());
        }
        waybill.setStatus(0);
        waybill.setDeleted(false);
        waybill.setCreateTime(LocalDateTime.now());
        waybill.setUpdateTime(LocalDateTime.now());
        boolean success = tmsWaybillMapper.insert(waybill) > 0;
        if (!success) {
            throw new RuntimeException("Failed to create waybill");
        }
        log.info("Waybill created: id={}, waybillNo={}", waybill.getId(), waybill.getWaybillNo());
        return waybill;
    }

    @Master(reason = "Update waybill status")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String waybillId, Integer status, String operator) {
        log.info("Update waybill status: waybillId={}, status={}, operator={}", waybillId, status, operator);
        TmsWaybill waybill = tmsWaybillMapper.selectById(waybillId);
        if (waybill == null) {
            log.warn("Waybill not found: waybillId={}", waybillId);
            return false;
        }
        waybill.setStatus(status);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());
        if (status == 4) {
            waybill.setActualDelivery(LocalDateTime.now());
        }
        boolean success = tmsWaybillMapper.updateById(waybill) > 0;
        if (success) {
            log.info("Waybill status updated: waybillNo={}, status={}", waybill.getWaybillNo(), status);
        }
        return success;
    }

    @Master(reason = "Cancel waybill")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWaybill(String waybillId, String reason, String operator) {
        log.info("Cancel waybill: waybillId={}, reason={}, operator={}", waybillId, reason, operator);
        TmsWaybill waybill = tmsWaybillMapper.selectById(waybillId);
        if (waybill == null) {
            log.warn("Waybill not found: waybillId={}", waybillId);
            return false;
        }
        String cancelFromStatus;
        if (waybill.getStatus() == 0) {
            cancelFromStatus = "CREATED";
        } else if (waybill.getStatus() == 1) {
            cancelFromStatus = "PENDING";
        } else if (waybill.getStatus() == 2) {
            cancelFromStatus = "IN_TRANSIT";
        } else {
            cancelFromStatus = "DELIVERED";
        }
        statusValidator.validateTransition("LOGISTICS", cancelFromStatus, "CANCELLED");
        waybill.setStatus(6);
        waybill.setExceptionType("CANCEL");
        waybill.setExceptionReason(reason);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());
        boolean success = tmsWaybillMapper.updateById(waybill) > 0;
        if (success) {
            log.info("Waybill cancelled: waybillNo={}", waybill.getWaybillNo());
        }
        return success;
    }

    private String generateWaybillNo() {
        return "WB" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}