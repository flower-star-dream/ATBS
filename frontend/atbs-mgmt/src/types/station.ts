// 机场相关类型定义
import type { PageQuery } from './index'

/**
 * 机场基础类型（对应atbs_airport表）
 */
export interface Station {
  id: number
  stationName: string
  address: string
  createTime?: string
  updateTime?: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 机场列表类型（包含分页信息）
 */
export interface StationList extends Station {
  // 继承Station的所有属性
}

/**
 * 机场查询参数类型
 */
export interface StationQuery extends PageQuery {
  stationName?: string
  address?: string
}

/**
 * 机场表单数据类型（用于新增/编辑）
 */
export interface StationForm {
  id?: number
  stationName: string
  address: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 机场下拉选项类型
 */
export interface StationOption {
  value: number
  label: string
  address: string
}
