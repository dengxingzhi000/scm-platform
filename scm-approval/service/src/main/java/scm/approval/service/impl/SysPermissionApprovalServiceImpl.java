package scm.approval.service.impl;

import scm.approval.domain.entity.SysPermissionApproval;
import scm.approval.mapper.SysPermissionApprovalMapper;
import scm.approval.service.ISysPermissionApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 权限申请审批表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class SysPermissionApprovalServiceImpl extends ServiceImpl<SysPermissionApprovalMapper, SysPermissionApproval>
        implements ISysPermissionApprovalService {

}
