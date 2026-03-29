import { config } from '@/config'
export const ossUrl = config.ossUrl
// 导出员工相关store
export { useEmployeeStore } from './employee'
// 导出 OAuth2 store
export { useOAuth2Store } from './oauth2'
export * from './app'