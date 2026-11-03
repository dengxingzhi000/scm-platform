package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurRfq;
import com.scmcloud.purchase.mapper.PurRfqMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRfqCommandService {

    private final PurRfqMapper purRfqMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurRfq entity) {
        return purRfqMapper.insert(entity) > 0;
    }

    @Master(reason = "更新询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurRfq entity) {
        return purRfqMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purRfqMapper.deleteById(id) > 0;
    }

    @Master(reason = "发布询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(String id) {
        PurRfq rfq = purRfqMapper.selectById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "DRAFT", "PENDING_APPROVAL");
        rfq.setStatus(1); // PENDING_APPROVAL
        rfq.setUpdateTime(LocalDateTime.now());
        return purRfqMapper.updateById(rfq) > 0;
    }

    @Master(reason = "关闭询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRfq rfq = purRfqMapper.selectById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        String fromStatus;
        switch (rfq.getStatus()) {
            case 0: fromStatus = "DRAFT"; break;
            case 1: fromStatus = "PENDING_APPROVAL"; break;
            case 2: fromStatus = "APPROVED"; break;
            case 3: fromStatus = "REJECTED"; break;
            case 4: fromStatus = "CANCELLED"; break;
            default: throw new IllegalStateException("未知状态: " + rfq.getStatus());
        }
        statusValidator.validateTransition("PURCHASE", fromStatus, "CANCELLED");
        rfq.setStatus(4); // CANCELLED
        rfq.setUpdateTime(LocalDateTime.now());
        return purRfqMapper.updateById(rfq) > 0;
    }
}
