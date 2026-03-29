import { airplaneRequest } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult, RouteStation, RouteStationList, RouteStationQuery, RouteStationForm, RouteStationSortUpdate } from '@/types'

/**
 * 获取航线机场列表
 * @param params 查询参数
 * @returns 航线机场分页列表
 */
export const getRouteStationList = (params: RouteStationQuery): Promise<PageResult<RouteStationList>> => {
  return airplaneRequest.get('/routeStations/getRouteStations', { params })
}

/**
 * 添加航线机场
 * @param data 航线机场信息
 * @returns 添加响应
 */
export const addRouteStation = (data: RouteStationForm): Promise<void> => {
  return airplaneRequest.post('/routeStations/addRouteStations', data)
}

/**
 * 更新航线机场
 * @param data 航线机场信息
 * @returns 更新响应
 */
export const updateRouteStation = (data: RouteStationForm): Promise<void> => {
  return airplaneRequest.put('/routeStations/updateRouteStations', data)
}

/**
 * 删除航线机场
 * @param ids 航线机场ID列表
 * @returns 删除响应
 */
export const deleteRouteStation = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/routeStations/deleteRouteStations', { data: ids })
}

/**
 * 批量更新航线机场排序
 * @param data 排序更新数据
 * @returns 更新响应
 */
export const updateRouteStationSort = (data: RouteStationSortUpdate): Promise<ApiResponse> => {
  return airplaneRequest.put('/routeStations/sort', data)
}
