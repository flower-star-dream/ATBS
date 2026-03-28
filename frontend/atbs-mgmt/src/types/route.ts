// 线路相关类型定义
import type { PageQuery } from './index'

/**
 * 线路基础类型（对应atbs_route表）
 */
export interface Route {
  id: number                    // 线路号（bigint）
  routeName: string            // 线路名（varchar(20)）
  startStation: string         // 起点站（varchar(50)）
  endStation: string           // 终点站（varchar(50)）
  stationCount: number         // 站点数（int）
  startStationName: string       // 起点站名（varchar(50））  
  endStationName: string         // 终点站名（varchar(50））
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 线路列表类型（包含分页信息）
 */
export interface RouteList extends Route {
  // 继承Route的所有属性
}

/**
 * 线路查询参数类型
 */
export interface RouteQuery extends PageQuery {
  routeName?: string           // 线路名（模糊查询）
  startStation?: string        // 起点站（模糊查询）
  endStation?: string          // 终点站（模糊查询）
}

/**
 * 线路表单数据类型（用于新增/编辑）
 */
export interface RouteForm {
  id?: number                  // 线路号（编辑时必填）
  routeName: string           // 线路名
  startStationId: number      // 起点站ID
  endStationId: number        // 终点站ID
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 线路状态枚举
 */
export enum ROUTE_STATUS {
  ENABLED = 1,
  DISABLED = 0
}

/**
 * 获取线路状态文本
 */
export const getRouteStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    [ROUTE_STATUS.ENABLED]: '已启用',
    [ROUTE_STATUS.DISABLED]: '已禁用'
  }
  return statusMap[status] || '未知状态'
}

/**
 * 获取线路状态标签类型
 */
export const getRouteStatusTagType = (status: number): string => {
  const typeMap: Record<number, string> = {
    [ROUTE_STATUS.ENABLED]: 'success',
    [ROUTE_STATUS.DISABLED]: 'danger'
  }
  return typeMap[status] || 'info'
}