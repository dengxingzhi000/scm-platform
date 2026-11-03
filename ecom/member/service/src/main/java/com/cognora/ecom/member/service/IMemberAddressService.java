package com.cognora.ecom.member.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.member.domain.entity.MemberAddress;

import java.util.List;

public interface IMemberAddressService extends IService<MemberAddress> {

    List<MemberAddress> getByUserId(String userId);

    MemberAddress getDefaultAddress(String userId);

    void setDefault(String userId, Long addressId);
}
