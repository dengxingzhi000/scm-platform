package com.frog.inventory.service.impl;

import com.frog.inventory.domain.entity.InvSnapshot;
import com.frog.inventory.mapper.InvSnapshotMapper;
import com.frog.inventory.service.IInvSnapshotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库存快照表（每日快照） 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class InvSnapshotServiceImpl extends ServiceImpl<InvSnapshotMapper, InvSnapshot> implements IInvSnapshotService {

}
