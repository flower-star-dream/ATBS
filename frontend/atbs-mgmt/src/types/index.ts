// 分页相关类型
export interface PageQuery {
  page: number
  pageSize: number
  keyword?: string
  [key: string]: any
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface StatusCount {
  status: number
  count: number
  description: string
}

// API响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 启用或禁用表单数据结构
export interface StartOrStopForm {
  id: string
  status: number
}

// 从各个类型定义文件重新导出
export * from './employee'
export * from './airplane'
export * from './user'
export * from './passenger'
export * from './order'
export * from './ticket'
export * from './schedule'
export * from './seat-reservation'
export * from './route'
export * from './station'
export * from './route-station'
export * from './client'
export * from './oauth2'
