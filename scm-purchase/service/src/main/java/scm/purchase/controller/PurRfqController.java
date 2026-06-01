package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRfq;
import scm.purchase.service.IPurRfqService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-rfq")
public class PurRfqController {

    private final IPurRfqService purRfqService;

    @GetMapping("/{id}")
    public PurRfq getById(@PathVariable String id) {
        return purRfqService.getById(id);
    }

    @GetMapping("/no/{rfqNo}")
    public PurRfq getByRfqNo(@PathVariable String rfqNo) {
        return purRfqService.getByRfqNo(rfqNo);
    }

    @GetMapping("/page")
    public Page<PurRfq> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer rfqType,
            @RequestParam(required = false) String keyword) {
        return purRfqService.pageQuery(page, size, status, rfqType, keyword);
    }

    @GetMapping("/list")
    public List<PurRfq> listByStatus(@RequestParam Integer status) {
        return purRfqService.listByStatus(status);
    }

    @PostMapping
    public boolean save(@RequestBody PurRfq purRfq) {
        return purRfqService.save(purRfq);
    }

    @PutMapping
    public boolean update(@RequestBody PurRfq purRfq) {
        return purRfqService.updateById(purRfq);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purRfqService.removeById(id);
    }

    @PostMapping("/{id}/publish")
    public boolean publish(@PathVariable String id) {
        return purRfqService.publish(id);
    }

    @PostMapping("/{id}/close")
    public boolean close(@PathVariable String id) {
        return purRfqService.close(id);
    }
}
