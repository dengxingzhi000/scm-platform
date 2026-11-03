package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurRfq;
import com.scmcloud.purchase.service.IPurRfqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-rfq")
@Tag(name = "询价管理", description = "采购询价单的查询、发布与关闭接口")
public class PurRfqController {

    private final IPurRfqService purRfqService;

    @Operation(summary = "根据 ID 查询询价单", description = "按主键查询单张采购询价单的完整信息。")
    @GetMapping("/{id}")
    public PurRfq getById(@Parameter(description = "询价单主键 ID") @PathVariable String id) {
        return purRfqService.getById(id);
    }

    @Operation(summary = "根据询价单号查询", description = "按业务单号查询采购询价单，用于单据核对与追溯。")
    @GetMapping("/no/{rfqNo}")
    public PurRfq getByRfqNo(@Parameter(description = "询价单业务单号") @PathVariable String rfqNo) {
        return purRfqService.getByRfqNo(rfqNo);
    }

    @Operation(summary = "分页查询询价单", description = "按状态、询价类型和关键字分页查询采购询价单。")
    @GetMapping("/page")
    public Page<PurRfq> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "询价单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "询价类型") @RequestParam(required = false) Integer rfqType,
            @Parameter(description = "关键字，用于模糊匹配单号等字段") @RequestParam(required = false) String keyword) {
        return purRfqService.pageQuery(page, size, status, rfqType, keyword);
    }

    @Operation(summary = "按状态查询询价单", description = "查询指定状态下的全部采购询价单，用于列表展示。")
    @GetMapping("/list")
    public List<PurRfq> listByStatus(@Parameter(description = "询价单状态") @RequestParam Integer status) {
        return purRfqService.listByStatus(status);
    }

    @Operation(summary = "新增询价单", description = "创建一张新的采购询价单。")
    @PostMapping
    public boolean save(@RequestBody PurRfq purRfq) {
        return purRfqService.save(purRfq);
    }

    @Operation(summary = "修改询价单", description = "按主键更新已有的采购询价单。")
    @PutMapping
    public boolean update(@RequestBody PurRfq purRfq) {
        return purRfqService.updateById(purRfq);
    }

    @Operation(summary = "删除询价单", description = "按主键删除采购询价单。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "询价单主键 ID") @PathVariable String id) {
        return purRfqService.removeById(id);
    }

    @Operation(summary = "发布询价单", description = "将询价单发布给供应商，进入报价阶段。")
    @PostMapping("/{id}/publish")
    public boolean publish(@Parameter(description = "询价单主键 ID") @PathVariable String id) {
        return purRfqService.publish(id);
    }

    @Operation(summary = "关闭询价单", description = "关闭采购询价单，停止接收新的供应商报价。")
    @PostMapping("/{id}/close")
    public boolean close(@Parameter(description = "询价单主键 ID") @PathVariable String id) {
        return purRfqService.close(id);
    }
}
