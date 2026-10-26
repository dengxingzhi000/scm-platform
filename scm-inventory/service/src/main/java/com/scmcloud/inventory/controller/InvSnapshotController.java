package com.scmcloud.inventory.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 库存快照表（每日快照）前端控制器
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@RestController
@RequestMapping("/inv-snapshot")
@Tag(name = "库存快照", description = "库存每日快照相关接口，当前仅保留控制器入口，暂无对外接口。")
public class InvSnapshotController {

}
