package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.mapper.WmsLocationMapper;
import com.scmcloud.warehouse.service.IWmsLocationService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation>
        implements IWmsLocationService {
}
