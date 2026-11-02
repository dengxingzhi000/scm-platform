package com.scmcloud.purchase.controller;

import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.domain.entity.PurRequestItem;
import com.scmcloud.purchase.service.IPurRequestItemService;
import com.scmcloud.purchase.service.IPurRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-request-item")
@Tag(name = "采购申请明细", description = "采购申请单明细行的查询与维护接口")
public class PurRequestItemController {

    private final IPurRequestItemService purRequestItemService;
    private final IPurRequestService purRequestService;

    @Operation(summary = "根据 ID 查询申请明细", description = "按明细主键查询单条采购申请明细记录。")
    @GetMapping("/{id}")
    public PurRequestItem getById(@Parameter(description = "申请明细主键 ID") @PathVariable String id) {
        return purRequestItemService.getById(id);
    }

    @Operation(summary = "查询申请单下的明细列表", description = "根据采购申请单 ID 查询该申请单包含的全部明细行。")
    @GetMapping("/list/{requestId}")
    public List<PurRequestItem> listByRequestId(@Parameter(description = "采购申请单 ID") @PathVariable String requestId) {
        return purRequestItemService.listByRequestId(requestId);
    }

    @Operation(summary = "新增申请明细", description = "向草稿或已驳回的采购申请追加一条明细行。")
    @PostMapping
    public boolean save(@RequestBody PurRequestItem purRequestItem) {
        if (purRequestItem == null) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "明细内容不能为空");
        }
        purRequestItem.setTenantId(null);
        requireEditable(purRequestItem.getRequestId());
        return purRequestItemService.save(purRequestItem);
    }

    @Operation(summary = "修改申请明细", description = "仅草稿或已驳回的采购申请可以修改明细。")
    @PutMapping
    public boolean update(@RequestBody PurRequestItem purRequestItem) {
        if (purRequestItem == null) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "明细内容不能为空");
        }
        PurRequestItem existing = requireItem(purRequestItem.getId());
        requireEditable(existing.getRequestId());
        purRequestItem.setTenantId(null);
        purRequestItem.setRequestId(existing.getRequestId());
        purRequestItem.setRequestNo(existing.getRequestNo());
        return purRequestItemService.updateById(purRequestItem);
    }

    @Operation(summary = "删除申请明细", description = "仅草稿或已驳回的采购申请可以删除明细。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "申请明细主键 ID") @PathVariable String id) {
        PurRequestItem existing = requireItem(id);
        requireEditable(existing.getRequestId());
        return purRequestItemService.removeById(id);
    }

    @Operation(summary = "按申请单删除明细", description = "仅草稿或已驳回的采购申请可以清空明细。")
    @DeleteMapping("/request/{requestId}")
    public boolean deleteByRequestId(@Parameter(description = "采购申请单 ID") @PathVariable String requestId) {
        requireEditable(requestId);
        return purRequestItemService.deleteByRequestId(requestId);
    }

    private PurRequestItem requireItem(String id) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "明细 ID 不能为空");
        }
        PurRequestItem item = purRequestItemService.getById(id);
        if (item == null) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "采购申请明细不存在: " + id);
        }
        return item;
    }

    private void requireEditable(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "采购申请 ID 不能为空");
        }
        PurRequest request = purRequestService.getById(requestId);
        if (request == null || Boolean.TRUE.equals(request.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "采购申请不存在: " + requestId);
        }
        Integer status = request.getStatus();
        if (status == null || (status != 0 && status != 3)) {
            throw new ServiceException(409, HttpStatus.CONFLICT, "仅草稿或已驳回的采购申请可以维护明细");
        }
    }
}
