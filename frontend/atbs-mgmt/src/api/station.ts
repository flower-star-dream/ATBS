import { airplaneRequest } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult, Station, StationQuery, StationForm } from '@/types'

/**
 * 获取机场列表
 * @param params 分页查询参数
 * @returns 机场分页列表
 */
export const getStationList = (params: StationQuery): Promise<PageResult<Station>> => {
  return airplaneRequest.get('/station/getStation', { params })
}

/**
 * 添加机场
 * @param data 机场信息
 * @returns 添加响应
 */
export const addStation = (data: StationForm): Promise<void> => {
  return airplaneRequest.post('/station/addStation', data)
}

/**
 * 更新机场
 * @param data 机场信息
 * @returns 更新响应
 */
export const updateStation = (data: StationForm): Promise<void> => {
  return airplaneRequest.put('/station/updateStation', data)
}

/**
 * 删除机场
 * @param ids 机场ID列表
 * @returns 删除响应
 */
export const deleteStation = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/station/deleteStation', { data: ids })
}
