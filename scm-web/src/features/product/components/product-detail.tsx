'use client'
import { Descriptions, Card, Tag, Table, Button, Space } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useProductDetail } from '../hooks'

const statusMap: Record<string, { color: string; label: string }> = {
  on_sale: { color: 'success', label: '在售' },
  off_sale: { color: 'error', label: '下架' },
  draft: { color: 'default', label: '草稿' },
}

interface ProductDetailProps { id: string }

export default function ProductDetail({ id }: ProductDetailProps) {
  const { data: product, isLoading } = useProductDetail(id)
  const router = useRouter()
  if (!product) return null

  const skuColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode' },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '价格', dataIndex: 'price', key: 'price', render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '库存', dataIndex: 'stock', key: 'stock' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'active' ? 'success' : 'error'}>{s === 'active' ? '启用' : '禁用'}</Tag> },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>商品详情</h2>
        <Space>
          <Button onClick={() => router.push('/product')}>返回</Button>
          <Button type="primary" icon={<EditOutlined />} onClick={() => router.push(`/product/${id}/edit`)}>编辑</Button>
        </Space>
      </div>
      <Card title="基本信息" loading={isLoading} style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="商品名称">{product.name}</Descriptions.Item>
          <Descriptions.Item label="商品编码">{product.code}</Descriptions.Item>
          <Descriptions.Item label="分类">{product.categoryName}</Descriptions.Item>
          <Descriptions.Item label="品牌">{product.brandName || '-'}</Descriptions.Item>
          <Descriptions.Item label="售价">¥{product.price?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="成本价">{product.costPrice ? `¥${product.costPrice.toLocaleString()}` : '-'}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={statusMap[product.status]?.color}>{statusMap[product.status]?.label}</Tag></Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>{product.description || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="SKU列表">
        <Table columns={skuColumns} dataSource={product.skus || []} rowKey="id" pagination={false} size="small" />
      </Card>
    </div>
  )
}
