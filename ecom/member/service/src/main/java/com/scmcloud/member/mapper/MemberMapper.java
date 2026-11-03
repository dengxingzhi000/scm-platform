package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
