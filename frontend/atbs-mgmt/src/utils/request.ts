import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import config from '@/config'

// 请求拦截器
import type { InternalAxiosRequestConfig } from 'axios'

const requestInterceptor = (config: InternalAxiosRequestConfig) => {
  config.headers = config.headers || {}
  // 无论是否登录，都添加X-Biz-Side头部
  // 添加必需的X-Biz-Side头部，设置为admin，使用小写格式以匹配后端期望
  config.headers['X-Biz-Side'] = 'admin'
  // 如果有token，则添加Authorization头部
  const token = localStorage.getItem('token')
  if (token) {
    // 添加Bearer前缀，确保token格式正确
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}

// 响应拦截器
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

// 错误响应拦截器
const errorResponseInterceptor = (error: any) => {
  // 处理401未授权错误
  if (error.response?.status === 401) {
    localStorage.removeItem('token')
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

// 创建请求实例并应用拦截器
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
 * 注意：根据网关配置，路径格式为 ${baseUrl}/${apiPrefix}/${module}
 */
export const createServiceRequest = (module: keyof typeof config.services) => {
  return createRequestInstance(`${config.baseUrl}${config.apiPrefix}/${module}`)
}

// 导出各个服务模块的请求实例
export const userRequest = createServiceRequest('user')
export const trainSeatRequest = createServiceRequest('trainSeat')
export const ticketRequest = createServiceRequest('ticket')
export const orderRequest = createServiceRequest('order')
export const systemRequest = createServiceRequest('system')

export default request