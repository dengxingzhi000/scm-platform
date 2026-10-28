import type { Supplier, SupplierSearchParams } from '../types'

const mockSuppliers: Supplier[] = [
  { id: '1', name: 'Apple供应链', code: 'SUP-001', contactPerson: '张经理', contactPhone: '13800000001', email: 'zhang@apple-supply.com', address: '上海市浦东新区世纪大道 88 号', status: 'active', rating: 5, createdAt: '2025-01-01' },
  { id: '2', name: 'Samsung代理', code: 'SUP-002', contactPerson: '李经理', contactPhone: '13800000002', email: 'li@samsung-agent.com', address: '北京市朝阳区建国路 99 号', status: 'active', rating: 4, createdAt: '2025-02-01' },
  { id: '3', name: 'Huawei授权', code: 'SUP-003', contactPerson: '王经理', contactPhone: '13800000003', email: 'wang@huawei-supply.com', address: '深圳市龙岗区坂田华为基地', status: 'disabled', rating: 3, createdAt: '2025-03-01' },
  { id: '4', name: '深圳华强电子', code: 'SUP-004', contactPerson: '陈经理', contactPhone: '13800000004', email: 'chen@hqelectronic.com', address: '深圳市福田区华强北路 1002 号', status: 'active', rating: 5, createdAt: '2025-04-12' },
  { id: '5', name: '上海联创供应链', code: 'SUP-005', contactPerson: '周经理', contactPhone: '13800000005', email: 'zhou@lianchuang.com', address: '上海市闵行区虹梅路 2588 号', status: 'active', rating: 4, createdAt: '2025-05-08' },
  { id: '6', name: '广州捷普物流', code: 'SUP-006', contactPerson: '吴经理', contactPhone: '13800000006', email: 'wu@jabil-logistics.com', address: '广州市黄埔区科学大道 182 号', status: 'active', rating: 4, createdAt: '2025-06-18' },
  { id: '7', name: '杭州锐迈科技', code: 'SUP-007', contactPerson: '郑经理', contactPhone: '13800000007', email: 'zheng@ruimai-tech.com', address: '杭州市余杭区文一西路 969 号', status: 'disabled', rating: 3, createdAt: '2025-07-22' },
  { id: '8', name: '苏州精工制造', code: 'SUP-008', contactPerson: '孙经理', contactPhone: '13800000008', email: 'sun@jinggong-mfg.com', address: '苏州市工业园区星湖街 328 号', status: 'active', rating: 5, createdAt: '2025-08-30' },
]

export const supplierService = {
  list: async (params?: SupplierSearchParams) => {
    let filtered = [...mockSuppliers]
    if (params?.keyword) filtered = filtered.filter(s => s.name.includes(params.keyword!) || s.code.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(s => s.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<Supplier> => {
    const s = mockSuppliers.find(x => x.id === id)
    if (!s) throw new Error('Supplier not found')
    return s
  },
}
