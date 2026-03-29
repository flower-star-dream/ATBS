import { airplaneRequest } from '@/utils/request'
import type { Aircraft, AircraftQuery, ApiResponse, PageQuery, PageResult } from '@/types'

/**
 * 获取飞机列表
 * @param params 分页查询参数
 * @returns 飞机分页列表
 */
export const getAircraftList = (params: AircraftQuery): Promise<PageResult<Aircraft>> => {
  return airplaneRequest.get('/aircraft/getAircraft', { params })
}

/**
 * 添加飞机
 * @param data 飞机信息
 * @returns 添加响应
 */
export const addAircraft = (data: Partial<Aircraft>): Promise<void> => {
  return airplaneRequest.post('/aircraft/addAircraft', data)
}

/**
 * 更新飞机
 * @param data 飞机信息
 * @returns 更新响应
 */
export const updateAircraft = (data: Partial<Aircraft>): Promise<void> => {
  return airplaneRequest.put('/aircraft/updateAircraft', data)
}

/**
 * 删除飞机
 * @param ids 飞机ID列表
 * @returns 删除响应
 */
export const deleteAircraft = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/aircraft/deleteAircraft', { data: ids })
}

// 为了保持向后兼容，保留旧的函数名
/** @deprecated 请使用getAircraftList替代 */
export const getAirplaneList = getAircraftList
/** @deprecated 请使用addAircraft替代 */
export const addAirplane = addAircraft
/** @deprecated 请使用updateAircraft替代 */
export const updateAirplane = updateAircraft
/** @deprecated 请使用deleteAircraft替代 */
export const deleteAirplane = deleteAircraft
