package com.cognora.ecom.member.controller;

import com.cognora.ecom.member.domain.entity.MemberAddress;
import com.cognora.ecom.member.service.IMemberAddressService;
import com.cognora.platform.common.web.ownership.OwnershipGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/addresses")
@Tag(name = "会员地址", description = "会员收货地址的查询、新增、修改、删除与默认地址设置")
public class MemberAddressController {
    private final IMemberAddressService addressService;

    @Operation(summary = "查询会员地址列表", description = "查询当前登录用户名下的全部收货地址。")
    @GetMapping
    public List<MemberAddress> list(@Parameter(description = "会员用户 ID") @PathVariable String userId) {
        OwnershipGuard.requireSelfOrThrow(userId);
        log.info("[API] List addresses: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return addressService.getByUserId(userId);
    }

    @Operation(summary = "新增会员地址", description = "为当前登录用户新增一条收货地址，会员 ID 以登录态为准。")
    @PostMapping
    public MemberAddress create(@Parameter(description = "会员用户 ID") @PathVariable String userId, @RequestBody MemberAddress address) {
        OwnershipGuard.requireSelfOrThrow(userId);
        log.info("[API] Create address: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        address.setUserId(userId);
        addressService.save(address);
        return address;
    }

    @Operation(summary = "更新会员地址", description = "按地址 ID 更新当前登录用户的收货地址信息。")
    @PutMapping("/{addressId}")
    public boolean update(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "收货地址 ID") @PathVariable Long addressId, @RequestBody MemberAddress address) {
        OwnershipGuard.requireSelfOrThrow(userId);
        requireAddressOwner(addressId, userId);
        log.info("[API] Update address: userId={}, addressId={}", (userId + "").replace("\r", "_").replace("\n", "_"), (addressId + "").replace("\r", "_").replace("\n", "_"));
        address.setId(addressId);
        address.setUserId(userId);
        return addressService.updateById(address);
    }

    @Operation(summary = "删除会员地址", description = "按地址 ID 删除当前登录用户的收货地址。")
    @DeleteMapping("/{addressId}")
    public boolean delete(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "收货地址 ID") @PathVariable Long addressId) {
        OwnershipGuard.requireSelfOrThrow(userId);
        requireAddressOwner(addressId, userId);
        log.info("[API] Delete address: userId={}, addressId={}", (userId + "").replace("\r", "_").replace("\n", "_"), (addressId + "").replace("\r", "_").replace("\n", "_"));
        return addressService.removeById(addressId);
    }

    @Operation(summary = "设置默认地址", description = "将当前登录用户的指定地址设为默认收货地址，原默认地址自动取消。")
    @PutMapping("/{addressId}/default")
    public boolean setDefault(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "收货地址 ID") @PathVariable Long addressId) {
        OwnershipGuard.requireSelfOrThrow(userId);
        requireAddressOwner(addressId, userId);
        log.info("[API] Set default address: userId={}, addressId={}", (userId + "").replace("\r", "_").replace("\n", "_"), (addressId + "").replace("\r", "_").replace("\n", "_"));
        addressService.setDefault(userId, addressId);
        return true;
    }

    private void requireAddressOwner(Long addressId, String userId) {
        MemberAddress address = addressService.getById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new AccessDeniedException("Access denied: address does not belong to the current user");
        }
    }
}
