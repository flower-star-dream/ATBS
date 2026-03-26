
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
  status: number // 0, 1, 2, ... 表示不同的状态
  count: number // 该状态的数量
  description: string // 状态描述
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

// 导出认证相关类型
export * from './auth'

// 导出员工相关类型（排除与auth冲突的LoginForm和LoginResponse）
export {
  Employee,
  EmployeeInfo,
  EmployeeList,
  ResetPasswordForm
} from './employee'

// 导出用户相关类型
export {
  User,
  UserList
} from './user'

// 导出乘车人相关类型
export * from './passenger'

// 导出订单相关类型
export * from './order'

// 导出票务相关类型
export * from './ticket'

// 导出班次相关类型
export * from './schedule'

// 导出座位预订相关类型
export * from './seat-reservation'

// 导出线路相关类型
export * from './route'

// 导出站点相关类型
export * from './station'

// 导出线路站点关联相关类型
export * from './route-station'

// 导出飞机相关类型
export * from './aircraft'
