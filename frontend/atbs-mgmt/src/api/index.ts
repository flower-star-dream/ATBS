// API 接口统一导出文件

// 认证相关 API
export * from './auth'

// 员工管理相关 API
export * from './employee'

// 用户管理相关 API
export * from './user'

// 飞机管理相关 API
export * from './aircraft'

// 班次管理相关 API
export * from './schedule'

// 订单管理相关 API（已合并票务服务）
export * from './order'

// 票务管理相关 API（已弃用，请使用order.ts）
// export * from './ticket'

// 乘车人管理相关 API
export * from './passenger'

// 座位预订管理相关 API
export * from './seat-reservation'

// 线路管理相关 API
export * from './route'

// 站点管理相关 API
export * from './station'

// 线路站点关联管理相关 API
export * from './route-station'
