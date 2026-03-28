import { orderRequest } from '@/utils/request'
import type * as T from '@/types'

/**
 * 分页查询订单列表
 * @param params 分页查询参数
 * @returns 订单分页列表
 */
export const getOrderListService = (params: T.OrderPageQueryREQ): Promise<T.PageResult<T.Order>> => {
  return orderRequest.get('/order/page', { params })
}

/**
 * 查询订单详情
 * @param id 订单ID
 * @returns 订单详情
 */
export const getOrderDetailService = (id: string): Promise<T.ApiResponse<T.OrderDetail>> => {
  return orderRequest.get(`/order/${id}`)
}

/**
 * 更新订单状态
 * @param data 状态更新数据
 * @returns 更新响应
 */
export const updateOrderStatusService = (data: T.OrderStatusUpdateREQ): Promise<T.ApiResponse> => {
  return orderRequest.put('/order/status', {
    id: data.id,
    status: data.status,
    remarks: data.remarks || ''
  })
}