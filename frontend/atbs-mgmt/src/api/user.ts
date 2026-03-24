import { userRequest } from '@/utils/request'
import type * as T from '@/types'

/**
 * 分页查询用户列表
 * @param params 分页查询参数
 * @returns 用户分页列表
 */
export const getUserListService = (params: T.UserPageQueryREQ): Promise<T.PageResult<T.User>> => {
  return userRequest.get('/user/list', { params })
}

/**
 * 更新用户状态（启用/禁用）
 * @param data 状态更新数据
 * @returns 更新响应
 */
export const updateUserStatusService = (data: T.StartOrStopForm): Promise<T.ApiResponse> => {
  return userRequest.post('/user/status', data)
}