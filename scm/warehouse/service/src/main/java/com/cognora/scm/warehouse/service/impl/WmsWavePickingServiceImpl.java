package com.cognora.scm.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.scm.warehouse.domain.entity.WmsWavePicking;
import com.cognora.scm.warehouse.mapper.WmsWavePickingMapper;
import com.cognora.scm.warehouse.service.IWmsWavePickingService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsWavePickingServiceImpl extends ServiceImpl<WmsWavePickingMapper, WmsWavePicking>
        implements IWmsWavePickingService {
}
