import { orderRequest } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult } from '@/types'
import type { RouteStation, RouteStationList, RouteStationQuery, RouteStationForm, RouteStationSortUpdate } from '@/types/route-station'

/**
 * 获取路线站点列表
 * @param params 查询参数
 * @returns 路线站点分页列表
 */
export const getRouteStationList = (params: RouteStationQuery): Promise<PageResult<RouteStationList>> => {
  return orderRequest.get('/routeStations/getRouteStations', { params })
}

/**
 * 添加路线站点
 * @param data 路线站点信息
 * @returns 添加响应
 */
export const addRouteStation = (data: RouteStationForm): Promise<void> => {
  return orderRequest.post('/routeStations/addRouteStations', data)
}

/**
 * 更新路线站点
 * @param data 路线站点信息
 * @returns 更新响应
 */
export const updateRouteStation = (data: RouteStationForm): Promise<void> => {
  return orderRequest.put('/routeStations/updateRouteStations', data)
}

/**
 * 删除路线站点
 * @param ids 路线站点ID列表
 * @returns 删除响应
 */
export const deleteRouteStation = (ids: number[]): Promise<void> => {
  return orderRequest.delete('/routeStations/deleteRouteStations', { data: ids })
}

/**
 * 批量更新线路站点排序
 * @param data 排序更新数据
 * @returns 更新响应
 */
export const updateRouteStationSort = (data: RouteStationSortUpdate): Promise<ApiResponse> => {
  return orderRequest.put('/routeStations/sort', data)
}
