package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.MemberAddress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberAddressMapper extends BaseMapper<MemberAddress> {
}
