# scm-product — OpenAPI 文档摘要

- 来源: `http://192.168.0.125:8201/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-product (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Spu

### GET /api/v1/prod-spu/{id}
- 说明: 查询 SPU 详情
- 描述: 根据SPU ID查询商品SPU的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseProdSpu`

### PUT /api/v1/prod-spu/{id}
- 说明: 更新 SPU
- 描述: 根据SPU ID更新商品SPU信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `ProdSpu`
- 响应: `ApiResponseVoid`

### DELETE /api/v1/prod-spu/{id}
- 说明: 删除 SPU
- 描述: 根据SPU ID逻辑删除商品SPU并将其状态置为删除。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-spu
- 说明: 分页查询 SPU
- 描述: 按关键词、分类、品牌与状态组合条件分页搜索商品SPU。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `categoryId`(query: string) `brandId`(query: string) `status`(query: integer)
- 响应: `ApiResponsePageResultProdSpu`

### POST /api/v1/prod-spu
- 说明: 创建 SPU
- 描述: 新增商品SPU，自动生成ID与时间字段，未指定状态时默认为草稿。
- 请求体: `ProdSpu`
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-spu/category/{categoryId}
- 说明: 按分类查询 SPU
- 描述: 查询指定商品分类下的全部SPU列表。
- 参数: `categoryId`(path,必填: string)
- 响应: `ApiResponseListProdSpu`

### GET /api/v1/prod-spu/brand/{brandId}
- 说明: 按品牌查询 SPU
- 描述: 查询指定品牌下的全部SPU列表。
- 参数: `brandId`(path,必填: string)
- 响应: `ApiResponseListProdSpu`

## Sku

### GET /api/v1/prod-sku/{id}
- 说明: 查询 SKU 详情
- 描述: 根据SKU ID查询SKU的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseProdSku`

### PUT /api/v1/prod-sku/{id}
- 说明: 更新 SKU
- 描述: 根据SKU ID更新SKU信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `ProdSku`
- 响应: `ApiResponseVoid`

### DELETE /api/v1/prod-sku/{id}
- 说明: 删除 SKU
- 描述: 根据SKU ID逻辑删除SKU并将其状态置为删除。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-sku
- 说明: 分页查询 SKU
- 描述: 分页查询未删除且未标记删除状态的SKU，可按所属SPU筛选。
- 参数: `page`(query: integer) `size`(query: integer) `spuId`(query: string)
- 响应: `ApiResponsePageResultProdSku`

### POST /api/v1/prod-sku
- 说明: 创建 SKU
- 描述: 新增SKU，自动生成ID与时间字段，未指定状态时默认为启用。
- 请求体: `ProdSku`
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-sku/spu/{spuId}
- 说明: 按 SPU 查询 SKU
- 描述: 查询指定SPU下的全部SKU列表。
- 参数: `spuId`(path,必填: string)
- 响应: `ApiResponseListProdSku`

## Category

### GET /api/v1/prod-category/{id}
- 说明: 查询分类详情
- 描述: 根据分类ID查询商品分类的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseProdCategory`

### PUT /api/v1/prod-category/{id}
- 说明: 更新分类
- 描述: 根据分类ID更新商品分类信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `ProdCategory`
- 响应: `ApiResponseVoid`

### DELETE /api/v1/prod-category/{id}
- 说明: 删除分类
- 描述: 根据分类ID逻辑删除商品分类。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-category
- 说明: 分页查询分类
- 描述: 按分类名称模糊筛选并分页查询未删除的商品分类，结果按排序号升序排列。
- 参数: `page`(query: integer) `size`(query: integer) `name`(query: string)
- 响应: `ApiResponsePageResultProdCategory`

### POST /api/v1/prod-category
- 说明: 创建分类
- 描述: 新增商品分类，自动生成ID并初始化创建与更新时间。
- 请求体: `ProdCategory`
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-category/tree
- 说明: 查询分类树
- 描述: 查询完整的商品分类树形结构。
- 响应: `ApiResponseListProdCategory`

### GET /api/v1/prod-category/search
- 说明: 搜索分类
- 描述: 根据分类名称模糊搜索商品分类列表。
- 参数: `name`(query: string)
- 响应: `ApiResponseListProdCategory`

### GET /api/v1/prod-category/children/{parentId}
- 说明: 查询子分类
- 描述: 查询指定父分类下的直接子分类列表。
- 参数: `parentId`(path,必填: string)
- 响应: `ApiResponseListProdCategory`

## Brand

### GET /api/v1/prod-brand/{id}
- 说明: 查询品牌详情
- 描述: 根据品牌ID查询品牌的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseProdBrand`

### PUT /api/v1/prod-brand/{id}
- 说明: 更新品牌
- 描述: 根据品牌ID更新品牌信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `ProdBrand`
- 响应: `ApiResponseVoid`

### DELETE /api/v1/prod-brand/{id}
- 说明: 删除品牌
- 描述: 根据品牌ID逻辑删除品牌。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-brand
- 说明: 分页查询品牌
- 描述: 按品牌名称模糊筛选并分页查询未删除的品牌，结果按排序号升序排列。
- 参数: `page`(query: integer) `size`(query: integer) `name`(query: string)
- 响应: `ApiResponsePageResultProdBrand`

### POST /api/v1/prod-brand
- 说明: 创建品牌
- 描述: 新增品牌，自动生成ID并初始化创建与更新时间。
- 请求体: `ProdBrand`
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-brand/search
- 说明: 搜索品牌
- 描述: 根据品牌名称模糊搜索品牌列表。
- 参数: `name`(query: string)
- 响应: `ApiResponseListProdBrand`

### GET /api/v1/prod-brand/featured
- 说明: 查询推荐品牌
- 描述: 查询已设置为推荐的全部品牌列表。
- 响应: `ApiResponseListProdBrand`

## AttributeTemplate

### GET /api/v1/prod-attribute-template/{id}
- 说明: 查询属性模板详情
- 描述: 根据模板ID查询属性模板的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseProdAttributeTemplate`

### PUT /api/v1/prod-attribute-template/{id}
- 说明: 更新属性模板
- 描述: 根据模板ID更新属性模板内容并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `ProdAttributeTemplate`
- 响应: `ApiResponseVoid`

### DELETE /api/v1/prod-attribute-template/{id}
- 说明: 删除属性模板
- 描述: 根据模板ID逻辑删除属性模板。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-attribute-template
- 说明: 分页查询属性模板
- 描述: 按分类筛选并分页查询未删除的属性模板列表。
- 参数: `page`(query: integer) `size`(query: integer) `categoryId`(query: string)
- 响应: `ApiResponsePageResultProdAttributeTemplate`

### POST /api/v1/prod-attribute-template
- 说明: 创建属性模板
- 描述: 新增属性模板，自动生成ID并初始化创建与更新时间。
- 请求体: `ProdAttributeTemplate`
- 响应: `ApiResponseVoid`

### GET /api/v1/prod-attribute-template/category/{categoryId}
- 说明: 按分类查询属性模板
- 描述: 查询指定商品分类下配置的全部属性模板。
- 参数: `categoryId`(path,必填: string)
- 响应: `ApiResponseListProdAttributeTemplate`

## ProductSearch

### GET /api/v1/products/search
- 说明: 快速搜索商品
- 描述: 以 GET 方式按关键词快速搜索商品，适用于简单查询场景。
- 参数: `keyword`(query: string) `page`(query: integer) `size`(query: integer) `sortBy`(query: string) `sortOrder`(query: string)
- 响应: `ApiResponsePageProductSearchResponse`

### POST /api/v1/products/search
- 说明: 综合搜索商品
- 描述: 支持关键词、分类、品牌、价格区间与排序条件的商品分页搜索。
- 请求体: `ProductSearchRequest`
- 响应: `ApiResponsePageProductSearchResponse`

### GET /api/v1/products/search/latest
- 说明: 查询最新商品
- 描述: 按发布时间倒序分页查询最新商品列表。
- 参数: `page`(query: integer) `size`(query: integer)
- 响应: `ApiResponsePageProductSearchResponse`

### GET /api/v1/products/search/hot
- 说明: 查询热门商品
- 描述: 按销量倒序分页查询热门商品列表。
- 参数: `page`(query: integer) `size`(query: integer)
- 响应: `ApiResponsePageProductSearchResponse`

### GET /api/v1/products/search/category/{categoryId}
- 说明: 按分类查询商品
- 描述: 分页查询指定商品分类下的商品，结果按销量排序。
- 参数: `categoryId`(path,必填: string) `page`(query: integer) `size`(query: integer)
- 响应: `ApiResponsePageProductSearchResponse`

### GET /api/v1/products/search/brand/{brandId}
- 说明: 按品牌查询商品
- 描述: 分页查询指定品牌下的商品，结果按销量排序。
- 参数: `brandId`(path,必填: string) `page`(query: integer) `size`(query: integer)
- 响应: `ApiResponsePageProductSearchResponse`

## Schemas

- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ProdSpu** — `tenantId`: string, `id`: string, `spuCode`: string, `spuName`: string, `categoryId`: string, `brandId`: string, `description`: string, `detailHtml`: string, `images`: string, `mainImage`: string, `videoUrl`: string, `minPrice`: number, `maxPrice`: number, `totalStock`: integer, `totalSales`: integer, `sortOrder`: integer, `seoTitle`: string, `seoKeywords`: string, `seoDescription`: string, `status`: integer, `publishedAt`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ProdSku** — `tenantId`: string, `id`: string, `spuId`: string, `skuCode`: string, `skuName`: string, `attributes`: string, `originalPrice`: number, `sellingPrice`: number, `costPrice`: number, `stock`: integer, `availableStock`: integer, `lockedStock`: integer, `salesCount`: integer, `weight`: number, `volume`: number, `barcode`: string, `imageUrl`: string, `images`: string, `status`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ProdCategory** — `tenantId`: string, `id`: string, `categoryCode`: string, `categoryName`: string, `parentId`: string, `level`: integer, `sortOrder`: integer, `iconUrl`: string, `imageUrl`: string, `description`: string, `isLeaf`: boolean, `enabled`: boolean, `seoTitle`: string, `seoKeywords`: string, `seoDescription`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ProdBrand** — `tenantId`: string, `id`: string, `brandCode`: string, `brandName`: string, `brandNameEn`: string, `logoUrl`: string, `description`: string, `website`: string, `country`: string, `establishedYear`: integer, `featured`: boolean, `sortOrder`: integer, `enabled`: boolean, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ProdAttributeTemplate** — `tenantId`: string, `id`: string, `templateName`: string, `categoryId`: string, `attributes`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ProductSearchRequest** — `keyword`: string, `categoryId`: string, `brandId`: string, `minPrice`: number, `maxPrice`: number, `sortBy`: string, `sortOrder`: string, `page`: integer, `size`: integer
- **ApiResponsePageProductSearchResponse** — `code`: integer, `message`: string, `data`: PageProductSearchResponse, `timestamp`: integer
- **PageProductSearchResponse** — `totalPages`: integer, `totalElements`: integer, `size`: integer, `content`: ProductSearchResponse[], `number`: integer, `sort`: SortObject, `pageable`: PageableObject, `numberOfElements`: integer, `first`: boolean, `last`: boolean, `empty`: boolean
- **PageableObject** — `offset`: integer, `paged`: boolean, `pageNumber`: integer, `pageSize`: integer, `sort`: SortObject, `unpaged`: boolean
- **ProductSearchResponse** — `id`: string, `spuCode`: string, `spuName`: string, `categoryId`: string, `categoryName`: string, `brandId`: string, `brandName`: string, `description`: string, `mainImage`: string, `minPrice`: number, `maxPrice`: number, `totalStock`: integer, `totalSales`: integer, `sortOrder`: integer, `status`: integer, `publishedAt`: string, `createTime`: string, `updateTime`: string
- **SortObject** — `empty`: boolean, `sorted`: boolean, `unsorted`: boolean
- **ApiResponsePageResultProdSpu** — `code`: integer, `message`: string, `data`: PageResultProdSpu, `timestamp`: integer
- **PageResultProdSpu** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: ProdSpu[]
- **ApiResponseProdSpu** — `code`: integer, `message`: string, `data`: ProdSpu, `timestamp`: integer
- **ApiResponseListProdSpu** — `code`: integer, `message`: string, `data`: ProdSpu[], `timestamp`: integer
- **ApiResponsePageResultProdSku** — `code`: integer, `message`: string, `data`: PageResultProdSku, `timestamp`: integer
- **PageResultProdSku** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: ProdSku[]
- **ApiResponseProdSku** — `code`: integer, `message`: string, `data`: ProdSku, `timestamp`: integer
- **ApiResponseListProdSku** — `code`: integer, `message`: string, `data`: ProdSku[], `timestamp`: integer
- **ApiResponsePageResultProdCategory** — `code`: integer, `message`: string, `data`: PageResultProdCategory, `timestamp`: integer
- **PageResultProdCategory** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: ProdCategory[]
- **ApiResponseProdCategory** — `code`: integer, `message`: string, `data`: ProdCategory, `timestamp`: integer
- **ApiResponseListProdCategory** — `code`: integer, `message`: string, `data`: ProdCategory[], `timestamp`: integer
- **ApiResponsePageResultProdBrand** — `code`: integer, `message`: string, `data`: PageResultProdBrand, `timestamp`: integer
- **PageResultProdBrand** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: ProdBrand[]
- **ApiResponseProdBrand** — `code`: integer, `message`: string, `data`: ProdBrand, `timestamp`: integer
- **ApiResponseListProdBrand** — `code`: integer, `message`: string, `data`: ProdBrand[], `timestamp`: integer
- **ApiResponsePageResultProdAttributeTemplate** — `code`: integer, `message`: string, `data`: PageResultProdAttributeTemplate, `timestamp`: integer
- **PageResultProdAttributeTemplate** — `total`: integer, `pageNum`: integer, `pageSize`: integer, `pages`: integer, `records`: ProdAttributeTemplate[]
- **ApiResponseProdAttributeTemplate** — `code`: integer, `message`: string, `data`: ProdAttributeTemplate, `timestamp`: integer
- **ApiResponseListProdAttributeTemplate** — `code`: integer, `message`: string, `data`: ProdAttributeTemplate[], `timestamp`: integer