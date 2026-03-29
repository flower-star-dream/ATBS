/**
 * 客户端类型枚举
 * 用于标识不同客户端来源
 */
export enum ClientType {
  /** 管理后台Web端 */
  ADMIN_WEB = 1,
  /** 小程序端 */
  APPLET = 2
}

/**
 * 客户端类型映射
 * 用于显示客户端类型名称
 */
export const ClientTypeMap = {
  [ClientType.ADMIN_WEB]: '管理后台Web端',
  [ClientType.APPLET]: '小程序端'
}
