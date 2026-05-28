package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRfq;
import scm.purchase.service.IPurRfqService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-rfq")
@Tag(name = "询价单管理", description = "询价单CRUD及工作流接口")
public class PurRfqController {

    @Autowired
    private IPurRfqService purRfqService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询询价单")
    public PurRfq getById(@PathVariable String id) {
        return purRfqService.getById(id);
    }

    @GetMapping("/no/{rfqNo}")
    @Operation(summary = "根据询价编号查询")
    public PurRfq getByRfqNo(@PathVariable String rfqNo) {
        return purRfqService.getByRfqNo(rfqNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询询价单")
    public Page<PurRfq> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer rfqType,
            @RequestParam(required = false) String keyword) {
        return purRfqService.pageQuery(page, size, status, rfqType, keyword);
    }

    @GetMapping("/list")
    @Operation(summary = "根据状态查询列表")
    public List<PurRfq> listByStatus(@RequestParam Integer status) {
        return purRfqService.listByStatus(status);
    }

    @PostMapping
    @Operation(summary = "创建询价单")
    public boolean save(@RequestBody PurRfq purRfq) {
        return purRfqService.save(purRfq);
    }

    @PutMapping
    @Operation(summary = "更新询价单")
    public boolean update(@RequestBody PurRfq purRfq) {
        return purRfqService.updateById(purRfq);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除询价单")
    public boolean delete(@PathVariable String id) {
        return purRfqService.removeById(id);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布询价单")
    public boolean publish(@PathVariable String id) {
        return purRfqService.publish(id);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭询价单")
    public boolean close(@PathVariable String id) {
        return purRfqService.close(id);
    }
}
