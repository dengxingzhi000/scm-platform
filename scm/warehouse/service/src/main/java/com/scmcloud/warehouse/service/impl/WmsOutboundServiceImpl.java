package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import com.scmcloud.warehouse.service.IWmsOutboundService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsOutboundServiceImpl extends ServiceImpl<WmsOutboundMapper, WmsOutbound>
        implements IWmsOutboundService {
}
