import { config } from '@/config'
export const ossUrl = config.ossUrl

// 导出认证相关store
export { useAuthStore } from './auth'

// 导出员工相关store
export { useEmployeeStore } from './employee'

// 导出应用相关store
export * from './app'
