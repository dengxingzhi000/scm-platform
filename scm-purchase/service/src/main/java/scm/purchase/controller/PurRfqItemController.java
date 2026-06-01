package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRfqItem;
import scm.purchase.service.IPurRfqItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-rfq-item")
public class PurRfqItemController {

    private final IPurRfqItemService purRfqItemService;

    @GetMapping("/{id}")
    public PurRfqItem getById(@PathVariable String id) {
        return purRfqItemService.getById(id);
    }

    @GetMapping("/list/{rfqId}")
    public List<PurRfqItem> listByRfqId(@PathVariable String rfqId) {
        return purRfqItemService.listByRfqId(rfqId);
    }

    @PostMapping
    public boolean save(@RequestBody PurRfqItem purRfqItem) {
        return purRfqItemService.save(purRfqItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurRfqItem purRfqItem) {
        return purRfqItemService.updateById(purRfqItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purRfqItemService.removeById(id);
    }

    @DeleteMapping("/rfq/{rfqId}")
    public boolean deleteByRfqId(@PathVariable String rfqId) {
        return purRfqItemService.deleteByRfqId(rfqId);
    }
}
