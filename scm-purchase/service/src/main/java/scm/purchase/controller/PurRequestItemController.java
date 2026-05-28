package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRequestItem;
import scm.purchase.service.IPurRequestItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-request-item")
@Tag(name = "采购申请明细管理", description = "采购申请明细CRUD接口")
public class PurRequestItemController {

    @Autowired
    private IPurRequestItemService purRequestItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurRequestItem getById(@PathVariable String id) {
        return purRequestItemService.getById(id);
    }

    @GetMapping("/list/{requestId}")
    @Operation(summary = "根据申请ID查询明细列表")
    public List<PurRequestItem> listByRequestId(@PathVariable String requestId) {
        return purRequestItemService.listByRequestId(requestId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurRequestItem purRequestItem) {
        return purRequestItemService.save(purRequestItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurRequestItem purRequestItem) {
        return purRequestItemService.updateById(purRequestItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purRequestItemService.removeById(id);
    }

    @DeleteMapping("/request/{requestId}")
    @Operation(summary = "根据申请ID删除所有明细")
    public boolean deleteByRequestId(@PathVariable String requestId) {
        return purRequestItemService.deleteByRequestId(requestId);
    }
}
