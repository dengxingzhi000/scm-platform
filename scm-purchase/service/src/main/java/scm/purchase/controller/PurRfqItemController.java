package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRfqItem;
import scm.purchase.service.IPurRfqItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-rfq-item")
@Tag(name = "询价单明细管理", description = "询价单明细CRUD接口")
public class PurRfqItemController {

    @Autowired
    private IPurRfqItemService purRfqItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurRfqItem getById(@PathVariable String id) {
        return purRfqItemService.getById(id);
    }

    @GetMapping("/list/{rfqId}")
    @Operation(summary = "根据询价单ID查询明细列表")
    public List<PurRfqItem> listByRfqId(@PathVariable String rfqId) {
        return purRfqItemService.listByRfqId(rfqId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurRfqItem purRfqItem) {
        return purRfqItemService.save(purRfqItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurRfqItem purRfqItem) {
        return purRfqItemService.updateById(purRfqItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purRfqItemService.removeById(id);
    }

    @DeleteMapping("/rfq/{rfqId}")
    @Operation(summary = "根据询价单ID删除所有明细")
    public boolean deleteByRfqId(@PathVariable String rfqId) {
        return purRfqItemService.deleteByRfqId(rfqId);
    }
}
