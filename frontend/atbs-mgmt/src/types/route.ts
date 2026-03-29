// 航线相关类型定义
import type { PageQuery } from './index'

/**
 * 航线基础类型（对应atbs_route表）
 */
export interface Route {
  id: number
  routeName: string
  startStation: string
  endStation: string
  stationCount: number
  startStationName: string
  endStationName: string
  createTime?: string
  updateTime?: string
  createPerson?: string
  updatePerson?: string
}

/**
 * 航线列表类型（包含分页信息）
 */
export interface RouteList extends Route {
  // 继承Route的所有属性
}

/**
 * 航线查询参数类型
 */
export interface RouteQuery extends PageQuery {
  routeName?: string
  startStation?: string
  endStation?: string
}

/**
 * 航线表单数据类型（用于新增/编辑）
 */
export interface RouteForm {
  id?: number
  routeName: string
  startStationId: number
  endStationId: number
  createPerson?: string
  updatePerson?: string
}

/**
 * 航线状态枚举
 */
export enum ROUTE_STATUS {
  ENABLED = 1,
  DISABLED = 0
}

/**
 * 获取航线状态文本
 */
export const getRouteStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    [ROUTE_STATUS.ENABLED]: '已启用',
    [ROUTE_STATUS.DISABLED]: '已禁用'
  }
  return statusMap[status] || '未知状态'
}

/**
 * 获取航线状态标签类型
 */
export const getRouteStatusTagType = (status: number): string => {
  const typeMap: Record<number, string> = {
    [ROUTE_STATUS.ENABLED]: 'success',
    [ROUTE_STATUS.DISABLED]: 'danger'
  }
  return typeMap[status] || 'info'
}
