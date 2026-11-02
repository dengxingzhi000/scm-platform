# scm-notify — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8211/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-notify (1.0.0)
- 认证: Bearer JWT (Authorization header)

## InAppNotification

### PUT /sys-user-notification/{id}/read
- 说明: 标记通知已读
- 描述: 将指定通知标记为已读并记录阅读时间。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseBoolean`

### PUT /sys-user-notification/read-all
- 说明: 全部标记已读
- 描述: 将当前用户（或当前租户）的全部未读通知标记为已读。
- 参数: `userId`(query: string)
- 响应: `ApiResponseBoolean`

### GET /sys-user-notification/unread-count
- 说明: 查询未读通知数
- 描述: 统计当前用户（或当前租户）未读通知数量。
- 参数: `userId`(query: string)
- 响应: `ApiResponseLong`

### GET /sys-user-notification/page
- 说明: 分页查询站内通知
- 描述: 按接收用户、通知类型与已读状态分页查询通知。
- 参数: `page`(query: integer) `size`(query: integer) `userId`(query: string) `type`(query: string) `isRead`(query: boolean)
- 响应: `ApiResponsePageSysUserNotification`

## NotificationPreference

### PUT /sys-user-notification-preference
- 说明: 更新通知偏好
- 描述: 更新用户通知偏好配置。
- 请求体: `SysUserNotificationPreference`
- 响应: `SysUserNotificationPreference`

### POST /sys-user-notification-preference
- 说明: 创建通知偏好
- 描述: 创建用户通知偏好并指定通知类型与通知渠道。
- 请求体: `SysUserNotificationPreference`
- 响应: `SysUserNotificationPreference`

### GET /sys-user-notification-preference/{id}
- 说明: 查询通知偏好
- 描述: 根据偏好ID查询用户通知偏好详情。
- 参数: `id`(path,必填: string)
- 响应: `SysUserNotificationPreference`

### DELETE /sys-user-notification-preference/{id}
- 说明: 删除通知偏好
- 描述: 根据偏好ID删除用户通知偏好。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-user-notification-preference/user/{userId}
- 说明: 查询用户通知偏好
- 描述: 查询指定用户的全部通知偏好。
- 参数: `userId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-user-notification-preference/page
- 说明: 分页查询通知偏好
- 描述: 按用户、通知类型与渠道分页查询用户通知偏好。
- 参数: `page`(query: integer) `size`(query: integer) `userId`(query: string) `notificationType`(query: string) `channel`(query: string)
- 响应: `PageSysUserNotificationPreference`

## NotificationTemplate

### PUT /sys-notification-template
- 说明: 更新通知模板
- 描述: 更新通知模板的内容与渠道配置。
- 请求体: `SysNotificationTemplate`
- 响应: `SysNotificationTemplate`

### POST /sys-notification-template
- 说明: 创建通知模板
- 描述: 创建通知模板并指定模板编码与通知渠道。
- 请求体: `SysNotificationTemplate`
- 响应: `SysNotificationTemplate`

### PUT /sys-notification-template/{id}/enable
- 说明: 启用通知模板
- 描述: 将指定通知模板状态置为启用。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### PUT /sys-notification-template/{id}/disable
- 说明: 禁用通知模板
- 描述: 将指定通知模板状态置为禁用。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-notification-template/{id}
- 说明: 查询通知模板
- 描述: 根据模板ID查询通知模板详情。
- 参数: `id`(path,必填: string)
- 响应: `SysNotificationTemplate`

### DELETE /sys-notification-template/{id}
- 说明: 删除通知模板
- 描述: 根据模板ID删除通知模板。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-notification-template/page
- 说明: 分页查询通知模板
- 描述: 按渠道、模板编码与状态分页查询通知模板。
- 参数: `page`(query: integer) `size`(query: integer) `channel`(query: string) `templateCode`(query: string) `status`(query: integer)
- 响应: `PageSysNotificationTemplate`

### GET /sys-notification-template/channel/{channel}
- 说明: 查询渠道通知模板
- 描述: 按通知渠道查询可用的模板列表。
- 参数: `channel`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## NotificationAudit

### POST /sys-notification-audit
- 说明: 创建通知审计
- 描述: 创建一条通知审计记录。
- 请求体: `SysNotificationAudit`
- 响应: `SysNotificationAudit`

### POST /sys-notification-audit/{id}/send
- 说明: 发送通知
- 描述: 根据审计记录ID触发对应通知的发送。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-notification-audit/{id}
- 说明: 查询通知审计
- 描述: 根据审计记录ID查询通知审计详情。
- 参数: `id`(path,必填: string)
- 响应: `SysNotificationAudit`

### DELETE /sys-notification-audit/{id}
- 说明: 删除通知审计
- 描述: 根据审计记录ID删除通知审计记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-notification-audit/user/{userId}
- 说明: 查询用户通知审计
- 描述: 查询指定用户的全部通知审计记录。
- 参数: `userId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-notification-audit/status/{status}
- 说明: 按状态查询通知审计
- 描述: 按通知状态查询通知审计记录。
- 参数: `status`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /sys-notification-audit/page
- 说明: 分页查询通知审计
- 描述: 按用户、渠道与状态分页查询通知审计记录。
- 参数: `page`(query: integer) `size`(query: integer) `userId`(query: string) `channel`(query: string) `status`(query: string)
- 响应: `PageSysNotificationAudit`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseBoolean** — `code`: integer, `message`: string, `data`: boolean, `timestamp`: integer
- **SysUserNotificationPreference** — `id`: string, `userId`: string, `notificationType`: string, `channel`: string, `enabled`: boolean, `quietHoursStart`: string, `quietHoursEnd`: string, `createTime`: string, `updateTime`: string
- **SysNotificationTemplate** — `id`: string, `templateCode`: string, `templateName`: string, `channel`: string, `subjectTemplate`: string, `contentTemplate`: string, `variablesSchema`: string, `status`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean
- **SysNotificationAudit** — `id`: string, `referenceId`: string, `userId`: string, `channel`: string, `status`: string, `subject`: string, `username`: string, `email`: string, `phone`: string, `templateCode`: string, `content`: string, `variables`: string, `errorMessage`: string, `retryCount`: integer, `maxRetries`: integer, `nextRetryTime`: string, `sentAt`: string, `createdAt`: string, `realName`: string
- **ApiResponseLong** — `code`: integer, `message`: string, `data`: integer, `timestamp`: integer
- **ApiResponsePageSysUserNotification** — `code`: integer, `message`: string, `data`: PageSysUserNotification, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageSysUserNotification** — `records`: SysUserNotification[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSysUserNotification, `searchCount`: PageSysUserNotification, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **SysUserNotification** — `tenantId`: string, `id`: string, `userId`: string, `title`: string, `content`: string, `type`: string, `isRead`: boolean, `link`: string, `readAt`: string, `createTime`: string, `updateTime`: string, `deleted`: boolean, `remark`: string
- **PageSysUserNotificationPreference** — `records`: SysUserNotificationPreference[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSysUserNotificationPreference, `searchCount`: PageSysUserNotificationPreference, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageSysNotificationTemplate** — `records`: SysNotificationTemplate[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSysNotificationTemplate, `searchCount`: PageSysNotificationTemplate, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageSysNotificationAudit** — `records`: SysNotificationAudit[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSysNotificationAudit, `searchCount`: PageSysNotificationAudit, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer