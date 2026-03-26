import { orderRequest } from '@/utils/request'
import type { Aircraft, AircraftQuery, ApiResponse, PageQuery, PageResult } from '@/types'

/**
 * 获取飞机列表
 * @param params 分页查询参数
 * @returns 飞机分页列表
 */
export const getAircraftList = (params: AircraftQuery): Promise<PageResult<Aircraft>> => {
  return orderRequest.get('/aircraft/getAircraft', { params })
}

/**
 * 添加飞机
 * @param data 飞机信息
 * @returns 添加响应
 */
export const addAircraft = (data: Partial<Aircraft>): Promise<void> => {
  return orderRequest.post('/aircraft/addAircraft', data)
}

/**
 * 更新飞机
 * @param data 飞机信息
 * @returns 更新响应
 */
export const updateAircraft = (data: Partial<Aircraft>): Promise<void> => {
  return orderRequest.put('/aircraft/updateAircraft', data)
}

/**
 * 删除飞机
 * @param ids 飞机ID列表
 * @returns 删除响应
 */
export const deleteAircraft = (ids: number[]): Promise<void> => {
  return orderRequest.delete('/aircraft/deleteAircraft', { data: ids })
}
