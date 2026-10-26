package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.MemberAddress;
import com.scmcloud.member.service.IMemberAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/addresses")
@Tag(name = "会员地址", description = "会员收货地址的查询、新增、修改、删除与默认地址设置")
public class MemberAddressController {
    private final IMemberAddressService addressService;

    @Operation(summary = "查询会员地址列表", description = "查询指定会员名下的全部收货地址。")
    @GetMapping
    public List<MemberAddress> list(@Parameter(description = "会员用户 ID") @PathVariable String userId) {
        log.info("[API] List addresses: userId={}", userId);
        return addressService.getByUserId(userId);
    }

    @Operation(summary = "新增会员地址", description = "为指定会员新增一条收货地址，会员 ID 以路径参数为准。")
    @PostMapping
    public MemberAddress create(@Parameter(description = "会员用户 ID") @PathVariable String userId, @RequestBody MemberAddress address) {
        log.info("[API] Create address: userId={}", userId);
        address.setUserId(userId);
        addressService.save(address);
        return address;
    }

    @Operation(summary = "更新会员地址", description = "按地址 ID 更新指定会员的收货地址信息。")
    @PutMapping("/{addressId}")
    public boolean update(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "收货地址 ID") @PathVariable Long addressId, @RequestBody MemberAddress address) {
        log.info("[API] Update address: userId={}, addressId={}", userId, addressId);
        address.setId(addressId);
        address.setUserId(userId);
        return addressService.updateById(address);
    }

    @Operation(summary = "删除会员地址", description = "按地址 ID 删除指定会员的收货地址。")
    @DeleteMapping("/{addressId}")
    public boolean delete(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "收货地址 ID") @PathVariable Long addressId) {
        log.info("[API] Delete address: userId={}, addressId={}", userId, addressId);
        return addressService.removeById(addressId);
    }

    @Operation(summary = "设置默认地址", description = "将指定地址设为该会员的默认收货地址，原默认地址自动取消。")
    @PutMapping("/{addressId}/default")
    public boolean setDefault(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "收货地址 ID") @PathVariable Long addressId) {
        log.info("[API] Set default address: userId={}, addressId={}", userId, addressId);
        addressService.setDefault(userId, addressId);
        return true;
    }
}
