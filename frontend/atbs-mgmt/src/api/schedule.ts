import { airplaneRequest } from '@/utils/request'
import type { Flight, FlightList, FlightQuery, RealTimeFlightPageQueryREQ, RealTimeFlightRES, ApiResponse, PageResult } from '@/types'

/**
 * 获取航班列表
 * @param params 分页查询参数
 * @returns 航班分页列表
 */
export const getScheduleList = (params: FlightQuery): Promise<PageResult<FlightList>> => {
  return airplaneRequest.get('/flight/getFlights', { params })
}
/**
 * 获取实时航班列表
 * @param params 分页查询参数
 * @returns 航班分页列表
 */
export const getRealTimeSchedule = (params: RealTimeFlightPageQueryREQ): Promise<PageResult<RealTimeFlightRES>> => {
  return airplaneRequest.get('/flight/realTimeFlight', { params })
}

/**
 * 添加航班
 * @param data 航班信息
 * @returns 添加响应
 */
export const addSchedule = (data: Partial<Flight>): Promise<void> => {
  return airplaneRequest.post('/flight/addFlight', data)
}

/**
 * 更新航班
 * @param data 航班信息
 * @returns 更新响应
 */
export const updateSchedule = (data: Partial<Flight>): Promise<void> => {
  return airplaneRequest.put('/flight/updateFlight', data)
}

/**
 * 删除航班
 * @param ids 航班ID列表
 * @returns 删除响应
 */
export const deleteSchedule = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/flight/deleteFlight', { data: ids })
}
