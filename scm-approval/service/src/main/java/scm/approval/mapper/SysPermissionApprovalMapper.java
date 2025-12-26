package scm.approval.mapper;

import org.apache.ibatis.annotations.Mapper;
import scm.approval.domain.entity.SysPermissionApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 权限申请审批表 Mapper 接口
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Mapper
public interface SysPermissionApprovalMapper extends BaseMapper<SysPermissionApproval> {

}
