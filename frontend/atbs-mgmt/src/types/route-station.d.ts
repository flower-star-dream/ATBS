// 线路站点关联相关类型定义
import type { PageQuery } from './index'
import type { Route } from './route'
import type { Station } from './station'

/**
 * 线路站点关联基础类型（对应hcd_route_stations表）
 */
export interface RouteStation {
  id: number                    // 路线站点号（bigint）
  routeId: number              // 线路号（bigint）
  stationId: number            // 站点号（bigint）
  stationSorting: number       // 站点顺序（int）
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
}

/**
 * 线路站点关联列表类型（包含站点信息）
 */
export interface RouteStationList extends RouteStation {
  stationName: string          // 站点名称
  address: string              // 站点地址
}

/**
 * 线路站点关联查询参数类型
 */
export interface RouteStationQuery extends PageQuery {
  routeId: number              // 线路ID（必填）
}

/**
 * 线路站点关联表单数据类型（用于新增/编辑）
 */
export interface RouteStationForm {
  id?: number                  // 路线站点号（编辑时必填）
  routeId: number             // 线路ID
  stationId: number           // 站点ID
  stationSorting: number      // 站点顺序
}

/**
 * 线路站点排序更新类型
 */
export interface RouteStationSortUpdate {
  routeId: number              // 线路ID
  routeStationsIds: number[]         // 按新顺序排列的线路站点ID数组
}

/**
 * 线路完整信息类型（包含起点终点信息）
 */
export interface RouteFullInfo {
  route: Route                 // 线路基本信息
  startStation: Station        // 起点站信息
  endStation: Station          // 终点站信息
  stations: RouteStationList[] // 线路所有站点（按顺序）
}
