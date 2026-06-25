package com.scmcloud.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.member.domain.entity.MemberAddress;
import com.scmcloud.member.mapper.MemberAddressMapper;
import com.scmcloud.member.service.IMemberAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class MemberAddressServiceImpl extends ServiceImpl<MemberAddressMapper, MemberAddress> implements IMemberAddressService {

    @Override
    public List<MemberAddress> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getUserId, userId)
                .orderByDesc(MemberAddress::getIsDefault)
                .orderByDesc(MemberAddress::getCreatedAt));
    }

    @Override
    public MemberAddress getDefaultAddress(String userId) {
        return getOne(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getUserId, userId)
                .eq(MemberAddress::getIsDefault, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String userId, Long addressId) {
        List<MemberAddress> addresses = getByUserId(userId);
        for (MemberAddress addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                updateById(addr);
            }
        }

        MemberAddress address = getById(addressId);
        if (address != null && address.getUserId().equals(userId)) {
            address.setIsDefault(true);
            updateById(address);
        }
    }
}
