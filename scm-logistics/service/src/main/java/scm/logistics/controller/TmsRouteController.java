package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsRoute;
import scm.logistics.service.ITmsRouteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tms-route")
@Tag(name = "配送路线管理", description = "配送路线的增删改查")
public class TmsRouteController {

    @Autowired
    private ITmsRouteService routeService;

    @GetMapping("/{id}")
    @Operation(summary = "查询路线详情")
    public ApiResponse<TmsRoute> getById(
            @Parameter(description = "路线ID", required = true) @PathVariable String id) {
        TmsRoute route = routeService.getById(id);
        return ApiResponse.success(route);
    }

    @GetMapping("/no/{routeNo}")
    @Operation(summary = "根据路线编号查询")
    public ApiResponse<TmsRoute> getByRouteNo(
            @Parameter(description = "路线编号", required = true) @PathVariable String routeNo) {
        TmsRoute route = routeService.getByRouteNo(routeNo);
        return ApiResponse.success(route);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询路线列表")
    public ApiResponse<Page<TmsRoute>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String courierId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate) {
        Page<TmsRoute> result = routeService.pageList(page, size, courierId, status, deliveryDate);
        return ApiResponse.success(result);
    }

    @GetMapping("/courier/{courierId}")
    @Operation(summary = "根据配送员查询路线")
    public ApiResponse<List<TmsRoute>> listByCourierId(
            @Parameter(description = "配送员ID", required = true) @PathVariable String courierId) {
        List<TmsRoute> list = routeService.listByCourierId(courierId);
        return ApiResponse.success(list);
    }

    @PostMapping
    @Operation(summary = "新增配送路线")
    public ApiResponse<TmsRoute> create(@RequestBody TmsRoute route) {
        log.info("新增配送路线: routeNo={}, courierId={}", route.getRouteNo(), route.getCourierId());
        route.setId(UUID.randomUUID().toString());
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        routeService.save(route);
        return ApiResponse.success(route);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改配送路线")
    public ApiResponse<TmsRoute> update(@PathVariable String id, @RequestBody TmsRoute route) {
        log.info("修改配送路线: id={}", id);
        route.setId(id);
        route.setUpdateTime(LocalDateTime.now());
        routeService.updateById(route);
        return ApiResponse.success(route);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配送路线")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除配送路线: id={}", id);
        routeService.removeById(id);
        return ApiResponse.success();
    }
}
