import { airplaneRequest } from '@/utils/request'
import type {
  SeatReservation,
  SeatReservationList,
  SeatReservationQuery,
  SeatReservationChangeStatusREQ,
  SeatReservationForm,
  SeatStatusStats,
  StatusCount,
  ApiResponse,
  PageResult
} from '@/types'

/**
 * 获取座位预订列表
 * @param params 分页查询参数
 * @returns 座位预订分页列表
 */
export const getSeatReservationList = (params: SeatReservationQuery): Promise<PageResult<SeatReservationList>> => {
  return airplaneRequest.get('/seatReservation/getSeatReservation', { params })
}

/**
 * 添加座位预订
 * @param data 座位预订信息
 * @returns 添加响应
 */
export const addSeatReservation = (data: Partial<SeatReservationForm>): Promise<void> => {
  return airplaneRequest.post('/seatReservation/addSeatReservation', data)
}

/**
 * 更新座位预订
 * @param data 座位预订信息
 * @returns 更新响应
 */
export const updateSeatReservation = (data: Partial<SeatReservationForm>): Promise<void> => {
  return airplaneRequest.put('/seatReservation/updateSeatReservation', data)
}

/**
 * 删除座位预订
 * @param ids 座位预订ID列表
 * @returns 删除响应
 */
export const deleteSeatReservation = (ids: number[]): Promise<void> => {
  return airplaneRequest.delete('/seatReservation/deleteSeatReservation', { data: ids })
}

/**
 * 批量更新座位状态
 * @param ids 座位预订ID数组
 * @param status 新的状态
 * @returns 更新响应
 */
export const batchUpdateSeatStatus = (data: Partial<SeatReservationChangeStatusREQ>): Promise<ApiResponse> => {
  return airplaneRequest.put('/seatReservation/batch-update-status', data)
}

/**
 * 检查座位是否可用
 * @param scheduleId 航班ID
 * @param seatNum 座位号
 * @param excludeId 排除的座位预订ID（编辑时用于排除自身）
 * @returns 检查结果
 */
export const checkSeatAvailability = (scheduleId: number, seatNum: number, excludeId?: number): Promise<boolean> => {
  return airplaneRequest.get('/seatReservation/check-availability', {
    params: { scheduleId, seatNum, excludeId }
  })
}

/**
 * 获取座位状态统计
 * @param scheduleId 航班ID（可选）
 * @returns 座位状态统计信息
 */
export const getStatus = (): Promise<StatusCount[]> => {
  return airplaneRequest.get('/seatReservation/getStatus')
}
