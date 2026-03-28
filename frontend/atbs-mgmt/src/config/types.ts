// 服务模块配置类型
export interface ServiceConfig {
  user: string
  trainSeat: string
  ticket: string
  order: string
  system: string
}

// 环境配置类型定义
export interface Config {
  baseUrl: string
  ossUrl: string
  apiPrefix: string
  timeout: number
  mock: boolean
  debug: boolean
  title: string
  services: ServiceConfig
}

// API响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  success: boolean
}

// 分页参数类型
export interface PageParams {
  page: number
  size: number
  total?: number
}

// 分页响应类型
export interface PageResponse<T = any> {
  list: T[]
  total: number
  page: number
  size: number
}

// 表格列配置类型
export interface TableColumn {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  fixed?: boolean | 'left' | 'right'
  sortable?: boolean | 'custom'
  formatter?: (row: any, column: any, cellValue: any, index: number) => string
}

// 表单验证规则类型
export interface FormRule {
  required?: boolean
  message?: string
  trigger?: string | string[]
  validator?: (rule: any, value: any, callback: any) => void
  pattern?: RegExp
  min?: number
  max?: number
  len?: number
  type?: string
}

// 路由元信息类型
export interface RouteMeta {
  title?: string
  icon?: string
  hidden?: boolean
  keepAlive?: boolean
  permission?: string | string[]
  activeMenu?: string
  breadcrumb?: boolean
  affix?: boolean
}

// 员工信息类型
export interface UserInfo {
  id: string | number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  roles: string[]
  permissions: string[]
  createTime: string
  updateTime: string
}