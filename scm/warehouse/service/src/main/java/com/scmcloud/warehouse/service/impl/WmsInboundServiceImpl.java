package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.mapper.WmsInboundMapper;
import com.scmcloud.warehouse.service.IWmsInboundService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsInboundServiceImpl extends ServiceImpl<WmsInboundMapper, WmsInbound>
        implements IWmsInboundService {
}
