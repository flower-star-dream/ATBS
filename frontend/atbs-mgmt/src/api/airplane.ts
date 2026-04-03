import { airplaneRequest } from '@/utils/request'
import type { Airplane, AirplaneQuery, ApiResponse, PageQuery, PageResult } from '@/types'

/**
 * 获取飞机列表
 * @param params 分页查询参数
 * @returns 飞机分页列表
 */
export const getAirplaneList = (params: AirplaneQuery): Promise<PageResult<Airplane>> => {
  return airplaneRequest.get('/airplane/getAirplane', { params })
}

/**
 * 添加飞机
 * @param data 飞机信息
 * @returns 添加响应
 */
export const addAirplane = (data: Partial<Airplane>): Promise<void> => {
  return airplaneRequest.post('/airplane/addAirplane', data)
}

/**
 * 更新飞机
 * @param data 飞机信息
 * @returns 更新响应
 */
export const updateAirplane = (data: Partial<Airplane>): Promise<void> => {
  return airplaneRequest.put('/airplane/updateAirplane', data)
}

/**
 * 删除飞机
 * @param ids 飞机ID列表
 * @returns 删除响应
 */
export const deleteAirplane = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/airplane/deleteAirplane', { data: ids })
}
