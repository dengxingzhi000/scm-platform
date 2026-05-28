package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRequest;
import scm.purchase.service.IPurRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-request")
@Tag(name = "采购申请管理", description = "采购申请单CRUD及工作流接口")
public class PurRequestController {

    @Autowired
    private IPurRequestService purRequestService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询采购申请")
    public PurRequest getById(@PathVariable String id) {
        return purRequestService.getById(id);
    }

    @GetMapping("/no/{requestNo}")
    @Operation(summary = "根据申请编号查询")
    public PurRequest getByRequestNo(@PathVariable String requestNo) {
        return purRequestService.getByRequestNo(requestNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询采购申请")
    public Page<PurRequest> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer requestType,
            @RequestParam(required = false) String keyword) {
        return purRequestService.pageQuery(page, size, status, requestType, keyword);
    }

    @GetMapping("/list")
    @Operation(summary = "根据状态查询列表")
    public List<PurRequest> listByStatus(@RequestParam Integer status) {
        return purRequestService.listByStatus(status);
    }

    @PostMapping
    @Operation(summary = "创建采购申请")
    public boolean save(@RequestBody PurRequest purRequest) {
        return purRequestService.save(purRequest);
    }

    @PutMapping
    @Operation(summary = "更新采购申请")
    public boolean update(@RequestBody PurRequest purRequest) {
        return purRequestService.updateById(purRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购申请")
    public boolean delete(@PathVariable String id) {
        return purRequestService.removeById(id);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交采购申请")
    public boolean submit(@PathVariable String id) {
        return purRequestService.submit(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public boolean approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        return purRequestService.approve(id, approverId, approverName);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批驳回")
    public boolean reject(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName,
            @RequestParam String reason) {
        return purRequestService.reject(id, approverId, approverName, reason);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭采购申请")
    public boolean close(@PathVariable String id) {
        return purRequestService.close(id);
    }
}
