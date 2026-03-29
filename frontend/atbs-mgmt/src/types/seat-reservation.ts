// 座位预订相关类型定义
import type { PageQuery } from './index'

/**
 * 座位预订状态枚举
 */
export enum BookingStatus {
  AVAILABLE = 0,
  RESERVED = 1,
  LOCKED = 2
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
 * 座位预订基础类型（对应atbs_seat_reservation表）
 */
export interface SeatReservation {
  id: number
  flightId: number
  seatNum: number
  bookingStatus: number
  createTime?: string
  updateTime?: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 座位预订列表类型（包含分页信息）
 */
export interface SeatReservationList extends SeatReservation {
  flightInfo?: string
  aircraftName?: string
  routeInfo?: string
  departureTime?: string
  arrivalTime?: string
}

/**
 * 座位预订查询参数类型
 */
export interface SeatReservationQuery extends PageQuery {
  flightId?: number
  seatNum?: number
  bookingStatus?: number
  createTimeStart?: string
  createTimeEnd?: string
}

/**
 * 座位预订表单数据类型（用于新增/编辑）
 */
export interface SeatReservationForm {
  id?: number
  flightId: number
  seatNum: number
  bookingStatus: number
  createPerson?: string
  updatePerson?: string
}

/**
 * 航班选择项类型（用于下拉框）
 */
export interface FlightOption {
  value: number
  label: string
  aircraftName?: string
  routeInfo?: string
  departureTime?: string
  arrivalTime?: string
  availableTickets?: number
}

/**
 * 座位预订状态变更请求
 */
export interface SeatReservationChangeStatusREQ {
  ids: number[]
  status: number
}

/**
 * 座位状态统计类型
 */
export interface SeatStatusStats {
  total: number
  available: number
  reserved: number
}
