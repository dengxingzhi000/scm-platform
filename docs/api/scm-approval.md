# scm-approval — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8209/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-approval (1.0.0)
- 认证: Bearer JWT (Authorization header)

## PermissionApproval

### POST /sys-permission-approval/{id}/reject
- 说明: 驳回权限审批
- 描述: 审批人驳回指定的权限变更申请并填写驳回原因。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string) `rejectReason`(query,必填: string)
- 响应: `ApiResponseSysPermissionApproval`

### POST /sys-permission-approval/{id}/approve
- 说明: 通过权限审批
- 描述: 审批人通过指定的权限变更申请并记录审批信息。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string)
- 响应: `ApiResponseSysPermissionApproval`

### POST /sys-permission-approval/submit
- 说明: 提交权限审批
- 描述: 提交一条权限变更申请，进入待审批状态。
- 请求体: `SysPermissionApproval`
- 响应: `ApiResponseSysPermissionApproval`

### GET /sys-permission-approval/pending
- 说明: 查询待审批单
- 描述: 查询指定审批人名下处于待审批状态的权限审批单。
- 参数: `approverId`(query,必填: string)
- 响应: `ApiResponseListSysPermissionApproval`

### GET /sys-permission-approval/applicant/{applicantId}
- 说明: 查询申请人审批单
- 描述: 查询指定申请人提交的全部权限审批单。
- 参数: `applicantId`(path,必填: string)
- 响应: `ApiResponseListSysPermissionApproval`

## ApprovalProcess

### POST /approval/flow/start
- 说明: 发起审批
- 描述: 根据流程定义与业务单据信息发起一个审批流程实例，返回流程实例 ID。
- 请求体: `StartProcessRequest`
- 响应: `ApiResponseString`

### POST /approval/flow/reject
- 说明: 审批驳回
- 描述: 填写驳回原因，将指定待办任务驳回并结束当前审批分支。
- 请求体: `RejectRequest`
- 响应: `ApiResponseVoid`

### POST /approval/flow/approve
- 说明: 审批通过
- 描述: 提交审批意见，将指定待办任务置为通过并推进流程。
- 请求体: `ApproveRequest`
- 响应: `ApiResponseVoid`

### GET /approval/flow/todo
- 说明: 查询待办任务
- 描述: 查询指定审批人名下尚未处理的待办任务列表。
- 参数: `assigneeId`(query,必填: string)
- 响应: `ApiResponseListTaskDTO`

### GET /approval/flow/status/{businessKey}
- 说明: 查询流程状态
- 描述: 根据业务单据标识查询审批流程状态，返回 COMPLETED、IN_PROGRESS 或 NOT_FOUND。
- 参数: `businessKey`(path,必填: string)
- 响应: `ApiResponseString`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseSysPermissionApproval** — `code`: integer, `message`: string, `data`: SysPermissionApproval, `timestamp`: integer
- **SysPermissionApproval** — `id`: string, `applicantId`: string, `approvalType`: integer, `targetUserId`: string, `roleIds`: string, `permissionIds`: string, `effectiveTime`: string, `expireTime`: string, `applyReason`: string, `businessJustification`: string, `approvalStatus`: integer, `currentApproverId`: string, `approvalChain`: string, `approvedBy`: string, `approvedTime`: string, `rejectReason`: string, `createTime`: string, `updateTime`: string, `applicantName`: string, `applicantDeptName`: string, `targetUserName`: string, `approverName`: string, `roleNames`: string, `permissionNames`: string
- **StartProcessRequest** — `processKey`: string, `businessKey`: string, `businessType`: string, `applicantId`: string, `variables`: object
- **ApiResponseString** — `code`: integer, `message`: string, `data`: string, `timestamp`: integer
- **RejectRequest** — `taskId`: string, `approverId`: string, `comment`: string
- **ApproveRequest** — `taskId`: string, `approverId`: string, `comment`: string, `variables`: object
- **ApiResponseListSysPermissionApproval** — `code`: integer, `message`: string, `data`: SysPermissionApproval[], `timestamp`: integer
- **ApiResponseListTaskDTO** — `code`: integer, `message`: string, `data`: TaskDTO[], `timestamp`: integer
- **TaskDTO** — `taskId`: string, `taskName`: string, `assignee`: string, `processInstanceId`: string, `createTime`: string, `dueDate`: string, `taskDefinitionKey`: string