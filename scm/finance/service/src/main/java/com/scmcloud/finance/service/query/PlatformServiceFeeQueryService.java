package com.scmcloud.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.mapper.PlatformServiceFeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformServiceFeeQueryService {
    private final PlatformServiceFeeMapper platformServiceFeeMapper;

    @Slave
    public List<PlatformServiceFee> listPendingFees() {
        log.debug("查询待付款平台服务费");
        LambdaQueryWrapper<PlatformServiceFee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PlatformServiceFee::getStatus, 0)
                .orderByDesc(PlatformServiceFee::getCreateTime);
        return platformServiceFeeMapper.selectList(wrapper);
    }
}
