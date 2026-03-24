import { userRequest } from '@/utils/request'
import type * as T from '@/types'

/**
 * 分页查询乘车人列表
 * @param params 分页查询参数
 * @returns 乘车人分页列表
 */
export const getPassengerListService = (params: T.PassengerPageQueryREQ): Promise<T.PageResult<T.Passenger>> => {
  return userRequest.get('/passenger/list', { params })
}

/**
 * 根据ID查询乘车人详情
 * @param id 乘车人ID
 * @returns 乘车人详情
 */
export const getPassengerDetailService = (id: string): Promise<T.Passenger> => {
  return userRequest.get(`/passenger/${id}`)
}