import { config } from '@/config'
export const ossUrl = config.ossUrl
// 导出员工相关store
export { useEmployeeStore } from './employee'
export * from './app'