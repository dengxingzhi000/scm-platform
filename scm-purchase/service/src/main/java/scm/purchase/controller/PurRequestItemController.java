package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRequestItem;
import scm.purchase.service.IPurRequestItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-request-item")
public class PurRequestItemController {

    private final IPurRequestItemService purRequestItemService;

    @GetMapping("/{id}")
    public PurRequestItem getById(@PathVariable String id) {
        return purRequestItemService.getById(id);
    }

    @GetMapping("/list/{requestId}")
    public List<PurRequestItem> listByRequestId(@PathVariable String requestId) {
        return purRequestItemService.listByRequestId(requestId);
    }

    @PostMapping
    public boolean save(@RequestBody PurRequestItem purRequestItem) {
        return purRequestItemService.save(purRequestItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurRequestItem purRequestItem) {
        return purRequestItemService.updateById(purRequestItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purRequestItemService.removeById(id);
    }

    @DeleteMapping("/request/{requestId}")
    public boolean deleteByRequestId(@PathVariable String requestId) {
        return purRequestItemService.deleteByRequestId(requestId);
    }
}
