package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.purchase.domain.dto.PurRequestConvertDTO;
import com.scmcloud.purchase.domain.entity.PurOrder;
import com.scmcloud.purchase.domain.entity.PurOrderItem;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.domain.entity.PurRequestItem;
import com.scmcloud.purchase.mapper.PurOrderItemMapper;
import com.scmcloud.purchase.mapper.PurOrderMapper;
import com.scmcloud.purchase.mapper.PurRequestItemMapper;
import com.scmcloud.purchase.mapper.PurRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 采购申请写服务：草稿维护、审批流转与「申请转采购单」。
 *
 * <p>转单在同一事务内先以条件更新占用申请单，再写入 {@code pur_order} /
 * {@code pur_order_item}，因此并发重复点击只会成功一次。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurRequestCommandService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_CONVERTED = 4;
    private static final int STATUS_CLOSED = 5;

    private static final int ORDER_STATUS_APPROVED = 2;
    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PurRequestMapper purRequestMapper;
    private final PurRequestItemMapper purRequestItemMapper;
    private final PurOrderMapper purOrderMapper;
    private final PurOrderItemMapper purOrderItemMapper;

    @Master(reason = "创建采购申请")
    @Transactional(rollbackFor = Exception.class)
    public PurRequest createDraft(PurRequest request, List<PurRequestItem> items,
                                  String operatorId, String operatorName) {
        if (request == null) {
            throw badRequest("采购申请内容不能为空");
        }
        List<PurRequestItem> lines = items == null ? List.of() : items;
        validateLines(lines);

        request.setId(UUID.randomUUID().toString());
        request.setTenantId(null);
        if (!StringUtils.hasText(request.getRequestNo())) {
            request.setRequestNo(nextNo("PR"));
        }
        if (request.getRequestType() == null) {
            request.setRequestType(1);
        }
        if (request.getPriority() == null) {
            request.setPriority(2);
        }
        // 申请人只能是当前登录用户，避免客户端伪造申请人。
        request.setRequesterId(operatorId);
        request.setRequesterName(operatorName);
        BigDecimal computedBudget = sumBudget(lines);
        if (computedBudget != null) {
            request.setBudgetAmount(computedBudget);
        }
        request.setStatus(STATUS_DRAFT);
        request.setConverted(false);
        request.setDeleted(false);
        validateHeader(request);

        purRequestMapper.insert(request);
        insertLines(request, lines);
        return request;
    }

    @Master(reason = "更新采购申请")
    @Transactional(rollbackFor = Exception.class)
    public PurRequest updateDraft(String id, PurRequest patch, List<PurRequestItem> items) {
        PurRequest existing = requireRequest(id);
        requireStatus(existing, "仅草稿或已驳回的采购申请可以编辑", STATUS_DRAFT, STATUS_REJECTED);
        if (patch == null) {
            throw badRequest("采购申请内容不能为空");
        }
        List<PurRequestItem> lines = items == null ? List.of() : items;
        validateLines(lines);

        PurRequest merged = patch
                .setId(id)
                .setRequestNo(existing.getRequestNo())
                .setStatus(existing.getStatus())
                .setConverted(existing.getConverted());
        if (merged.getRequestType() == null) {
            merged.setRequestType(existing.getRequestType());
        }
        if (merged.getPriority() == null) {
            merged.setPriority(existing.getPriority());
        }
        if (!StringUtils.hasText(merged.getDeptId())) {
            merged.setDeptId(existing.getDeptId());
        }
        // 申请人不可编辑：始终沿用申请单原值。
        merged.setRequesterId(existing.getRequesterId());
        merged.setRequesterName(existing.getRequesterName());
        if (merged.getExpectedDelivery() == null) {
            merged.setExpectedDelivery(existing.getExpectedDelivery());
        }
        if (!StringUtils.hasText(merged.getPurpose())) {
            merged.setPurpose(existing.getPurpose());
        }
        BigDecimal computedBudget = sumBudget(lines);
        if (computedBudget != null) {
            merged.setBudgetAmount(computedBudget);
        }
        validateHeader(merged);

        int rows = purRequestMapper.update(null, Wrappers.<PurRequest>lambdaUpdate()
                .eq(PurRequest::getId, id)
                .in(PurRequest::getStatus, STATUS_DRAFT, STATUS_REJECTED)
                .set(PurRequest::getRequestType, merged.getRequestType())
                .set(PurRequest::getPriority, merged.getPriority())
                .set(PurRequest::getDeptId, merged.getDeptId())
                .set(PurRequest::getDeptName, merged.getDeptName())
                .set(PurRequest::getRequesterId, merged.getRequesterId())
                .set(PurRequest::getRequesterName, merged.getRequesterName())
                .set(PurRequest::getRequesterPhone, merged.getRequesterPhone())
                .set(PurRequest::getExpectedDelivery, merged.getExpectedDelivery())
                .set(PurRequest::getPurpose, merged.getPurpose())
                .set(PurRequest::getBudgetAmount, merged.getBudgetAmount())
                .set(PurRequest::getAttachments, merged.getAttachments())
                .set(PurRequest::getRemark, merged.getRemark())
                .set(PurRequest::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw conflict("采购申请状态已变化，无法保存");
        }

        purRequestItemMapper.delete(Wrappers.<PurRequestItem>lambdaQuery()
                .eq(PurRequestItem::getRequestId, id));
        insertLines(merged, lines);
        return requireRequest(id);
    }

    @Master(reason = "提交采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurRequest request = requireRequest(id);
        requireStatus(request, "仅草稿或已驳回的采购申请可以提交", STATUS_DRAFT, STATUS_REJECTED);

        Long lineCount = purRequestItemMapper.selectCount(Wrappers.<PurRequestItem>lambdaQuery()
                .eq(PurRequestItem::getRequestId, id));
        if (lineCount == null || lineCount == 0) {
            throw conflict("请先添加至少一条采购明细");
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = purRequestMapper.update(null, Wrappers.<PurRequest>lambdaUpdate()
                .eq(PurRequest::getId, id)
                .in(PurRequest::getStatus, STATUS_DRAFT, STATUS_REJECTED)
                .set(PurRequest::getStatus, STATUS_PENDING)
                .set(PurRequest::getSubmittedAt, now)
                .set(PurRequest::getRejectedAt, null)
                .set(PurRequest::getRejectReason, null)
                .set(PurRequest::getUpdateTime, now));
        if (rows == 0) {
            throw conflict("采购申请状态已变化，无法提交");
        }
        return true;
    }

    @Master(reason = "审批采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurRequest request = requireRequest(id);
        requireStatus(request, "仅待审批的采购申请可以审批通过", STATUS_PENDING);
        if (!StringUtils.hasText(approverId)) {
            throw badRequest("审批人身份缺失");
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = purRequestMapper.update(null, Wrappers.<PurRequest>lambdaUpdate()
                .eq(PurRequest::getId, id)
                .eq(PurRequest::getStatus, STATUS_PENDING)
                .set(PurRequest::getStatus, STATUS_APPROVED)
                .set(PurRequest::getCurrentApproverId, approverId)
                .set(PurRequest::getCurrentApproverName, approverName)
                .set(PurRequest::getApprovedAt, now)
                .set(PurRequest::getUpdateTime, now));
        if (rows == 0) {
            throw conflict("采购申请状态已变化，无法审批");
        }
        return true;
    }

    @Master(reason = "驳回采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(String id, String approverId, String approverName, String reason) {
        PurRequest request = requireRequest(id);
        requireStatus(request, "仅待审批的采购申请可以驳回", STATUS_PENDING);
        if (!StringUtils.hasText(reason)) {
            throw badRequest("请填写驳回原因");
        }
        if (!StringUtils.hasText(approverId)) {
            throw badRequest("审批人身份缺失");
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = purRequestMapper.update(null, Wrappers.<PurRequest>lambdaUpdate()
                .eq(PurRequest::getId, id)
                .eq(PurRequest::getStatus, STATUS_PENDING)
                .set(PurRequest::getStatus, STATUS_REJECTED)
                .set(PurRequest::getCurrentApproverId, approverId)
                .set(PurRequest::getCurrentApproverName, approverName)
                .set(PurRequest::getRejectedAt, now)
                .set(PurRequest::getRejectReason, reason.trim())
                .set(PurRequest::getUpdateTime, now));
        if (rows == 0) {
            throw conflict("采购申请状态已变化，无法驳回");
        }
        return true;
    }

    @Master(reason = "关闭采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRequest request = requireRequest(id);
        if (Objects.equals(request.getStatus(), STATUS_CONVERTED)) {
            throw conflict("已转采购单的申请不能关闭");
        }
        if (Objects.equals(request.getStatus(), STATUS_CLOSED)) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = purRequestMapper.update(null, Wrappers.<PurRequest>lambdaUpdate()
                .eq(PurRequest::getId, id)
                .in(PurRequest::getStatus, STATUS_DRAFT, STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED)
                .set(PurRequest::getStatus, STATUS_CLOSED)
                .set(PurRequest::getUpdateTime, now));
        if (rows == 0) {
            throw conflict("采购申请状态已变化，无法关闭");
        }
        return true;
    }

    @Master(reason = "删除采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        PurRequest request = requireRequest(id);
        requireStatus(request, "仅草稿或已驳回的采购申请可以删除", STATUS_DRAFT, STATUS_REJECTED);

        purRequestItemMapper.delete(Wrappers.<PurRequestItem>lambdaQuery()
                .eq(PurRequestItem::getRequestId, id));
        return purRequestMapper.deleteById(id) > 0;
    }

    @Master(reason = "采购申请转采购单")
    @Transactional(rollbackFor = Exception.class)
    public PurOrder convert(String id, PurRequestConvertDTO dto, String operatorId, String operatorName) {
        if (dto == null) {
            throw badRequest("请填写采购单信息");
        }
        if (!StringUtils.hasText(dto.supplierId())) {
            throw badRequest("请选择供应商");
        }
        if (!StringUtils.hasText(dto.warehouseId())) {
            throw badRequest("请选择收货仓库");
        }
        if (dto.expectedDelivery() == null) {
            throw badRequest("请填写期望到货时间");
        }

        PurRequest request = requireRequest(id);
        if (!Objects.equals(request.getStatus(), STATUS_APPROVED) || Boolean.TRUE.equals(request.getConverted())) {
            throw conflict("仅已审批且未转单的采购申请可以生成采购单");
        }

        List<PurRequestItem> lines = purRequestItemMapper.selectList(Wrappers.<PurRequestItem>lambdaQuery()
                .eq(PurRequestItem::getRequestId, id)
                .orderByAsc(PurRequestItem::getCreateTime));
        if (lines.isEmpty()) {
            throw conflict("采购申请没有明细，无法生成采购单");
        }

        Map<String, BigDecimal> priceOverrides = new HashMap<>();
        if (dto.itemPrices() != null) {
            for (PurRequestConvertDTO.ItemPrice price : dto.itemPrices()) {
                if (price != null && StringUtils.hasText(price.requestItemId()) && price.unitPrice() != null) {
                    priceOverrides.put(price.requestItemId(), price.unitPrice());
                }
            }
        }

        BigDecimal taxRate = resolveTaxRate(dto.taxRate());
        BigDecimal freight = nonNegative(dto.freightAmount(), "运费");
        BigDecimal discount = nonNegative(dto.discountAmount(), "折扣");

        String orderId = UUID.randomUUID().toString();
        String orderNo = nextNo("PO");
        String supplierName = StringUtils.hasText(dto.supplierName()) ? dto.supplierName() : dto.supplierId();
        LocalDateTime now = LocalDateTime.now();

        // 先占用申请单：并发转单时只有第一个条件更新能命中。
        int claimed = purRequestMapper.update(null, Wrappers.<PurRequest>lambdaUpdate()
                .eq(PurRequest::getId, id)
                .eq(PurRequest::getStatus, STATUS_APPROVED)
                .eq(PurRequest::getConverted, false)
                .set(PurRequest::getStatus, STATUS_CONVERTED)
                .set(PurRequest::getConverted, true)
                .set(PurRequest::getPurchaseOrderId, orderId)
                .set(PurRequest::getPurchaseOrderNo, orderNo)
                .set(PurRequest::getUpdateTime, now));
        if (claimed == 0) {
            throw conflict("采购申请已被转单或状态已变化");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<PurOrderItem> orderItems = new ArrayList<>(lines.size());
        int index = 0;
        for (PurRequestItem line : lines) {
            index++;
            BigDecimal unitPrice = priceOverrides.get(line.getId());
            if (unitPrice == null) {
                unitPrice = line.getBudgetPrice();
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw badRequest("明细 [" + line.getMaterialName() + "] 缺少单价");
            }
            BigDecimal quantity = line.getQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("明细 [" + line.getMaterialName() + "] 的数量必须大于 0");
            }

            BigDecimal subtotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            String materialCode = StringUtils.hasText(line.getMaterialCode())
                    ? line.getMaterialCode()
                    : "REQ-" + request.getRequestNo() + "-" + index;

            orderItems.add(new PurOrderItem()
                    .setId(UUID.randomUUID().toString())
                    .setOrderId(orderId)
                    .setOrderNo(orderNo)
                    .setMaterialCode(materialCode)
                    .setMaterialName(line.getMaterialName())
                    .setMaterialSpec(line.getMaterialSpec())
                    .setMaterialCategory(line.getMaterialCategory())
                    .setQuantity(quantity)
                    .setUnit(StringUtils.hasText(line.getUnit()) ? line.getUnit() : "件")
                    .setReceivedQuantity(BigDecimal.ZERO)
                    .setQualifiedQuantity(BigDecimal.ZERO)
                    .setUnitPrice(unitPrice)
                    .setTaxRate(taxRate)
                    .setDiscountRate(BigDecimal.ZERO)
                    .setSubtotal(subtotal)
                    .setDeliveryDate(dto.expectedDelivery().toLocalDate())
                    .setQualityRequirement(line.getQualityStandard())
                    .setItemRemark(line.getRemark())
                    .setCreateTime(now));
        }

        BigDecimal payable = total.add(freight).subtract(discount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("应付金额不能为负数");
        }
        BigDecimal taxAmount = total.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        String approvedBy = StringUtils.hasText(request.getCurrentApproverId())
                ? request.getCurrentApproverId() : operatorId;
        String approvedByName = StringUtils.hasText(request.getCurrentApproverName())
                ? request.getCurrentApproverName() : operatorName;
        LocalDateTime approvedAt = request.getApprovedAt() != null ? request.getApprovedAt() : now;

        PurOrder order = new PurOrder()
                .setId(orderId)
                .setOrderNo(orderNo)
                .setRequestId(request.getId())
                .setRequestNo(request.getRequestNo())
                .setSupplierId(dto.supplierId())
                .setSupplierName(supplierName)
                .setSupplierContact(dto.supplierContact())
                .setOrderType(resolveOrderType(dto.orderType(), request.getRequestType()))
                .setTotalAmount(total)
                .setTaxAmount(taxAmount)
                .setFreightAmount(freight)
                .setDiscountAmount(discount)
                .setPayableAmount(payable)
                .setStatus(ORDER_STATUS_APPROVED)
                .setWarehouseId(dto.warehouseId())
                .setWarehouseName(dto.warehouseName())
                .setWarehouseAddress(dto.warehouseAddress())
                .setOrderDate(LocalDate.now())
                .setExpectedDelivery(dto.expectedDelivery())
                .setBuyerId(operatorId)
                .setBuyerName(operatorName)
                .setApprovedBy(approvedBy)
                .setApprovedByName(approvedByName)
                .setApprovedAt(approvedAt)
                .setDeleted(false)
                .setRemark(StringUtils.hasText(dto.remark())
                        ? dto.remark()
                        : "由采购申请 " + request.getRequestNo() + " 转入");
        purOrderMapper.insert(order);
        for (PurOrderItem item : orderItems) {
            purOrderItemMapper.insert(item);
        }

        log.info("Purchase request {} converted to order {}", request.getRequestNo(), orderNo);
        return order;
    }

    private PurRequest requireRequest(String id) {
        if (!StringUtils.hasText(id)) {
            throw badRequest("采购申请 ID 不能为空");
        }
        PurRequest request = purRequestMapper.selectById(id);
        if (request == null || Boolean.TRUE.equals(request.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "采购申请不存在: " + id);
        }
        return request;
    }

    private void requireStatus(PurRequest request, String message, int... allowed) {
        for (int status : allowed) {
            if (Objects.equals(request.getStatus(), status)) {
                return;
            }
        }
        throw conflict(message);
    }

    private void validateHeader(PurRequest request) {
        if (request.getRequestType() == null || request.getRequestType() < 1 || request.getRequestType() > 4) {
            throw badRequest("申请类型必须是 1-4");
        }
        if (request.getPriority() == null || request.getPriority() < 1 || request.getPriority() > 3) {
            throw badRequest("优先级必须是 1-3");
        }
        if (!StringUtils.hasText(request.getDeptId())) {
            throw badRequest("请选择申请部门");
        }
        if (!StringUtils.hasText(request.getRequesterId())) {
            throw badRequest("申请人不能为空");
        }
        if (request.getExpectedDelivery() == null) {
            throw badRequest("请填写期望到货时间");
        }
        if (!StringUtils.hasText(request.getPurpose())) {
            throw badRequest("请填写采购用途");
        }
    }

    private void validateLines(List<PurRequestItem> lines) {
        for (PurRequestItem line : lines) {
            if (!StringUtils.hasText(line.getMaterialName())) {
                throw badRequest("明细的物料名称不能为空");
            }
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("明细 [" + line.getMaterialName() + "] 的数量必须大于 0");
            }
            if (line.getBudgetPrice() != null && line.getBudgetPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw badRequest("明细 [" + line.getMaterialName() + "] 的预算单价不能为负数");
            }
        }
    }

    private void insertLines(PurRequest request, List<PurRequestItem> lines) {
        int index = 0;
        for (PurRequestItem line : lines) {
            index++;
            line.setId(UUID.randomUUID().toString());
            line.setTenantId(null);
            line.setRequestId(request.getId());
            line.setRequestNo(request.getRequestNo());
            if (!StringUtils.hasText(line.getMaterialCode())) {
                line.setMaterialCode("REQ-" + request.getRequestNo() + "-" + index);
            }
            if (!StringUtils.hasText(line.getUnit())) {
                line.setUnit("件");
            }
            line.setCreateTime(LocalDateTime.now());
            purRequestItemMapper.insert(line);
        }
    }

    private BigDecimal sumBudget(List<PurRequestItem> lines) {
        BigDecimal total = null;
        for (PurRequestItem line : lines) {
            if (line.getQuantity() == null || line.getBudgetPrice() == null) {
                continue;
            }
            BigDecimal subtotal = line.getQuantity().multiply(line.getBudgetPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            total = total == null ? subtotal : total.add(subtotal);
        }
        return total;
    }

    private BigDecimal resolveTaxRate(BigDecimal taxRate) {
        if (taxRate == null) {
            return BigDecimal.ZERO;
        }
        if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw badRequest("税率必须在 0 到 1 之间");
        }
        return taxRate;
    }

    private BigDecimal nonNegative(BigDecimal value, String label) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest(label + "不能为负数");
        }
        return amount;
    }

    private int resolveOrderType(Integer requested, Integer requestType) {
        if (requested != null && requested >= 1 && requested <= 4) {
            return requested;
        }
        if (requestType != null && requestType >= 1 && requestType <= 4) {
            return requestType;
        }
        return 1;
    }

    private String nextNo(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(NO_FORMAT) + "-"
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(400, HttpStatus.BAD_REQUEST, message);
    }

    private ServiceException conflict(String message) {
        return new ServiceException(409, HttpStatus.CONFLICT, message);
    }
}
