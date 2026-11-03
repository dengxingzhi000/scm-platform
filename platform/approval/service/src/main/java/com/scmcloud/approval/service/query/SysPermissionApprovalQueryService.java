package com.scmcloud.approval.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.approval.domain.entity.SysPermissionApproval;
import com.scmcloud.approval.mapper.SysPermissionApprovalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionApprovalQueryService {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVING = 1;

    private final SysPermissionApprovalMapper sysPermissionApprovalMapper;

    @Slave
    public List<SysPermissionApproval> listByApplicant(String applicantId) {
        log.debug("查询申请人审批列表: applicantId={}", applicantId);
        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysPermissionApproval::getApplicantId, applicantId)
               .orderByDesc(SysPermissionApproval::getCreateTime);
        return sysPermissionApprovalMapper.selectList(wrapper);
    }

    @Slave
    public List<SysPermissionApproval> listPending(String approverId) {
        log.debug("查询待审批列表: approverId={}", approverId);
        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.and(w -> w.eq(SysPermissionApproval::getApprovalStatus, STATUS_PENDING)
                          .or()
                          .eq(SysPermissionApproval::getApprovalStatus, STATUS_APPROVING));
        wrapper.orderByDesc(SysPermissionApproval::getCreateTime);
        return sysPermissionApprovalMapper.selectList(wrapper);
    }
}
