import { orderRequest } from '@/utils/request'
import type * as T from '@/types'

/**
 * 分页查询车票列表
 * @param params 分页查询参数
 * @returns 车票分页列表
 */
export const getTicketListService = (params: T.TicketPageQueryREQ): Promise<T.PageResult<T.Ticket>> => {
  return orderRequest.get('/ticket/page', { params })
}

/**
 * 查询车票详情
 * @param id 车票ID
 * @returns 车票详情
 */
export const getTicketDetailService = (id: string): Promise<T.ApiResponse<T.TicketDetail>> => {
  return orderRequest.get(`/ticket/${id}`)
}

export const getTicketDetailByOrderIdService = (id: string): Promise<T.ApiResponse<T.TicketDetail>> => {
  return orderRequest.get(`/ticket/order/${id}`)
}

/**
 * 更新车票状态
 * @param data 状态更新数据
 * @returns 更新响应
 */
export const updateTicketStatusService = (data: T.TicketStatusUpdateREQ): Promise<T.ApiResponse> => {
  return orderRequest.post('/ticket/status', {
    id: data.id,
    status: data.status,
    scheduleId: data.scheduleId || undefined,
    startStationId: data.startStationId || undefined,
    endStationId: data.endStationId || undefined
  })
}