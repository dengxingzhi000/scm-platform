package com.scmcloud.inventory.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 库存告警前端控制器
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@RestController
@RequestMapping("/inv-alert")
@Tag(name = "库存预警", description = "库存预警相关接口，当前仅保留控制器入口，暂无对外接口。")
public class InvAlertController {

}
