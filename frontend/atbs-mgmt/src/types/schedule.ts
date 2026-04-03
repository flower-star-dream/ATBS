// 航班相关类型定义
import type { PageQuery } from './index'

/**
 * 航班基础类型（对应atbs_schedule表）
 */
export interface Schedule {
  id: number
  airplaneId: number
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
export interface ScheduleList extends Schedule {
  airplaneName?: string
  routeName?: string
  routeInfo?: string
}

/**
 * 航班查询参数类型
 */
export interface ScheduleQuery extends PageQuery {
  airplaneId?: number
  routeId?: number
  captain?: string
  startTimeStart?: string
  startTimeEnd?: string
  availableTicketsMin?: number
  availableTicketsMax?: number
}

export interface RealTimeSchedulePageQueryREQ extends PageQuery {
  nowTime?: string
  startStationId?: string
  endStationId?: string
}

export interface RealTimeScheduleRES extends Schedule {

}

/**
 * 航班表单数据类型（用于新增/编辑）
 */
export interface ScheduleForm {
  id?: number
  airplaneId: number
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
export interface AirplaneOption {
  value: number
  label: string
  airplaneModel?: string
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


