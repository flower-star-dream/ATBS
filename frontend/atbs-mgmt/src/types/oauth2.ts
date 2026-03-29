/**
 * OAuth2 相关类型定义
 * @description 定义授权码登录流程所需的数据类型
 */

/**
 * OAuth2 Token 响应
 */
export interface OAuth2TokenResponse {
  /** 访问令牌 */
  access_token: string
  /** 令牌类型 */
  token_type: string
  /** 过期时间（秒） */
  expires_in: number
  /** 刷新令牌 */
  refresh_token: string
  /** 权限范围 */
  scope: string
  /** 建议刷新标记 */
  refresh_recommended?: boolean
}

/**
 * OAuth2 授权码登录参数
 */
export interface OAuth2LoginParams {
  /** 客户端ID */
  clientId: string
  /** 重定向URI */
  redirectUri: string
  /** 权限范围 */
  scope: string
  /** 状态码（防CSRF） */
  state: string
  /** PKCE Code Challenge */
  codeChallenge?: string
  /** PKCE Code Challenge Method */
  codeChallengeMethod?: string
}

/**
 * OAuth2 用户信息响应
 */
export interface OAuth2UserInfo {
  /** 用户唯一标识 */
  sub: string
  /** 首选用户名 */
  preferred_username: string
  /** 姓名 */
  name: string
  /** 手机号 */
  phone: string
  /** 邮箱 */
  email: string
  /** 用户ID */
  userId: number
  /** 角色列表 */
  roles: string[]
  /** 更新时间戳 */
  updated_at?: number
}

/**
 * OAuth2 授权码回调参数
 */
export interface OAuth2CallbackParams {
  /** 授权码 */
  code?: string
  /** 状态码 */
  state?: string
  /** 错误码 */
  error?: string
  /** 错误描述 */
  error_description?: string
}
