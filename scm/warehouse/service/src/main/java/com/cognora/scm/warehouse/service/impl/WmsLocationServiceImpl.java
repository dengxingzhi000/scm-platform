package com.cognora.scm.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.scm.warehouse.domain.entity.WmsLocation;
import com.cognora.scm.warehouse.mapper.WmsLocationMapper;
import com.cognora.scm.warehouse.service.IWmsLocationService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation>
        implements IWmsLocationService {
}
