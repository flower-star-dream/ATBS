import type { Config, OAuth2Config } from './types'
import { getEnv } from './env'

/**
 * 统一配置管理模块
 * 将所有环境配置集中在一处，无需修改.env文件
 */

// 获取当前环境
const env = getEnv()

// 服务模块配置常量
const serviceConfig = {
  // 开发环境服务配置 - 路径为空，由gateway处理路由
  development: {
    auth: '',
    user: '',
    airplane: '',
    order: '',
    prediction: ''
  },
  // 测试环境服务配置 - 路径为空，由gateway处理路由
  staging: {
    auth: '',
    user: '',
    airplane: '',
    order: '',
    prediction: ''
  },
  // 生产环境服务配置 - 路径为空，由gateway处理路由
  production: {
    auth: '',
    user: '',
    airplane: '',
    order: '',
    prediction: ''
  }
}

// OAuth2 配置常量
const oauth2Config: Record<string, OAuth2Config> = {
  // 开发环境 OAuth2 配置
  development: {
    authBaseUrl: 'http://localhost:8080',
    clientId: 'mgmt-client',
    clientSecret: 'mgmt-secret',
    scope: 'openid profile read write'
  },
  // 测试环境 OAuth2 配置
  staging: {
    authBaseUrl: 'https://atbs.flower-star-dream.top',
    clientId: 'mgmt-client',
    clientSecret: 'mgmt-secret',
    scope: 'openid profile read write'
  },
  // 生产环境 OAuth2 配置
  production: {
    authBaseUrl: 'https://atbs.flower-star-dream.top',
    clientId: 'mgmt-client',
    clientSecret: 'mgmt-secret',
    scope: 'openid profile read write'
  }
}

/**
 * 主配置对象 - 包含所有环境的默认配置
 * 如需修改配置，只需修改此对象中的对应值
 */
const defaultConfig: Config = {
  baseUrl: 'http://localhost:8080',
  ossUrl: 'http://localhost:9000/atbs',
  apiPrefix: '/api/mgmt/v1',
  timeout: 10000,
  mock: false,
  debug: true,
  title: '飞机订票系统-后端管理',
  services: serviceConfig.development,
  oauth2: oauth2Config.development
}

/**
 * 环境特定配置覆盖
 * 用于定义不同环境的特定配置
 */
const envConfigs: Record<string, Partial<Config>> = {
  // 开发环境特定配置
  development: {
    title: '飞机订票系统-后端管理 - 开发环境',
    services: serviceConfig.development,
    oauth2: oauth2Config.development
  },

  // 测试环境特定配置
  staging: {
    baseUrl: 'https://atbs.flower-star-dream.top',
    ossUrl: 'https://atbs.flower-star-dream.top/atbs',
    mock: false,
    debug: true,
    title: '飞机订票系统-后端管理 - 测试环境',
    services: serviceConfig.staging,
    oauth2: oauth2Config.staging
  },

  // 生产环境特定配置
  production: {
    baseUrl: 'https://atbs.flower-star-dream.top',
    ossUrl: 'https://atbs.flower-star-dream.top/atbs',
    timeout: 30000,
    mock: false,
    debug: false,
    services: serviceConfig.production,
    oauth2: oauth2Config.production
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