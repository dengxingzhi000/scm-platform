package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurRfqItem;
import com.scmcloud.purchase.service.IPurRfqItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-rfq-item")
@Tag(name = "询价明细", description = "询价单明细行的查询与维护接口")
public class PurRfqItemController {

    private final IPurRfqItemService purRfqItemService;

    @Operation(summary = "根据 ID 查询询价明细", description = "按明细主键查询单条询价明细记录。")
    @GetMapping("/{id}")
    public PurRfqItem getById(@Parameter(description = "询价明细主键 ID") @PathVariable String id) {
        return purRfqItemService.getById(id);
    }

    @Operation(summary = "查询询价单下的明细列表", description = "根据询价单 ID 查询该询价单包含的全部明细行。")
    @GetMapping("/list/{rfqId}")
    public List<PurRfqItem> listByRfqId(@Parameter(description = "询价单 ID") @PathVariable String rfqId) {
        return purRfqItemService.listByRfqId(rfqId);
    }

    @Operation(summary = "新增询价明细", description = "创建一条新的询价明细记录。")
    @PostMapping
    public boolean save(@RequestBody PurRfqItem purRfqItem) {
        return purRfqItemService.save(purRfqItem);
    }

    @Operation(summary = "修改询价明细", description = "按主键更新已有的询价明细记录。")
    @PutMapping
    public boolean update(@RequestBody PurRfqItem purRfqItem) {
        return purRfqItemService.updateById(purRfqItem);
    }

    @Operation(summary = "删除询价明细", description = "按明细主键删除单条询价明细记录。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "询价明细主键 ID") @PathVariable String id) {
        return purRfqItemService.removeById(id);
    }

    @Operation(summary = "按询价单删除明细", description = "删除指定询价单下的全部明细行。")
    @DeleteMapping("/rfq/{rfqId}")
    public boolean deleteByRfqId(@Parameter(description = "询价单 ID") @PathVariable String rfqId) {
        return purRfqItemService.deleteByRfqId(rfqId);
    }
}
