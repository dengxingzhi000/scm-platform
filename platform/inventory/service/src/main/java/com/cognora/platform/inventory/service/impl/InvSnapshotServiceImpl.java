package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.inventory.domain.entity.InvSnapshot;
import com.cognora.platform.inventory.mapper.InvSnapshotMapper;
import com.cognora.platform.inventory.service.IInvSnapshotService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库存快照表（每日快照）服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class InvSnapshotServiceImpl extends ServiceImpl<InvSnapshotMapper, InvSnapshot> implements IInvSnapshotService {

}
