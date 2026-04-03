import { airplaneRequest } from '@/utils/request'
import type { Schedule, ScheduleList, ScheduleQuery, RealTimeSchedulePageQueryREQ, RealTimeScheduleRES, ApiResponse, PageResult } from '@/types'

/**
 * 获取航班列表
 * @param params 分页查询参数
 * @returns 航班分页列表
 */
export const getScheduleList = (params: ScheduleQuery): Promise<PageResult<ScheduleList>> => {
  return airplaneRequest.get('/schedule/getSchedules', { params })
}
/**
 * 获取实时航班列表
 * @param params 分页查询参数
 * @returns 航班分页列表
 */
export const getRealTimeSchedule = (params: RealTimeSchedulePageQueryREQ): Promise<PageResult<RealTimeScheduleRES>> => {
  return airplaneRequest.get('/schedule/realTimeSchedule', { params })
}

/**
 * 添加航班
 * @param data 航班信息
 * @returns 添加响应
 */
export const addSchedule = (data: Partial<Schedule>): Promise<void> => {
  return airplaneRequest.post('/schedule/addSchedule', data)
}

/**
 * 更新航班
 * @param data 航班信息
 * @returns 更新响应
 */
export const updateSchedule = (data: Partial<Schedule>): Promise<void> => {
  return airplaneRequest.put('/schedule/updateSchedule', data)
}

/**
 * 删除航班
 * @param ids 航班ID列表
 * @returns 删除响应
 */
export const deleteSchedule = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/schedule/deleteSchedule', { data: ids })
}
