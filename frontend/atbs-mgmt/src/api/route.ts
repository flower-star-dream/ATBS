import { trainSeatRequest } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult } from '@/types'
import type { Route, RouteQuery, RouteForm } from '@/types/route'

/**
 * 添加路线
 * @param data 路线信息
 * @returns 添加响应
 */
export const addRoute = (data: RouteForm): Promise<void> => {
  return trainSeatRequest.post('/route/addRoute', data)
}

/**
 * 删除路线
 * @param ids 路线ID列表
 * @returns 删除响应
 */
export const deleteRoute = (ids: number[]): Promise<void> => {
  return trainSeatRequest.delete('/route/deleteRoute', { data: ids })
}

/**
 * 更新路线
 * @param data 路线信息
 * @returns 更新响应
 */
export const updateRoute = (data: RouteForm): Promise<void> => {
  return trainSeatRequest.put('/route/updateRoute', data)
}

/**
 * 获取路线列表
 * @param params 分页查询参数
 * @returns 路线分页列表
 */
export const getRouteList = (params: RouteQuery): Promise<PageResult<Route>> => {
  return trainSeatRequest.get('/route/getRoute', { params })
}