package com.frog.inventory.service.impl;

import com.frog.inventory.domain.entity.InvAlert;
import com.frog.inventory.mapper.InvAlertMapper;
import com.frog.inventory.service.IInvAlertService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库存告警表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class InvAlertServiceImpl extends ServiceImpl<InvAlertMapper, InvAlert> implements IInvAlertService {

}
