import type { Waybill, TrackingEvent } from '../types'

const mockWaybills: Waybill[] = [
  { id: '1', waybillNo: 'WB-2026-001', orderNo: 'ORD-2026-001', carrierName: '顺丰速运', trackingNo: 'SF1234567890', status: 'delivered', statusLabel: '已送达', senderName: '主仓库', senderAddress: '上海市浦东新区', receiverName: '张三', receiverAddress: '北京市朝阳区', createdAt: '2026-06-02' },
  { id: '2', waybillNo: 'WB-2026-002', orderNo: 'ORD-2026-002', carrierName: '京东物流', trackingNo: 'JD0987654321', status: 'in_transit', statusLabel: '运输中', senderName: '主仓库', senderAddress: '上海市浦东新区', receiverName: '李四', receiverAddress: '广州市天河区', createdAt: '2026-06-08' },
  { id: '3', waybillNo: 'WB-2026-003', orderNo: 'ORD-2026-003', carrierName: '京东物流', trackingNo: 'JD1122334455', status: 'picked', statusLabel: '已揽收', senderName: '华东仓', senderAddress: '杭州市余杭区', receiverName: '王五', receiverAddress: '成都市高新区', createdAt: '2026-06-09' },
  { id: '4', waybillNo: 'WB-2026-004', orderNo: 'ORD-2026-004', carrierName: '中通快递', trackingNo: 'ZT5566778899', status: 'exception', statusLabel: '异常', senderName: '华南仓', senderAddress: '广州市黄埔区', receiverName: '赵六', receiverAddress: '武汉市洪山区', createdAt: '2026-06-07' },
  { id: '5', waybillNo: 'WB-2026-005', orderNo: 'ORD-2026-005', carrierName: '圆通速递', trackingNo: 'YT9988776655', status: 'pending', statusLabel: '待揽收', senderName: '华北仓', senderAddress: '北京市大兴区', receiverName: '钱七', receiverAddress: '西安市雁塔区', createdAt: '2026-06-10' },
  { id: '6', waybillNo: 'WB-2026-006', orderNo: 'ORD-2026-006', carrierName: '顺丰速运', trackingNo: 'SF6677889900', status: 'in_transit', statusLabel: '运输中', senderName: '主仓库', senderAddress: '上海市浦东新区', receiverName: '孙八', receiverAddress: '南京市鼓楼区', createdAt: '2026-06-11' },
]

const mockTracking: TrackingEvent[] = [
  { id: '1', time: '2026-06-02 14:00', location: '上海市', description: '已发货', status: 'picked' },
  { id: '2', time: '2026-06-03 08:00', location: '南京市', description: '运输中', status: 'in_transit' },
  { id: '3', time: '2026-06-04 10:00', location: '北京市', description: '已到达', status: 'in_transit' },
  { id: '4', time: '2026-06-05 16:30', location: '北京市朝阳区', description: '已签收', status: 'delivered' },
]

export const logisticsService = {
  listWaybills: async () => mockWaybills,
  getTracking: async (waybillId: string) => mockTracking,
}
