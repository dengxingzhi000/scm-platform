# scm-file — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8214/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-file (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Ocr

### POST /file/ocr/recognize
- 说明: 识别图片文字
- 描述: 识别上传图片中的文字并返回识别结果。
- 响应: `ApiResponseString`

### POST /file/ocr/invoice
- 说明: 提取发票信息
- 描述: 识别发票图片并提取结构化发票信息。
- 响应: `ApiResponseInvoiceInfo`

### POST /file/ocr/contract
- 说明: 提取合同信息
- 描述: 识别合同图片并提取结构化合同信息。
- 响应: `ApiResponseContractInfo`

## File

### POST /api/v1/files/upload
- 说明: 上传文件
- 描述: 上传文件并返回文件的元数据信息。
- 参数: `bizType`(query: string) `bizId`(query: string)
- 响应: `ApiResponseFileMetadataDTO`

### GET /api/v1/files/{id}/url
- 说明: 获取文件访问地址
- 描述: 根据文件 ID 生成临时预签名访问地址。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseMapStringString`

### GET /api/v1/files/{id}/content
- 说明: 下载文件
- 描述: 根据文件 ID 输出文件内容流供浏览器下载。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /api/v1/files/page
- 说明: 分页查询文件
- 描述: 按名称关键字、业务类型和业务 ID 分页查询当前租户的文件元数据。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `bizType`(query: string) `bizId`(query: string)
- 响应: `ApiResponsePageFileMetadataDTO`

### GET /api/v1/files/check/{md5}
- 说明: 校验文件是否存在
- 描述: 根据文件 MD5 校验当前租户下文件是否已存在，用于秒传。
- 参数: `md5`(path,必填: string)
- 响应: `ApiResponseFileMetadataDTO`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseString** — `code`: integer, `message`: string, `data`: string, `timestamp`: integer
- **ApiResponseInvoiceInfo** — `code`: integer, `message`: string, `data`: InvoiceInfo, `timestamp`: integer
- **InvoiceInfo** — `invoiceNo`: string, `invoiceDate`: string, `amount`: string, `buyerName`: string, `sellerName`: string, `rawText`: string
- **ApiResponseContractInfo** — `code`: integer, `message`: string, `data`: ContractInfo, `timestamp`: integer
- **ContractInfo** — `contractNo`: string, `partyA`: string, `partyB`: string, `amount`: string, `signDate`: string, `rawText`: string
- **ApiResponseFileMetadataDTO** — `code`: integer, `message`: string, `data`: FileMetadataDTO, `timestamp`: integer
- **FileMetadataDTO** — `id`: string, `originalName`: string, `storageKey`: string, `contentType`: string, `fileSize`: integer, `storageEngine`: string, `md5`: string, `version`: integer, `bizType`: string, `bizId`: string, `status`: string, `refCount`: integer, `tenantId`: string, `createBy`: string, `updateBy`: integer, `createTime`: string, `updateTime`: string
- **ApiResponsePageFileMetadataDTO** — `code`: integer, `message`: string, `data`: PageFileMetadataDTO, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageFileMetadataDTO** — `records`: FileMetadataDTO[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageFileMetadataDTO, `searchCount`: PageFileMetadataDTO, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer