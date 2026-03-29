// 航班相关类型定义
import type { PageQuery } from './index'

/**
 * 航班基础类型（对应atbs_flight表）
 */
export interface Flight {
  id: number
  aircraftId: number
  routeId: number
  captain: string
  availableTickets: number
  startTime: string
  endTime: string
  createTime?: string
  updateTime?: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 航班列表类型（包含分页信息）
 */
export interface FlightList extends Flight {
  aircraftName?: string
  routeName?: string
  routeInfo?: string
}

/**
 * 航班查询参数类型
 */
export interface FlightQuery extends PageQuery {
  aircraftId?: number
  routeId?: number
  captain?: string
  startTimeStart?: string
  startTimeEnd?: string
  availableTicketsMin?: number
  availableTicketsMax?: number
}

export interface RealTimeFlightPageQueryREQ extends PageQuery {
  nowTime?: string
  startStationId?: string
  endStationId?: string
}

export interface RealTimeFlightRES extends Flight {

}

/**
 * 航班表单数据类型（用于新增/编辑）
 */
export interface FlightForm {
  id?: number
  aircraftId: number
  routeId: number
  captain: string
  availableTickets: number
  startTime: string
  endTime: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 飞机选择项类型（用于下拉框）
 */
export interface AircraftOption {
  value: number
  label: string
  aircraftModel?: string
  seatNum?: number
}

/**
 * 航线选择项类型（用于下拉框）
 */
export interface RouteOption {
  value: number
  label: string
  departureStation?: string
  arrivalStation?: string
  distance?: number
}

// 为了保持向后兼容，保留Schedule相关的类型别名
/** @deprecated 请使用Flight替代 */
export type Schedule = Flight
/** @deprecated 请使用FlightList替代 */
export type ScheduleList = FlightList
/** @deprecated 请使用FlightQuery替代 */
export type ScheduleQuery = FlightQuery
/** @deprecated 请使用FlightForm替代 */
export type ScheduleForm = FlightForm
/** @deprecated 请使用RealTimeFlightPageQueryREQ替代 */
export type RealTimeSchedulePageQueryREQ = RealTimeFlightPageQueryREQ
/** @deprecated 请使用RealTimeFlightRES替代 */
export type RealTimeScheduleRES = RealTimeFlightRES
/** @deprecated 请使用AircraftOption替代 */
export type AirplaneOption = AircraftOption
