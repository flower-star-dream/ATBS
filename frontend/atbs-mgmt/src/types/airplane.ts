// 飞机相关类型定义
import type { PageQuery } from './index'

/**
 * 飞机基础类型（对应atbs_aircraft表）
 */
export interface Aircraft {
  id: number
  aircraftName: string
  aircraftModel: string
  seatNum: number
  serviceYears: number
  status: number
  createTime?: string
  updateTime?: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 飞机列表类型（包含分页信息）
 */
export interface AircraftList extends Aircraft {
  // 继承Aircraft的所有属性
}

/**
 * 飞机查询参数类型
 */
export interface AircraftQuery extends PageQuery {
  aircraftName?: string
}

/**
 * 飞机表单数据类型（用于新增/编辑）
 */
export interface AircraftForm {
  id?: number
  aircraftName: string
  aircraftModel: string
  seatNum: number
  serviceYears: number
  status?: number
  createPerson?: string
  updatePerson?: string
}

// 为了保持向后兼容，保留Airplane相关的类型别名
/** @deprecated 请使用Aircraft替代 */
export type Airplane = Aircraft
/** @deprecated 请使用AircraftList替代 */
export type AirplaneList = AircraftList
/** @deprecated 请使用AircraftQuery替代 */
export type AirplaneQuery = AircraftQuery
/** @deprecated 请使用AircraftForm替代 */
export type AirplaneForm = AircraftForm
