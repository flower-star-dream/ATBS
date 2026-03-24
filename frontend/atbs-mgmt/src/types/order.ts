// 订单状态枚举
export enum OrderStatus {
  PENDING = 0,      // 待支付
  PAID = 1,         // 已支付
  TICKETED = 2,     // 已出票
  COMPLETED = 3,    // 已完成
  CANCELLED = 4,    // 已取消
  REFUNDED = 5      // 已退款
}

// 订单状态映射
export const OrderStatusMap = {
  [OrderStatus.PENDING]: { text: '待支付', type: 'warning' },
  [OrderStatus.PAID]: { text: '已支付', type: 'success' },
  [OrderStatus.TICKETED]: { text: '已出票', type: 'success' },
  [OrderStatus.COMPLETED]: { text: '已完成', type: 'success' },
  [OrderStatus.CANCELLED]: { text: '已取消', type: 'info' },
  [OrderStatus.REFUNDED]: { text: '已退款', type: 'info' }
}

// 订单列表查询请求参数
export interface OrderPageQueryREQ {
  page: number
  pageSize: number
  userId?: string        // 用户ID
  username?: string      // 用户名（模糊查询）
  status?: number        // 订单状态
  id?: string            // 订单号
  startTime?: string     // 开始时间
  endTime?: string       // 结束时间
}

// 订单响应数据
export interface Order {
  id: string                    // 订单ID
  orderNumber: string           // 订单号
  userId: string               // 用户ID
  username: string             // 用户名
  status: OrderStatus          // 订单状态
  remarks: string              // 订单备注
  totalPrice: number           // 订单总价
  payTime?: string             // 支付时间
  createTime: string           // 创建时间
  updateTime: string           // 更新时间
  createPerson: string         // 创建人
  updatePerson: string         // 更新人
}

// 订单详情响应数据
export interface OrderDetail extends Order {
  tickets: OrderTicket[]            // 关联的车票列表
  passengers: OrderPassenger[]      // 关联的乘客信息
}

// 简化的车票信息（用于订单详情）
export interface OrderTicket {
  id: string
  trainNumber: string          // 车次号
  departure: string            // 出发站
  arrival: string              // 到达站
  departureTime: string        // 出发时间
  arrivalTime: string          // 到达时间
  seatNumber: string           // 座位号
  seatType: string             // 座位类型
  price: number                // 票价
  status: number               // 车票状态
  passengerName: string        // 乘客姓名
}

// 简化的乘客信息（用于订单详情）
export interface OrderPassenger {
  id: string
  realName: string             // 真实姓名
  cardType: string             // 证件类型
  idCard: string               // 证件号码
}

// 订单状态更新请求
export interface OrderStatusUpdateREQ {
  id: string                   // 订单ID
  status: OrderStatus          // 新状态
  remarks?: string             // 备注（可选）
}