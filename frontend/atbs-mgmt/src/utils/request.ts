import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import config from '@/config'

/**
 * 客户端类型枚举
 * ADMIN_WEB = 1（管理后台Web端）
 * APPLET = 2（小程序端）
 */
export enum ClientType {
  ADMIN_WEB = 1,
  APPLET = 2
}

// 请求拦截器
import type { InternalAxiosRequestConfig } from 'axios'

/**
 * 请求拦截器
 * 添加X-Client-Type头部和认证令牌
 */
const requestInterceptor = (config: InternalAxiosRequestConfig) => {
  config.headers = config.headers || {}
  // 添加X-Client-Type头部，标识客户端类型为管理后台Web端
  config.headers['X-Client-Type'] = ClientType.ADMIN_WEB
  // 如果有token，则添加Authorization头部（不添加Bearer前缀，后端自动处理）
  const token = localStorage.getItem('access_token') || localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
}

/**
 * 响应拦截器
 * 处理成功响应
 */
const responseInterceptor = (response: AxiosResponse<ApiResponse>) => {
  const { code, message, data } = response.data

  if (code === 200) {
    return data
  } else {
    // 不在这里显示错误信息，而是将错误传递给错误拦截器统一处理
    // 创建一个包含原始响应信息的错误对象
    const error = new Error(message || '请求失败')
    ;(error as any).response = response
    return Promise.reject(error)
  }
}

/**
 * 错误响应拦截器
 * 处理错误响应
 */
const errorResponseInterceptor = (error: any) => {
  // 处理401未授权错误
  if (error.response?.status === 401) {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('token')
    localStorage.removeItem('user_info')
    window.location.href = '/login'
    ElMessage.error('登录已过期，请重新登录')
  } else {
    // 从错误对象中提取错误信息
    // 优先从响应体中获取message字段，适用于后端返回的500错误等情况
    const errorMessage = (error.response?.data?.message) ||
                        error.message ||
                        '网络错误'

    // 显示错误信息
    ElMessage.error(errorMessage)
  }

  // 确保错误对象中包含完整的响应体信息
  if (error.response && error.response.data) {
    // 将响应体数据附加到错误对象上，方便调用者查看
    (error as any).responseBody = error.response.data
  }

  // 继续传递错误，让调用者能够捕获并处理
  return Promise.reject(error)
}

/**
 * 创建请求实例并应用拦截器
 * @param baseURL 基础URL
 * @returns axios实例
 */
const createRequestInstance = (baseURL: string) => {
  const instance = axios.create({
    baseURL,
    timeout: config.timeout
  })

  instance.interceptors.request.use(requestInterceptor, error => Promise.reject(error))
  instance.interceptors.response.use(responseInterceptor, errorResponseInterceptor)

  return instance
}

// 默认请求实例
const request = createRequestInstance(`${config.baseUrl}${config.apiPrefix}`)

/**
 * 根据服务模块创建请求方法
 * @param module 服务模块名称
 * @returns 特定模块的请求方法
 * 注意：根据网关配置，路径格式为 ${baseUrl}${apiPrefix}/${module}
 */
export const createServiceRequest = (module: keyof typeof config.services) => {
  return createRequestInstance(`${config.baseUrl}${config.apiPrefix}/${module}`)
}

// 导出各个服务模块的请求实例
export const userRequest = createServiceRequest('user')
export const orderRequest = createServiceRequest('order')

export default request
