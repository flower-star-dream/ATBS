import type { Config } from './types'
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
    user: '',
    trainSeat: '',
    ticket: '',
    order: '',
    system: ''
  },
  // 测试环境服务配置 - 路径为空，由gateway处理路由
  staging: {
    user: '',
    trainSeat: '',
    ticket: '',
    order: '',
    system: ''
  },
  // 生产环境服务配置 - 路径为空，由gateway处理路由
  production: {
    user: '',
    trainSeat: '',
    ticket: '',
    order: '',
    system: ''
  }
}

/**
 * 主配置对象 - 包含所有环境的默认配置
 * 如需修改配置，只需修改此对象中的对应值
 */
const defaultConfig: Config = {
  baseUrl: 'http://localhost:8080',
  ossUrl: 'http://localhost:9000/atbs',
  apiPrefix: '/api/v1/mgmt',
  timeout: 10000,
  mock: false,
  debug: true,
  title: '火车订票系统-后端管理',
  services: serviceConfig.development
}

/**
 * 环境特定配置覆盖
 * 用于定义不同环境的特定配置
 */
const envConfigs: Record<string, Partial<Config>> = {
  // 开发环境特定配置
  development: {
    title: '火车订票系统-后端管理 - 开发环境',
    services: serviceConfig.development
  },
  
  // 测试环境特定配置
  staging: {
    baseUrl: 'https://atbs.flower-star-dream.top',
    ossUrl: 'https://atbs.flower-star-dream.top/atbs',
    mock: false,
    debug: true,
    title: '火车订票系统-后端管理 - 测试环境',
    services: serviceConfig.staging
  },
  
  // 生产环境特定配置
  production: {
    baseUrl: 'https://atbs.flower-star-dream.top',
    ossUrl: 'https://atbs.flower-star-dream.top/atbs',
    timeout: 30000,
    mock: false,
    debug: false,
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