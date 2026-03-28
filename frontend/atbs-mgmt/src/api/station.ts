import { trainSeatRequest } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult } from '@/types'
import type { Station, StationQuery, StationForm } from '@/types/station'

/**
 * 获取站点列表
 * @param params 分页查询参数
 * @returns 站点分页列表
 */
export const getStationList = (params: StationQuery): Promise<PageResult<Station>> => {
  return trainSeatRequest.get('/station/getStation', { params })
}

/**
 * 添加站点
 * @param data 站点信息
 * @returns 添加响应
 */
export const addStation = (data: StationForm): Promise<void> => {
  return trainSeatRequest.post('/station/addStation', data)
}

/**
 * 更新站点
 * @param data 站点信息
 * @returns 更新响应
 */
export const updateStation = (data: StationForm): Promise<void> => {
  return trainSeatRequest.put('/station/updateStation', data)
}

/**
 * 删除站点
 * @param ids 站点ID列表
 * @returns 删除响应
 */
export const deleteStation = (ids: number[]): Promise<void> => {
  return trainSeatRequest.delete('/station/deleteStation', { data: ids })
}