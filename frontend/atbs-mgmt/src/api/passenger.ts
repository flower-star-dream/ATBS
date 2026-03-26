import { userRequest } from '@/utils/request'
import type { Passenger, PassengerPageQueryREQ } from '@/types/passenger'
import type { PageResult } from '@/types'

/**
 * 分页查询乘车人列表
 * @param params 分页查询参数
 * @returns 乘车人分页列表
 */
export const getPassengerListService = (params: PassengerPageQueryREQ): Promise<PageResult<Passenger>> => {
  return userRequest.get('/passenger/list', { params })
}

/**
 * 根据ID查询乘车人详情
 * @param id 乘车人ID
 * @returns 乘车人详情
 */
export const getPassengerDetailService = (id: string): Promise<Passenger> => {
  return userRequest.get(`/passenger/${id}`)
}
