package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurRequest;
import scm.purchase.service.IPurRequestService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-request")
public class PurRequestController {

    private final IPurRequestService purRequestService;

    @GetMapping("/{id}")
    public PurRequest getById(@PathVariable String id) {
        return purRequestService.getById(id);
    }

    @GetMapping("/no/{requestNo}")
    public PurRequest getByRequestNo(@PathVariable String requestNo) {
        return purRequestService.getByRequestNo(requestNo);
    }

    @GetMapping("/page")
    public Page<PurRequest> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer requestType,
            @RequestParam(required = false) String keyword) {
        return purRequestService.pageQuery(page, size, status, requestType, keyword);
    }

    @GetMapping("/list")
    public List<PurRequest> listByStatus(@RequestParam Integer status) {
        return purRequestService.listByStatus(status);
    }

    @PostMapping
    public boolean save(@RequestBody PurRequest purRequest) {
        return purRequestService.save(purRequest);
    }

    @PutMapping
    public boolean update(@RequestBody PurRequest purRequest) {
        return purRequestService.updateById(purRequest);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purRequestService.removeById(id);
    }

    @PostMapping("/{id}/submit")
    public boolean submit(@PathVariable String id) {
        return purRequestService.submit(id);
    }

    @PostMapping("/{id}/approve")
    public boolean approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        return purRequestService.approve(id, approverId, approverName);
    }

    @PostMapping("/{id}/reject")
    public boolean reject(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName,
            @RequestParam String reason) {
        return purRequestService.reject(id, approverId, approverName, reason);
    }

    @PostMapping("/{id}/close")
    public boolean close(@PathVariable String id) {
        return purRequestService.close(id);
    }
}
