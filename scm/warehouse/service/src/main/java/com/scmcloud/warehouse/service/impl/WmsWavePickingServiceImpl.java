package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import com.scmcloud.warehouse.service.IWmsWavePickingService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsWavePickingServiceImpl extends ServiceImpl<WmsWavePickingMapper, WmsWavePicking>
        implements IWmsWavePickingService {
}
