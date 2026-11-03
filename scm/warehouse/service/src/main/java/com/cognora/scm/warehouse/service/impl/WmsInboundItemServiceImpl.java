package com.cognora.scm.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.scm.warehouse.domain.entity.WmsInboundItem;
import com.cognora.scm.warehouse.mapper.WmsInboundItemMapper;
import com.cognora.scm.warehouse.service.IWmsInboundItemService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsInboundItemServiceImpl extends ServiceImpl<WmsInboundItemMapper, WmsInboundItem>
        implements IWmsInboundItemService {
}
