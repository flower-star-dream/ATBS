// 航线机场关联相关类型定义
import type { PageQuery } from './index'
import type { Route } from './route'
import type { Station } from './station'

/**
 * 航线机场关联基础类型（对应atbs_route_stations表）
 */
export interface RouteStation {
  id: number
  routeId: number
  stationId: number
  stationSorting: number
  createTime?: string
  updateTime?: string
}

/**
 * 航线机场关联列表类型（包含机场信息）
 */
export interface RouteStationList extends RouteStation {
  stationName: string
  address: string
}

/**
 * 航线机场关联查询参数类型
 */
export interface RouteStationQuery extends PageQuery {
  routeId: number
}

/**
 * 航线机场关联表单数据类型（用于新增/编辑）
 */
export interface RouteStationForm {
  id?: number
  routeId: number
  stationId: number
  stationSorting: number
}

/**
 * 航线机场排序更新类型
 */
export interface RouteStationSortUpdate {
  routeId: number
  routeStationsIds: number[]
}

/**
 * 航线完整信息类型（包含起点终点信息）
 */
export interface RouteFullInfo {
  route: Route
  startStation: Station
  endStation: Station
  stations: RouteStationList[]
}
