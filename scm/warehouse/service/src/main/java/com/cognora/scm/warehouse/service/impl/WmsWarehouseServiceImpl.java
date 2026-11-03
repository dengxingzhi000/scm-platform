package com.cognora.scm.warehouse.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.scm.warehouse.domain.entity.WmsWarehouse;
import com.cognora.scm.warehouse.mapper.WmsWarehouseMapper;
import com.cognora.scm.warehouse.service.IWmsWarehouseService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse>
        implements IWmsWarehouseService {
}
