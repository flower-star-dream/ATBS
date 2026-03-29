// 机票相关类型定义

/**
 * 机票状态枚举
 */
export enum TicketStatus {
  NORMAL = 1,
  USED = 2,
  CANCELLED = 3,
  CHANGED = 4,
  REFUNDED = 5
}

/**
 * 机票状态映射
 */
export const TicketStatusMap = {
  [TicketStatus.NORMAL]: { text: '正常', type: 'success' },
  [TicketStatus.USED]: { text: '已使用', type: 'info' },
  [TicketStatus.CANCELLED]: { text: '已取消', type: 'danger' },
  [TicketStatus.CHANGED]: { text: '已改签', type: 'warning' },
  [TicketStatus.REFUNDED]: { text: '已退票', type: 'info' }
}

/**
 * 座位类型枚举
 */
export enum SeatType {
  FIRST_CLASS = '头等舱',
  BUSINESS = '商务舱',
  ECONOMY = '经济舱'
}

/**
 * 机票列表查询请求参数
 */
export interface TicketPageQueryREQ {
  page: number
  pageSize: number
  orderId?: string
  realName?: string
  flightNumber?: string
  startStation?: string
  endStation?: string
  status?: number
  rideDateStart?: string
  rideDateEnd?: string
}

/**
 * 机票响应数据
 */
export interface Ticket {
  id: string
  orderId: string
  orderNumber: string
  realName: string
  cardType: string
  idCard: string
  seatNumber: string
  seatType: string
  flightNumber: string
  startStation: string
  endStation: string
  startTime: string
  endTime: string
  duration: string
  status: TicketStatus
  money: number
  createTime: string
  updateTime: string
  createPerson: string
  updatePerson: string
}

/**
 * 机票详情响应数据
 */
export interface TicketDetail extends Ticket {
  userId: string
  username: string
  phone: string
  orderStatus: number
  payTime: string
}

/**
 * 机票状态更新请求
 */
export interface TicketStatusUpdateREQ {
  id: string
  status: TicketStatus
  flightId?: string
  startStationId?: string
  endStationId?: string
}
