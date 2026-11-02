# scm-document — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8213/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-document (1.0.0)
- 认证: Bearer JWT (Authorization header)

## DocumentTemplate

### POST /api/v1/templates
- 说明: 创建模板
- 描述: 新建并保存一个文档模板。
- 请求体: `DocTemplate`
- 响应: `ApiResponseDocTemplate`

### GET /api/v1/templates/{templateId}/schema
- 说明: 查询模板变量 Schema
- 描述: 根据模板 ID 查询该模板的全部变量 Schema。
- 参数: `templateId`(path,必填: string)
- 响应: `ApiResponseListDocTemplateSchema`

### POST /api/v1/templates/{templateId}/schema
- 说明: 保存模板变量 Schema
- 描述: 为指定模板保存变量 Schema 定义。
- 参数: `templateId`(path,必填: string)
- 请求体: `DocTemplateSchema`
- 响应: `ApiResponseDocTemplateSchema`

### POST /api/v1/templates/render
- 说明: 渲染模板
- 描述: 按模板和变量数据渲染生成文档。
- 请求体: `RenderRequest`
- 响应: `ApiResponseRenderResult`

### GET /api/v1/templates/{id}
- 说明: 查询模板
- 描述: 根据模板 ID 查询文档模板详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseDocTemplate`

### GET /api/v1/templates/{id}/versions
- 说明: 查询模板版本
- 描述: 根据模板 ID 查询该模板的历史版本列表。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListObject`

### GET /api/v1/templates/page
- 说明: 分页查询模板
- 描述: 按名称/编码关键字、类型、分类和状态分页查询文档模板。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `templateType`(query: string) `category`(query: string) `status`(query: string)
- 响应: `ApiResponsePageDocTemplate`

## Document

### POST /api/v1/documents
- 说明: 创建文档
- 描述: 新建并保存一份文档。
- 请求体: `DocDocument`
- 响应: `ApiResponseDocDocument`

### GET /api/v1/documents/{id}
- 说明: 查询文档
- 描述: 根据文档 ID 查询文档详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseDocDocument`

### GET /api/v1/documents/page
- 说明: 分页查询文档
- 描述: 按名称/编码关键字、业务类型和状态分页查询文档。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `businessType`(query: string) `status`(query: string)
- 响应: `ApiResponsePageDocDocument`

## DocumentFile

### POST /api/v1/document-files
- 说明: 创建文档文件
- 描述: 保存文档文件元数据并绑定当前租户。
- 请求体: `DocumentFile`
- 响应: `ApiResponseDocumentFile`

### GET /api/v1/document-files/{id}
- 说明: 查询文档文件
- 描述: 根据文件 ID 查询当前租户下的文档文件元数据。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseDocumentFile`

### DELETE /api/v1/document-files/{id}
- 说明: 删除文档文件
- 描述: 根据文件 ID 删除文档文件记录。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/document-files/{id}/url
- 说明: 获取文件访问地址
- 描述: 根据文件 ID 生成文档文件的临时预签名访问地址。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseMapStringString`

### GET /api/v1/document-files/page
- 说明: 分页查询文档文件
- 描述: 按名称关键字、来源类型和关联业务 ID 分页查询当前租户的文档文件。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `sourceType`(query: string) `refId`(query: string)
- 响应: `ApiResponsePageDocumentFile`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **DocTemplate** — `id`: string, `templateCode`: string, `templateName`: string, `templateType`: string, `category`: string, `fileFormat`: string, `status`: string, `currentVersion`: integer, `description`: string, `tenantId`: string, `createdBy`: string, `createTime`: string, `updateTime`: string, `deleted`: integer
- **ApiResponseDocTemplate** — `code`: integer, `message`: string, `data`: DocTemplate, `timestamp`: integer
- **DocTemplateSchema** — `id`: string, `templateId`: string, `schemaCode`: string, `name`: string, `description`: string, `variables`: string, `status`: string, `tenantId`: string, `createdBy`: string, `createTime`: string
- **ApiResponseDocTemplateSchema** — `code`: integer, `message`: string, `data`: DocTemplateSchema, `timestamp`: integer
- **RenderRequest** — `templateVersionId`: string, `data`: object, `businessType`: string, `businessId`: string, `operatorId`: string, `operatorName`: string
- **ApiResponseRenderResult** — `code`: integer, `message`: string, `data`: RenderResult, `timestamp`: integer
- **RenderResult** — `documentId`: string, `documentVersion`: integer, `docCode`: string, `status`: string
- **DocDocument** — `id`: string, `docCode`: string, `docName`: string, `templateId`: string, `templateVersion`: integer, `currentVersion`: integer, `status`: string, `businessType`: string, `businessId`: string, `tenantId`: string, `createdBy`: string, `createTime`: string, `updateTime`: string, `deleted`: integer
- **ApiResponseDocDocument** — `code`: integer, `message`: string, `data`: DocDocument, `timestamp`: integer
- **DocumentFile** — `id`: string, `originalName`: string, `sourceType`: string, `refId`: string, `fileRef`: string, `tenantId`: string, `createBy`: string, `createTime`: string, `updateTime`: string, `deleted`: integer
- **ApiResponseDocumentFile** — `code`: integer, `message`: string, `data`: DocumentFile, `timestamp`: integer
- **ApiResponseListDocTemplateSchema** — `code`: integer, `message`: string, `data`: DocTemplateSchema[], `timestamp`: integer
- **ApiResponseListObject** — `code`: integer, `message`: string, `data`: any[], `timestamp`: integer
- **ApiResponsePageDocTemplate** — `code`: integer, `message`: string, `data`: PageDocTemplate, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageDocTemplate** — `records`: DocTemplate[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageDocTemplate, `searchCount`: PageDocTemplate, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponsePageDocDocument** — `code`: integer, `message`: string, `data`: PageDocDocument, `timestamp`: integer
- **PageDocDocument** — `records`: DocDocument[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageDocDocument, `searchCount`: PageDocDocument, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponsePageDocumentFile** — `code`: integer, `message`: string, `data`: PageDocumentFile, `timestamp`: integer
- **PageDocumentFile** — `records`: DocumentFile[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageDocumentFile, `searchCount`: PageDocumentFile, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer