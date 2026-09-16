package com.scmcloud.inventory.service.impl;

import com.scmcloud.inventory.domain.entity.InvAlert;
import com.scmcloud.inventory.mapper.InvAlertMapper;
import com.scmcloud.inventory.service.IInvAlertService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库存告警服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class InvAlertServiceImpl extends ServiceImpl<InvAlertMapper, InvAlert> implements IInvAlertService {

}
