// 飞机相关类型定义
import type { PageQuery } from './index'

/**
 * 飞机基础类型（对应atbs_airplane表）
 */
export interface Airplane {
  id: number
  airplaneName: string
  airplaneModel: string
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
export interface AirplaneList extends Airplane {
  // 继承Airplane的所有属性
}

/**
 * 飞机查询参数类型
 */
export interface AirplaneQuery extends PageQuery {
  airplaneName?: string
}

/**
 * 飞机表单数据类型（用于新增/编辑）
 */
export interface AirplaneForm {
  id?: number
  airplaneName: string
  airplaneModel: string
  seatNum: number
  serviceYears: number
  status?: number
  createPerson?: string
  updatePerson?: string
}


