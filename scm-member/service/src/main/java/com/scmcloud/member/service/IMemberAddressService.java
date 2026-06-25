package com.scmcloud.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.member.domain.entity.MemberAddress;

import java.util.List;

public interface IMemberAddressService extends IService<MemberAddress> {

    List<MemberAddress> getByUserId(String userId);

    MemberAddress getDefaultAddress(String userId);

    void setDefault(String userId, Long addressId);
}
