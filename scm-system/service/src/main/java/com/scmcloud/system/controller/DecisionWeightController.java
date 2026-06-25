package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.decision.config.WeightProfile;
import com.scmcloud.system.service.DecisionWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/decision/weights")
public class DecisionWeightController {

    private final DecisionWeightService weightService;

    @GetMapping("/{engineType}")
    public ApiResponse<List<WeightProfile>> getActive(@PathVariable String engineType,
                                                       @RequestParam(required = false) String scene) {
        return ApiResponse.success(weightService.findActive(engineType, scene));
    }

    @GetMapping("/{engineType}/versions")
    public ApiResponse<List<WeightProfile>> getVersions(@PathVariable String engineType) {
        return ApiResponse.success(weightService.findAllVersions(engineType));
    }

    @PostMapping
    public ApiResponse<WeightProfile> create(@RequestBody WeightProfile profile) {
        return ApiResponse.success(weightService.create(profile));
    }

    @PutMapping("/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable String id) {
        weightService.activate(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        weightService.delete(id);
        return ApiResponse.success();
    }
}
