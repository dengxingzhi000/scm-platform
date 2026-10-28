# scm-system — OpenAPI 文档摘要

- 来源: `http://192.168.0.125:8081/api/public/v3/api-docs`
- 标题: SCM Platform API - system-service (1.0.0)
- 认证: Bearer JWT (Authorization header)

## DecisionWeight

### PUT /api/v1/decision/weights/{id}/activate
- 说明: 启用权重方案
- 描述: 将指定的权重方案设为该引擎当前生效的版本。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/v1/decision/weights
- 说明: 新增权重方案
- 描述: 创建新的权重方案，各维度权重之和须为 1。
- 请求体: `WeightProfile`
- 响应: `ApiResponseWeightProfile`

### GET /api/v1/decision/weights/{engineType}
- 说明: 查询生效权重方案
- 描述: 按引擎类型与业务场景查询当前生效的权重方案。
- 参数: `engineType`(path,必填: string) `scene`(query: string)
- 响应: `ApiResponseListWeightProfile`

### GET /api/v1/decision/weights/{engineType}/versions
- 说明: 查询权重历史版本
- 描述: 查询指定引擎类型下全部历史版本的权重方案。
- 参数: `engineType`(path,必填: string)
- 响应: `ApiResponseListWeightProfile`

### DELETE /api/v1/decision/weights/{id}
- 说明: 删除权重方案
- 描述: 根据方案 ID 删除指定的权重方案。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

## DecisionRule

### PUT /api/v1/decision/rules/{id}
- 说明: 修改决策规则
- 描述: 根据规则 ID 更新规则内容并返回更新后的规则。
- 参数: `id`(path,必填: string)
- 请求体: `DecisionRule`
- 响应: `ApiResponseDecisionRule`

### DELETE /api/v1/decision/rules/{id}
- 说明: 删除决策规则
- 描述: 根据规则 ID 删除指定的决策规则。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/decision/rules/{id}/toggle
- 说明: 启停决策规则
- 描述: 根据规则 ID 开启或关闭该条决策规则。
- 参数: `id`(path,必填: string) `enabled`(query,必填: boolean)
- 响应: `ApiResponseVoid`

### POST /api/v1/decision/rules
- 说明: 新增决策规则
- 描述: 创建一条决策规则并返回创建后的规则信息。
- 请求体: `DecisionRule`
- 响应: `ApiResponseDecisionRule`

### POST /api/v1/decision/rules/conflicts/{engineType}
- 说明: 检测规则冲突
- 描述: 检测指定引擎类型下全部决策规则之间是否存在条件冲突。
- 参数: `engineType`(path,必填: string)
- 响应: `ApiResponseListConflict`

### GET /api/v1/decision/rules/{engineType}
- 说明: 查询规则列表
- 描述: 按决策引擎类型查询该引擎下的全部决策规则。
- 参数: `engineType`(path,必填: string)
- 响应: `ApiResponseListDecisionRule`

## User

### GET /api/system/users/{id}
- 说明: 查询用户详情
- 描述: 根据用户 ID 查询用户的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseUserDTO`

### PUT /api/system/users/{id}
- 说明: 修改用户
- 描述: 根据用户 ID 更新用户信息并写入审计日志。
- 参数: `id`(path,必填: string)
- 请求体: `UserDTO`
- 响应: `ApiResponseVoid`

### DELETE /api/system/users/{id}
- 说明: 删除用户
- 描述: 根据用户 ID 删除用户并写入审计日志。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/system/users
- 说明: 分页查询用户列表
- 描述: 分页查询用户列表，支持按用户名和用户状态筛选。
- 参数: `page`(query: integer) `size`(query: integer) `username`(query: string) `status`(query: integer)
- 响应: `ApiResponsePageResultUserDTO`

### POST /api/system/users
- 说明: 新增用户
- 描述: 新增一个用户并写入审计日志。
- 请求体: `UserDTO`
- 响应: `ApiResponseVoid`

### POST /api/system/users/{userId}/terminate-temporary-role/{roleId}
- 说明: 终止临时角色
- 描述: 提前终止用户某个临时角色的授权。
- 参数: `userId`(path,必填: string) `roleId`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/system/users/{userId}/extend-temporary-role/{roleId}
- 说明: 延长临时角色有效期
- 描述: 更新用户某个临时角色的失效时间，延长其有效期。
- 参数: `userId`(path,必填: string) `roleId`(path,必填: string) `newExpireTime`(query,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/system/users/{id}/reset-password
- 说明: 重置密码
- 描述: 根据用户 ID 重置密码并返回新的初始密码。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseString`

### POST /api/system/users/{id}/lock
- 说明: 锁定或解锁用户
- 描述: 根据用户 ID 锁定或解锁用户账号，锁定操作会设置 24 小时锁定截止时间。
- 参数: `id`(path,必填: string) `lock`(query,必填: boolean)
- 响应: `ApiResponseVoid`

### POST /api/system/users/{id}/grant-temporary-roles
- 说明: 授予临时角色
- 描述: 为用户授予在指定生效时间到失效时间之间有效的临时角色。
- 参数: `id`(path,必填: string)
- 请求体: `TemporaryRoleGrantDTO`
- 响应: `ApiResponseVoid`

### POST /api/system/users/{id}/grant-roles
- 说明: 授予角色
- 描述: 为指定用户重新绑定角色，原有角色关系会被清除后写入。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/system/users/{id}/force-logout
- 说明: 强制用户下线
- 描述: 调用认证服务强制指定用户下线，并记录下线原因。
- 参数: `id`(path,必填: string) `reason`(query,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/system/users/change-password
- 说明: 修改密码
- 描述: 当前登录用户修改自己的登录密码。
- 请求体: `ChangePasswordRequest`
- 响应: `ApiResponseVoid`

### GET /api/system/users/{userId}/update-login
- 说明: 更新最后登录信息
- 描述: 更新指定用户的最后登录时间与登录 IP。
- 参数: `userId`(path,必填: string) `ipAddress`(query,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/system/users/{id}/temporary-roles
- 说明: 查询临时角色列表
- 描述: 查询指定用户当前持有的临时角色列表。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListMapStringObject`

### GET /api/system/users/{id}/statistics
- 说明: 查询用户统计信息
- 描述: 查询指定用户的统计数据。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseMapStringObject`

## StatusDict

### PUT /api/system/status/transitions
- 说明: 修改状态流转规则
- 描述: 更新已有状态流转规则的起始状态、目标状态与动作等信息。
- 请求体: `SysStatusTransition`
- 响应: `ApiResponseVoid`

### POST /api/system/status/transitions
- 说明: 新增状态流转规则
- 描述: 新增一条状态流转规则，主键由系统自动生成。
- 请求体: `SysStatusTransition`
- 响应: `ApiResponseVoid`

### PUT /api/system/status/dict
- 说明: 修改状态字典
- 描述: 更新已有业务状态定义的信息。
- 请求体: `SysStatusDict`
- 响应: `ApiResponseVoid`

### POST /api/system/status/dict
- 说明: 新增状态字典
- 描述: 新增一条业务状态定义，主键由系统自动生成。
- 请求体: `SysStatusDict`
- 响应: `ApiResponseVoid`

### POST /api/system/status/transition
- 说明: 执行状态流转
- 描述: 按业务类型、当前状态与动作编码执行一次状态流转并返回流转结果。
- 参数: `bizType`(query,必填: string) `fromStatus`(query,必填: string) `actionCode`(query,必填: string)
- 响应: `ApiResponseTransitionResult`

### GET /api/system/status/transitions/{bizType}
- 说明: 查询状态流转规则
- 描述: 根据业务类型查询该业务下全部允许的状态流转规则。
- 参数: `bizType`(path,必填: string)
- 响应: `ApiResponseListSysStatusTransition`

### GET /api/system/status/next-statuses
- 说明: 查询下一状态
- 描述: 查询指定业务在当前状态下允许流转到的下一状态列表。
- 参数: `bizType`(query,必填: string) `currentStatus`(query,必填: string)
- 响应: `ApiResponseListString`

### GET /api/system/status/dict/{bizType}
- 说明: 查询业务状态字典
- 描述: 根据业务类型查询该业务下的全部状态定义。
- 参数: `bizType`(path,必填: string)
- 响应: `ApiResponseListSysStatusDict`

### GET /api/system/status/dict/{bizType}/{statusCode}
- 说明: 查询状态详情
- 描述: 根据业务类型与状态编码查询单个状态定义。
- 参数: `bizType`(path,必填: string) `statusCode`(path,必填: string)
- 响应: `ApiResponseSysStatusDict`

### GET /api/system/status/check
- 说明: 校验状态流转
- 描述: 校验指定业务能否从当前状态直接流转到目标状态。
- 参数: `bizType`(query,必填: string) `fromStatus`(query,必填: string) `toStatus`(query,必填: string)
- 响应: `ApiResponseTransitionCheckResult`

### GET /api/system/status/actions
- 说明: 查询可执行动作
- 描述: 查询指定业务在当前状态下可执行的动作列表。
- 参数: `bizType`(query,必填: string) `currentStatus`(query,必填: string)
- 响应: `ApiResponseListAvailableAction`

### DELETE /api/system/status/transitions/{id}
- 说明: 删除状态流转规则
- 描述: 根据流转规则 ID 删除该状态流转规则。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### DELETE /api/system/status/dict/{id}
- 说明: 删除状态字典
- 描述: 根据状态 ID 删除该业务状态定义。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

## Role

### PUT /api/system/roles/{id}
- 说明: 修改角色
- 描述: 根据角色 ID 更新角色信息并写入审计日志。
- 参数: `id`(path,必填: string)
- 请求体: `RoleDTO`
- 响应: `ApiResponseVoid`

### DELETE /api/system/roles/{id}
- 说明: 删除角色
- 描述: 根据角色 ID 删除角色并写入审计日志。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/system/roles
- 说明: 分页查询角色列表
- 描述: 分页查询角色列表，支持按角色名称模糊筛选。
- 参数: `page`(query: integer) `size`(query: integer) `roleName`(query: string)
- 响应: `ApiResponsePageResultRoleDTO`

### POST /api/system/roles
- 说明: 新增角色
- 描述: 新增一个角色并写入审计日志。
- 请求体: `RoleDTO`
- 响应: `ApiResponseVoid`

### POST /api/system/roles/{id}/grant-permissions
- 说明: 角色授权
- 描述: 为指定角色重新绑定权限，原有权限关系会被覆盖。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/system/roles/{id}/permissions
- 说明: 查询角色权限
- 描述: 查询指定角色已绑定的权限 ID 列表。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListUUID`

### GET /api/system/roles/all
- 说明: 查询全部角色
- 描述: 查询当前租户下的全部角色，用于下拉选择。
- 响应: `ApiResponseListRoleDTO`

## Permission

### GET /api/system/permissions/{id}
- 说明: 查询单个权限
- 描述: 根据权限 ID 查询权限详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponsePermissionDTO`

### PUT /api/system/permissions/{id}
- 说明: 修改权限
- 描述: 根据权限 ID 更新权限信息并写入审计日志。
- 参数: `id`(path,必填: string)
- 请求体: `PermissionDTO`
- 响应: `ApiResponseVoid`

### DELETE /api/system/permissions/{id}
- 说明: 删除权限
- 描述: 根据权限 ID 删除权限并写入审计日志。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/system/permissions
- 说明: 新增权限
- 描述: 新增一条权限并写入高风险审计日志。
- 请求体: `PermissionDTO`
- 响应: `ApiResponseVoid`

### GET /api/system/permissions/user/{userId}
- 说明: 查询用户权限
- 描述: 查询指定用户拥有的全部权限标识，供其他服务通过 Feign 调用。
- 参数: `userId`(path,必填: string)
- 响应: `ApiResponseSetString`

### GET /api/system/permissions/tree
- 说明: 查询权限树
- 描述: 查询完整的权限树形结构，用于权限分配与菜单展示。
- 响应: `ApiResponseListPermissionDTO`

### GET /api/system/permissions/find-by-url
- 说明: 按 URL 查询权限
- 描述: 根据请求 URL 与 HTTP 方法查询匹配的权限标识，供网关鉴权调用。
- 参数: `url`(query,必填: string) `method`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /api/system/permissions/api
- 说明: 查询全部 API 权限
- 描述: 查询全部 API 权限映射关系，用于动态权限加载。
- 响应: `200,400,401,403,423,429,500`

## Dict

### GET /api/system/dicts/types
- 说明: 查询字典类型列表
- 描述: 按字典编码和状态筛选查询全部字典类型。
- 参数: `dictCode`(query: string) `status`(query: integer)
- 响应: `ApiResponseListSysDictType`

### PUT /api/system/dicts/types
- 说明: 修改字典类型
- 描述: 更新已有字典类型的编码、名称、描述与状态等信息。
- 请求体: `SysDictType`
- 响应: `ApiResponseVoid`

### POST /api/system/dicts/types
- 说明: 新增字典类型
- 描述: 新增一个字典类型，主键由系统自动生成。
- 请求体: `SysDictType`
- 响应: `ApiResponseVoid`

### PUT /api/system/dicts/items
- 说明: 修改字典项
- 描述: 更新已有字典项的编码、名称、取值、排序与状态等信息。
- 请求体: `SysDictItem`
- 响应: `ApiResponseVoid`

### POST /api/system/dicts/items
- 说明: 新增字典项
- 描述: 为指定字典类型新增一个字典项，主键由系统自动生成。
- 请求体: `SysDictItem`
- 响应: `ApiResponseVoid`

### GET /api/system/dicts/types/code/{dictCode}
- 说明: 按编码查询字典类型
- 描述: 根据字典编码查询对应的字典类型详情。
- 参数: `dictCode`(path,必填: string)
- 响应: `ApiResponseSysDictType`

### GET /api/system/dicts/items/{dictCode}
- 说明: 查询字典项列表
- 描述: 根据字典编码查询该字典下的全部字典项。
- 参数: `dictCode`(path,必填: string)
- 响应: `ApiResponseListSysDictItem`

### DELETE /api/system/dicts/types/{id}
- 说明: 删除字典类型
- 描述: 根据字典类型 ID 删除该字典类型。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### DELETE /api/system/dicts/items/{id}
- 说明: 删除字典项
- 描述: 根据字典项 ID 删除该字典项。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

## Dept

### PUT /api/system/depts/{id}
- 说明: 修改部门
- 描述: 根据部门 ID 更新部门编码、名称、上级部门等信息。
- 参数: `id`(path,必填: string)
- 请求体: `DeptDTO`
- 响应: `ApiResponseVoid`

### DELETE /api/system/depts/{id}
- 说明: 删除部门
- 描述: 根据部门 ID 删除指定部门。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/system/depts
- 说明: 新增部门
- 描述: 新增部门，可设置上级部门、部门编码、名称、负责人与联系方式等信息。
- 请求体: `DeptDTO`
- 响应: `ApiResponseVoid`

### GET /api/system/depts/tree
- 说明: 查询部门树
- 描述: 查询完整的部门树形结构，用于组织架构展示与部门选择。
- 响应: `ApiResponseListDeptDTO`

## Schemas

- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **DecisionRule** — `id`: string, `engineType`: string, `scene`: string, `ruleType`: string, `expression`: string, `description`: string, `enabled`: boolean, `priority`: integer, `version`: integer
- **ApiResponseDecisionRule** — `code`: integer, `message`: string, `data`: DecisionRule, `timestamp`: integer
- **UserDTO** — `id`: string, `username`: string, `password`: string, `realName`: string, `idCard`: string, `email`: string, `phone`: string, `avatar`: string, `status`: integer, `deptId`: string, `deptName`: string, `userLevel`: integer, `accountType`: integer, `loginAttempts`: integer, `lockedUntil`: string, `passwordExpireTime`: string, `forceChangePassword`: boolean, `twoFactorEnabled`: boolean, `createTime`: string, `lastLoginTime`: string, `lastLoginIp`: string, `remark`: string, `roleIds`: string[], `roleNames`: string[], `permissions`: string[]
- **SysStatusTransition** — `id`: string, `tenantId`: string, `bizType`: string, `fromStatus`: string, `toStatus`: string, `actionCode`: string, `actionName`: string, `actionNameEn`: string, `needApproval`: boolean, `enabled`: boolean, `conditionExpression`: string, `preAction`: string, `postAction`: string, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **SysStatusDict** — `id`: string, `tenantId`: string, `bizType`: string, `statusCode`: string, `statusName`: string, `statusNameEn`: string, `color`: string, `icon`: string, `sortOrder`: integer, `isInitial`: boolean, `isTerminal`: boolean, `isCancellable`: boolean, `enabled`: boolean, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **RoleDTO** — `id`: string, `tenantId`: string, `roleCode`: string, `roleName`: string, `roleDesc`: string, `roleType`: string, `roleCategory`: string, `roleLevel`: integer, `dataScope`: integer, `maxApprovalAmount`: number, `businessScope`: string, `customDeptIds`: string[], `status`: integer, `sortOrder`: integer, `createTime`: string, `updateTime`: string, `permissionIds`: string[], `deptIds`: string[], `userCount`: integer
- **PermissionDTO** — `id`: string, `parentId`: string, `permissionCode`: string, `permissionName`: string, `permissionType`: integer, `routePath`: string, `component`: string, `redirect`: string, `icon`: string, `apiPath`: string, `httpMethod`: string, `permissionScope`: string, `permissionLevel`: integer, `riskLevel`: integer, `needApproval`: boolean, `needTwoFactor`: boolean, `sortOrder`: integer, `visible`: boolean, `status`: integer, `createTime`: string, `updateTime`: string, `children`: PermissionDTO[]
- **SysDictType** — `id`: string, `tenantId`: string, `dictCode`: string, `dictName`: string, `dictNameEn`: string, `description`: string, `status`: integer, `isSystem`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **SysDictItem** — `id`: string, `tenantId`: string, `dictTypeId`: string, `dictCode`: string, `itemCode`: string, `itemName`: string, `itemNameEn`: string, `itemValue`: string, `cssClass`: string, `labelClass`: string, `icon`: string, `color`: string, `sortOrder`: integer, `isDefault`: boolean, `status`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **DeptDTO** — `id`: string, `parentId`: string, `deptCode`: string, `deptName`: string, `deptType`: integer, `leaderId`: string, `leaderName`: string, `phone`: string, `email`: string, `isolationLevel`: integer, `sortOrder`: integer, `status`: integer, `children`: DeptDTO[], `userCount`: integer, `childCount`: integer
- **WeightCondition** — `field`: string, `operator`: string, `value`: any
- **WeightProfile** — `id`: string, `engineType`: string, `scene`: string, `version`: integer, `weights`: object, `conditions`: WeightCondition[], `active`: boolean
- **ApiResponseWeightProfile** — `code`: integer, `message`: string, `data`: WeightProfile, `timestamp`: integer
- **ApiResponseListConflict** — `code`: integer, `message`: string, `data`: Conflict[], `timestamp`: integer
- **Conflict** — `type`: string, `ruleId1`: string, `ruleId2`: string, `description`: string, `severity`: string
- **ApiResponseString** — `code`: integer, `message`: string, `data`: string, `timestamp`: integer
- **TemporaryRoleGrantDTO** — `roleIds`: string[], `effectiveTime`: string, `expireTime`: string, `reason`: string
- **ChangePasswordRequest** — `oldPassword`: string, `newPassword`: string, `confirmPassword`: string
- **ApiResponseTransitionResult** — `code`: integer, `message`: string, `data`: TransitionResult, `timestamp`: integer
- **TransitionResult** — `success`: boolean, `bizType`: string, `fromStatus`: string, `toStatus`: string, `actionCode`: string, `needApproval`: boolean, `postAction`: string, `errorMessage`: string
- **ApiResponseListWeightProfile** — `code`: integer, `message`: string, `data`: WeightProfile[], `timestamp`: integer
- **ApiResponseListDecisionRule** — `code`: integer, `message`: string, `data`: DecisionRule[], `timestamp`: integer
- **ApiResponsePageResultUserDTO** — `code`: integer, `message`: string, `data`: PageResultUserDTO, `timestamp`: integer
- **PageResultUserDTO** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: UserDTO[]
- **ApiResponseUserDTO** — `code`: integer, `message`: string, `data`: UserDTO, `timestamp`: integer
- **ApiResponseListMapStringObject** — `code`: integer, `message`: string, `data`: object[], `timestamp`: integer
- **ApiResponseMapStringObject** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseListSysStatusTransition** — `code`: integer, `message`: string, `data`: SysStatusTransition[], `timestamp`: integer
- **ApiResponseListString** — `code`: integer, `message`: string, `data`: string[], `timestamp`: integer
- **ApiResponseListSysStatusDict** — `code`: integer, `message`: string, `data`: SysStatusDict[], `timestamp`: integer
- **ApiResponseSysStatusDict** — `code`: integer, `message`: string, `data`: SysStatusDict, `timestamp`: integer
- **ApiResponseTransitionCheckResult** — `code`: integer, `message`: string, `data`: TransitionCheckResult, `timestamp`: integer
- **TransitionCheckResult** — `allowed`: boolean, `bizType`: string, `fromStatus`: string, `toStatus`: string, `reason`: string
- **ApiResponseListAvailableAction** — `code`: integer, `message`: string, `data`: AvailableAction[], `timestamp`: integer
- **AvailableAction** — `actionCode`: string, `actionName`: string, `actionNameEn`: string, `targetStatus`: string, `targetStatusName`: string, `needApproval`: boolean, `conditionExpression`: string
- **ApiResponsePageResultRoleDTO** — `code`: integer, `message`: string, `data`: PageResultRoleDTO, `timestamp`: integer
- **PageResultRoleDTO** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: RoleDTO[]
- **ApiResponseListUUID** — `code`: integer, `message`: string, `data`: string[], `timestamp`: integer
- **ApiResponseListRoleDTO** — `code`: integer, `message`: string, `data`: RoleDTO[], `timestamp`: integer
- **ApiResponsePermissionDTO** — `code`: integer, `message`: string, `data`: PermissionDTO, `timestamp`: integer
- **ApiResponseSetString** — `code`: integer, `message`: string, `data`: string[], `timestamp`: integer
- **ApiResponseListPermissionDTO** — `code`: integer, `message`: string, `data`: PermissionDTO[], `timestamp`: integer
- **ApiPermissionDTO** — `apiPath`: string, `httpMethod`: string, `permissionCode`: string
- **ApiResponseListSysDictType** — `code`: integer, `message`: string, `data`: SysDictType[], `timestamp`: integer
- **ApiResponseSysDictType** — `code`: integer, `message`: string, `data`: SysDictType, `timestamp`: integer
- **ApiResponseListSysDictItem** — `code`: integer, `message`: string, `data`: SysDictItem[], `timestamp`: integer
- **ApiResponseListDeptDTO** — `code`: integer, `message`: string, `data`: DeptDTO[], `timestamp`: integer