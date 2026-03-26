import type { Config } from './types'
import { getEnv } from './env'

/**
 * 统一配置管理模块
 * 将所有环境配置集中在一处，无需修改.env文件
 */

// 获取当前环境
const env = getEnv()

// 服务模块配置常量
// 注意：路径格式已调整为 /api/{service}/v1/*/*
const serviceConfig = {
  // 开发环境服务配置
  development: {
    user: '',
    order: '',
    system: ''
  },
  // 测试环境服务配置
  staging: {
    user: '',
    order: '',
    system: ''
  },
  // 生产环境服务配置
  production: {
    user: '',
    order: '',
    system: ''
  }
}

/**
 * 主配置对象 - 包含所有环境的默认配置
 * 注意：apiPrefix已从 /api/v1/mgmt 调整为 /api/mgmt/v1
 */
const defaultConfig: Config = {
  baseUrl: 'http://localhost:8080',
  ossUrl: 'http://localhost:9000/hcd',
  apiPrefix: '/api/mgmt/v1',
  timeout: 10000,
  mock: false,
  debug: true,
  title: '飞机订票系统-后端管理',
  services: serviceConfig.development
}

/**
 * 环境特定配置覆盖
 * 用于定义不同环境的特定配置
 */
const envConfigs: Record<string, Partial<Config>> = {
  // 开发环境特定配置
  development: {
    title: '飞机订票系统-后端管理 - 开发环境',
    services: serviceConfig.development
  },

  // 测试环境特定配置
  staging: {
    baseUrl: 'https://hcd.flower-star-dream.top',
    ossUrl: 'https://hcd.flower-star-dream.top/hcd',
    mock: false,
    debug: true,
    title: '飞机订票系统-后端管理 - 测试环境',
    services: serviceConfig.staging
  },

  // 生产环境特定配置
  production: {
    baseUrl: 'https://hcd.flower-star-dream.top',
    ossUrl: 'https://hcd.flower-star-dream.top/hcd',
    timeout: 30000,
    mock: false,
    debug: false,
    title: '飞机订票系统-后端管理',
    services: serviceConfig.production
  }
}

// 合并默认配置和环境特定配置
export const config: Config = {
  ...defaultConfig,
  ...(envConfigs[env] || {})
}

// 环境判断辅助函数
export const isDev = env === 'development'
export const isStaging = env === 'staging'
export const isProd = env === 'production'

// 默认导出配置
export default config
