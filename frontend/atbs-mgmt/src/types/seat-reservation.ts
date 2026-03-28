// 座位预订相关类型定义

/**
 * 座位预订基础类型（对应atbs_seat_reservation表）
 */
export interface SeatReservation {
  id: number                    // 座位预订号（bigint）
  scheduleId: number           // 班次号（bigint）- 外键关联atbs_schedule
  seatNum: number           // 座位号（int）
  bookingStatus: number        // 预订状态（int）- 0:可预订, 1:已预订, 2:已锁定
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 座位预订状态枚举
 */
export enum BookingStatus {
  AVAILABLE = 0,    // 可预订
  RESERVED = 1,     // 已预订
  LOCKED = 2        // 已锁定
}

/**
 * 座位预订状态标签映射
 */
export const BOOKING_STATUS_LABELS = {
  [BookingStatus.AVAILABLE]: '可预订',
  [BookingStatus.RESERVED]: '已预订',
  [BookingStatus.LOCKED]: '已锁定'
}

/**
 * 座位预订状态标签类型映射
 */
export const BOOKING_STATUS_TYPES = {
  [BookingStatus.AVAILABLE]: 'success',
  [BookingStatus.RESERVED]: 'warning'
}

/**
 * 座位预订列表类型（包含分页信息）
 */
export interface SeatReservationList extends SeatReservation {
  // 继承SeatReservation的所有属性
  scheduleInfo?: string          // 班次信息（如：班次号 + 列车名称）
  trainName?: string            // 列车名称
  routeInfo?: string            // 线路信息（出发站-到达站）
  departureTime?: string        // 出发时间
  arrivalTime?: string          // 到达时间
}

/**
 * 座位预订查询参数类型
 */
export interface SeatReservationQuery extends PageQuery {
  scheduleId?: number           // 班次ID
  seatNum?: number           // 座位号
  bookingStatus?: number        // 预订状态
  createTimeStart?: string      // 创建时间开始
  createTimeEnd?: string        // 创建时间结束
}

/**
 * 座位预订表单数据类型（用于新增/编辑）
 */
export interface SeatReservationForm {
  id?: number                   // 座位预订号（编辑时必填）
  scheduleId: number            // 班次号
  seatNum: number            // 座位号
  bookingStatus: number         // 预订状态
  createPerson?: string         // 创建人
  updatePerson?: string         // 更新者
}

/**
 * 班次选择项类型（用于下拉框）
 */
export interface ScheduleOption {
  value: number                 // 班次ID
  label: string                 // 班次显示信息
  trainName?: string           // 列车名称
  routeInfo?: string           // 线路信息
  departureTime?: string       // 出发时间
  arrivalTime?: string         // 到达时间
  availableTickets?: number    // 余票数
}

export interface SeatReservationChangeStatusREQ {
  ids: number[]                   // 座位预订号
  status: number               // 新的状态
}

/**
 * 座位状态统计类型
 */
export interface SeatStatusStats {
  total: number                 // 总座位数
  available: number            // 可预订座位数
  reserved: number             // 已预订座位数
}

/**
 * 分页查询基础类型
 */
export interface PageQuery {
  page: number                  // 当前页码
  pageSize: number             // 每页条数
}

/**
 * 分页结果类型
 */
export interface PageResult<T> {
  records: T[]                  // 数据列表
  total: number                // 总记录数
  page: number                 // 当前页码
  pageSize: number            // 每页条数
}

/**
 * API响应类型
 */
export interface ApiResponse {
  code: number                  // 响应码
  message: string              // 响应消息
  data?: any                   // 响应数据
}