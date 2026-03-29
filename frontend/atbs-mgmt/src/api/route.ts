import { airplaneRequest } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult, Route, RouteQuery, RouteForm } from '@/types'

/**
 * 添加航线
 * @param data 航线信息
 * @returns 添加响应
 */
export const addRoute = (data: RouteForm): Promise<void> => {
  return airplaneRequest.post('/route/addRoute', data)
}

/**
 * 删除航线
 * @param ids 航线ID列表
 * @returns 删除响应
 */
export const deleteRoute = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/route/deleteRoute', { data: ids })
}

/**
 * 更新航线
 * @param data 航线信息
 * @returns 更新响应
 */
export const updateRoute = (data: RouteForm): Promise<void> => {
  return airplaneRequest.put('/route/updateRoute', data)
}

/**
 * 获取航线列表
 * @param params 分页查询参数
 * @returns 航线分页列表
 */
export const getRouteList = (params: RouteQuery): Promise<PageResult<Route>> => {
  return airplaneRequest.get('/route/getRoute', { params })
}
