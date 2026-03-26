// 车票状态枚举
export enum TicketStatus {
  NORMAL = 1,       // 正常
  USED = 2,         // 已使用
  CANCELLED = 3,    // 已取消
  CHANGED = 4,      // 已改签
  REFUNDED = 5      // 已退票
}

// 车票状态映射
export const TicketStatusMap = {
  [TicketStatus.NORMAL]: { text: '正常', type: 'success' },
  [TicketStatus.USED]: { text: '已使用', type: 'info' },
  [TicketStatus.CANCELLED]: { text: '已取消', type: 'danger' },
  [TicketStatus.CHANGED]: { text: '已改签', type: 'warning' },
  [TicketStatus.REFUNDED]: { text: '已退票', type: 'info' }
}

// 座位类型枚举
export enum SeatType {
  BUSINESS = '商务座',
  FIRST_CLASS = '一等座',
  SECOND_CLASS = '二等座',
  HARD_SEAT = '硬座',
  SOFT_SEAT = '软座',
  HARD_SLEEPER = '硬卧',
  SOFT_SLEEPER = '软卧'
}

// 车票列表查询请求参数
export interface TicketPageQueryREQ {
  page: number
  pageSize: number
  orderId?: string         // 订单ID
  realName?: string        // 乘车人姓名（模糊查询）
  trainNumber?: string     // 车次号（模糊查询）
  startStation?: string    // 出发站
  endStation?: string      // 到达站
  status?: number          // 车票状态
  rideDateStart?: string   // 乘车日期范围-开始
  rideDateEnd?: string     // 乘车日期范围-结束
}

// 车票响应数据
export interface Ticket {
  id: string                    // 车票ID
  orderId: string              // 订单ID
  orderNumber: string          // 订单号
  realName: string             // 乘车人姓名
  cardType: string             // 证件类型
  idCard: string               // 证件号码（脱敏）
  seatNumber: string           // 座位号
  seatType: string             // 座位类型
  trainNumber: string          // 车次号
  startStation: string         // 出发站
  endStation: string            // 到达站
  startTime: string             // 出发时间
  endTime: string                // 到达时间
  duration: string             // 行程时长
  status: TicketStatus         // 车票状态
  money: number                // 票价
  createTime: string           // 创建时间
  updateTime: string           // 更新时间
  createPerson: string         // 创建人
  updatePerson: string         // 更新人
}

// 车票详情响应数据
export interface TicketDetail extends Ticket {
  userId: string               // 用户ID
  username: string             // 用户名
  phone: string                // 用户手机号
  orderStatus: number          // 订单状态
  payTime: string              // 支付时间
}

// 车票状态更新请求
export interface TicketStatusUpdateREQ {
  id: string                   // 车票ID
  status: TicketStatus         // 新状态
  scheduleId?: string          // 班次ID
  startStationId?: string      // 出发站ID
  endStationId?: string        // 到达站ID
}
