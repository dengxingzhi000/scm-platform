package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import com.scmcloud.warehouse.service.IWmsOutboundItemService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsOutboundItemServiceImpl extends ServiceImpl<WmsOutboundItemMapper, WmsOutboundItem>
        implements IWmsOutboundItemService {
}
