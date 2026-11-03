package com.scmcloud.common.dto.dept;

import com.scmcloud.common.web.annotation.Sensitive;
import com.scmcloud.common.web.enums.SensitiveType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 部门 DTO
 *
 * @author Deng
 * createData 2025/11/7 11:15
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptDTO {
    private UUID id;
    private UUID parentId;

    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Integer deptType;
    private UUID leaderId;
    @Sensitive(type = SensitiveType.NAME)
    private String leaderName;
    @Sensitive(type = SensitiveType.MOBILE)
    private String phone;
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;
    private Integer isolationLevel;
    private Integer sortOrder;
    private Integer status;

    // 树形结构
    private List<DeptDTO> children;

    // 统计信息
    private Integer userCount;
    private Integer childCount;
}
