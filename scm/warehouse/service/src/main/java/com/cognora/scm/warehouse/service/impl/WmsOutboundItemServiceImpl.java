package com.cognora.scm.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.scm.warehouse.domain.entity.WmsOutboundItem;
import com.cognora.scm.warehouse.mapper.WmsOutboundItemMapper;
import com.cognora.scm.warehouse.service.IWmsOutboundItemService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsOutboundItemServiceImpl extends ServiceImpl<WmsOutboundItemMapper, WmsOutboundItem>
        implements IWmsOutboundItemService {
}
