# scm-audit — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8210/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-audit (1.0.0)
- 认证: Bearer JWT (Authorization header)

## SensitiveOperationLog

### POST /sys-sensitive-operation-log
- 说明: 创建敏感操作日志
- 描述: 写入一条敏感操作日志记录。
- 请求体: `SysSensitiveOperationLog`
- 响应: `SysSensitiveOperationLog`

### GET /sys-sensitive-operation-log/{id}
- 说明: 查询敏感操作日志
- 描述: 根据日志 ID 查询敏感操作日志详情。
- 参数: `id`(path,必填: string)
- 响应: `SysSensitiveOperationLog`

### DELETE /sys-sensitive-operation-log/{id}
- 说明: 删除敏感操作日志
- 描述: 根据日志 ID 删除敏感操作日志记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-sensitive-operation-log/user/{userId}
- 说明: 按用户查询敏感操作日志
- 描述: 查询指定用户产生的全部敏感操作日志。
- 参数: `userId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-sensitive-operation-log/risk-level/{riskScore}
- 说明: 按风险等级查询敏感操作日志
- 描述: 查询指定风险分值的敏感操作日志。
- 参数: `riskScore`(path,必填: integer)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-sensitive-operation-log/page
- 说明: 分页查询敏感操作日志
- 描述: 按用户、操作类型和风险分值分页查询敏感操作日志。
- 参数: `page`(query: integer) `size`(query: integer) `userId`(query: string) `operationType`(query: string) `riskScore`(query: integer)
- 响应: `PageSysSensitiveOperationLog`

## AuditLog

### POST /sys-audit-log
- 说明: 创建审计日志
- 描述: 写入一条审计日志记录。
- 请求体: `SysAuditLog`
- 响应: `SysAuditLog`

### GET /sys-audit-log/{id}
- 说明: 查询审计日志
- 描述: 根据日志 ID 查询审计日志详情。
- 参数: `id`(path,必填: string)
- 响应: `SysAuditLog`

### DELETE /sys-audit-log/{id}
- 说明: 删除审计日志
- 描述: 根据日志 ID 删除审计日志记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-audit-log/user/{userId}
- 说明: 按用户查询审计日志
- 描述: 查询指定用户产生的全部审计日志。
- 参数: `userId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-audit-log/page
- 说明: 分页查询审计日志
- 描述: 按用户、操作类型、模块、业务类型和风险等级分页查询审计日志。
- 参数: `page`(query: integer) `size`(query: integer) `userId`(query: string) `operationType`(query: string) `module`(query: string) `businessType`(query: string) `riskLevel`(query: integer)
- 响应: `PageSysAuditLog`

### GET /sys-audit-log/module/{module}
- 说明: 按模块查询审计日志
- 描述: 查询指定业务模块的审计日志。
- 参数: `module`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-audit-log/business-type/{businessType}
- 说明: 按业务类型查询审计日志
- 描述: 查询指定业务类型的审计日志。
- 参数: `businessType`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **SysSensitiveOperationLog** — `id`: string, `userId`: string, `username`: string, `operationType`: string, `operationModule`: string, `sensitiveDataType`: string, `dataFingerprint`: string, `affectedCount`: integer, `targetTable`: string, `targetIds`: string, `operationDetail`: string, `approvalRequired`: boolean, `approvalId`: string, `riskScore`: integer, `ipAddress`: string, `userAgent`: string, `deviceFingerprint`: string, `location`: string, `createTime`: string, `realName`: string, `deptName`: string
- **SysAuditLog** — `id`: string, `userId`: string, `username`: string, `realName`: string, `deptId`: string, `operationType`: string, `operationModule`: string, `operationDesc`: string, `requestUri`: string, `requestMethod`: string, `requestParams`: string, `responseData`: string, `responseStatus`: integer, `ipAddress`: string, `location`: string, `userAgent`: string, `businessType`: string, `businessId`: string, `oldValue`: string, `newValue`: string, `riskLevel`: integer, `status`: integer, `errorMsg`: string, `executeTime`: integer, `createTime`: string
- **OrderItem** — `column`: string, `asc`: boolean
- **PageSysSensitiveOperationLog** — `records`: SysSensitiveOperationLog[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSysSensitiveOperationLog, `searchCount`: PageSysSensitiveOperationLog, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageSysAuditLog** — `records`: SysAuditLog[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSysAuditLog, `searchCount`: PageSysAuditLog, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer