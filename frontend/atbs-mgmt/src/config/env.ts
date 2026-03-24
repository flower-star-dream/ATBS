// 环境工具函数

/**
 * 获取当前环境
 */
export const getEnv = (): string => {
  return import.meta.env.MODE || process.env.NODE_ENV || 'development'
}

/**
 * 判断是否为开发环境
 */
export const isDev = (): boolean => {
  return getEnv() === 'development'
}

/**
 * 判断是否为测试环境
 */
export const isStaging = (): boolean => {
  return getEnv() === 'staging'
}

/**
 * 判断是否为生产环境
 */
export const isProd = (): boolean => {
  return getEnv() === 'production'
}

/**
 * 获取环境变量值
 */
export const getEnvVar = (key: string, defaultValue?: string): string => {
  return import.meta.env[key] || process.env[key] || defaultValue || ''
}

/**
 * 获取布尔类型的环境变量
 */
export const getBooleanEnvVar = (key: string, defaultValue = false): boolean => {
  const value = getEnvVar(key)
  if (value === '') return defaultValue
  return value === 'true'
}

/**
 * 获取数字类型的环境变量
 */
export const getNumberEnvVar = (key: string, defaultValue = 0): number => {
  const value = getEnvVar(key)
  if (value === '') return defaultValue
  const num = Number(value)
  return isNaN(num) ? defaultValue : num
}

/**
 * 获取所有环境变量
 */
export const getAllEnvVars = (): Record<string, string> => {
  return {
    ...Object.fromEntries(
      Object.entries(import.meta.env).filter(([key]) => key.startsWith('VITE_'))
    ),
    ...Object.fromEntries(
      Object.entries(process.env).filter(([key]) => !key.startsWith('npm_'))
    ),
  }
}

/**
 * 环境配置工具类
 */
export class EnvConfig {
  private static instance: EnvConfig
  private config: Record<string, any> = {}

  private constructor() {
    this.init()
  }

  static getInstance(): EnvConfig {
    if (!EnvConfig.instance) {
      EnvConfig.instance = new EnvConfig()
    }
    return EnvConfig.instance
  }

  private init() {
    this.config = {
      env: getEnv(),
      isDev: isDev(),
      isStaging: isStaging(),
      isProd: isProd(),
      ...getAllEnvVars(),
    }
  }

  get(key: string): any {
    return this.config[key]
  }

  getAll(): Record<string, any> {
    return { ...this.config }
  }

  is(key: string): boolean {
    return this.config.env === key
  }

  has(key: string): boolean {
    return key in this.config
  }
}