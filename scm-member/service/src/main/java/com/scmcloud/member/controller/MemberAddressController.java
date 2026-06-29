package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.MemberAddress;
import com.scmcloud.member.service.IMemberAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/addresses")
public class MemberAddressController {

    private final IMemberAddressService addressService;

    @GetMapping
    public List<MemberAddress> list(@PathVariable String userId) {
        log.info("[API] List addresses: userId={}", userId);
        return addressService.getByUserId(userId);
    }

    @PostMapping
    public MemberAddress create(@PathVariable String userId, @RequestBody MemberAddress address) {
        log.info("[API] Create address: userId={}", userId);
        address.setUserId(userId);
        addressService.save(address);
        return address;
    }

    @PutMapping("/{addressId}")
    public boolean update(@PathVariable String userId, @PathVariable Long addressId, @RequestBody MemberAddress address) {
        log.info("[API] Update address: userId={}, addressId={}", userId, addressId);
        address.setId(addressId);
        address.setUserId(userId);
        return addressService.updateById(address);
    }

    @DeleteMapping("/{addressId}")
    public boolean delete(@PathVariable String userId, @PathVariable Long addressId) {
        log.info("[API] Delete address: userId={}, addressId={}", userId, addressId);
        return addressService.removeById(addressId);
    }

    @PutMapping("/{addressId}/default")
    public boolean setDefault(@PathVariable String userId, @PathVariable Long addressId) {
        log.info("[API] Set default address: userId={}, addressId={}", userId, addressId);
        addressService.setDefault(userId, addressId);
        return true;
    }
}
