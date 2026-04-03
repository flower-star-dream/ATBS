// 订单相关类型定义

/**
 * 订单状态枚举
 */
export enum OrderStatus {
  PENDING = 0,
  PAID = 1,
  TICKETED = 2,
  COMPLETED = 3,
  CANCELLED = 4,
  REFUNDED = 5
}

/**
 * 订单状态映射
 */
export const OrderStatusMap = {
  [OrderStatus.PENDING]: { text: '待支付', type: 'warning' },
  [OrderStatus.PAID]: { text: '已支付', type: 'success' },
  [OrderStatus.TICKETED]: { text: '已出票', type: 'success' },
  [OrderStatus.COMPLETED]: { text: '已完成', type: 'success' },
  [OrderStatus.CANCELLED]: { text: '已取消', type: 'info' },
  [OrderStatus.REFUNDED]: { text: '已退款', type: 'info' }
}

/**
 * 订单列表查询请求参数
 */
export interface OrderPageQueryREQ {
  page: number
  pageSize: number
  userId?: string
  username?: string
  status?: number
  id?: string
  startTime?: string
  endTime?: string
}

/**
 * 订单响应数据
 */
export interface Order {
  id: string
  orderNumber: string
  userId: string
  username: string
  status: OrderStatus
  remarks: string
  totalPrice: number
  payTime?: string
  createTime: string
  updateTime: string
  createPerson: string
  updatePerson: string
}

/**
 * 简化的机票信息（用于订单详情）
 */
export interface OrderTicket {
  id: string
  scheduleNumber: string
  departure: string
  arrival: string
  departureTime: string
  arrivalTime: string
  seatNumber: string
  seatType: string
  price: number
  status: number
  passengerName: string
}

/**
 * 简化的乘客信息（用于订单详情）
 */
export interface OrderPassenger {
  id: string
  realName: string
  cardType: string
  idCard: string
}

/**
 * 订单详情响应数据
 */
export interface OrderDetail extends Order {
  tickets: OrderTicket[]
  passengers: OrderPassenger[]
}

/**
 * 订单状态更新请求
 */
export interface OrderStatusUpdateREQ {
  id: string
  status: OrderStatus
  remarks?: string
}
