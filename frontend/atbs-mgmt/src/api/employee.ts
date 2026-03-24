import { userRequest } from '@/utils/request'
import type * as T from '@/types'

/**
 * 员工登录
 * @param data 登录表单数据
 * @returns 登录响应
 */
export const login = (data: T.LoginForm): Promise<T.LoginResponse> => {
  return userRequest.post('/employee/login', data)
}

/**
 * 获取当前员工信息
 * @returns 员工信息
 */
export const getEmployeeInfoService = (): Promise<T.EmployeeInfo> => {
  return userRequest.get(`/employee/info`)
}

/**
 * 更新员工信息
 * @param data 员工信息
 * @returns 更新响应
 */
export const updateEmployeeInfoService = (data: Partial<T.EmployeeInfo>): Promise<T.ApiResponse> => {
  return userRequest.put('/employee/info', data)
}

/**
 * 获取员工列表
 * @param params 分页查询参数
 * @returns 员工分页列表
 */
export const getEmployeeListService = (params: T.PageQuery): Promise<T.PageResult<T.EmployeeList>> => {
  return userRequest.get('/employee/list', { params })
}

/**
 * 添加员工
 * @param data 员工信息
 * @returns 创建响应
 */
export const createEmployeeService = (data: Partial<T.Employee>): Promise<T.ApiResponse> => {
  return userRequest.post('/employee/add', data)
}

/**
 * 更新员工
 * @param id 员工ID
 * @param data 员工信息
 * @returns 更新响应
 */
export const updateEmployeeService = (data: Partial<T.Employee>): Promise<T.ApiResponse> => {
  return userRequest.put(`/employee/update`, data)
}

/**
 * 删除员工
 * @param ids 员工ID列表
 * @returns 删除响应
 */
export const deleteEmployeeService = (ids: string[]): Promise<T.ApiResponse> => {
  return userRequest.delete(`/employee/${ids}`)
}

/** 
 * 重置员工密码
 * @param data 重置密码表单数据
 * @returns 重置响应
 */
export const resetPasswordService = (data: T.ResetPasswordForm): Promise<T.ApiResponse> => {
  return userRequest.post('/employee/resetPwd', data)
}

/** 
 * 修改账号状态
 * @param id 用户ID
 * @param data 状态表单数据
 * @returns 状态响应
 */
// 启用或禁用员工账号
export const startOrStopService = (data: T.StartOrStopForm): Promise<T.ApiResponse> => {
  return userRequest.post(`/employee/status`, data);
};
