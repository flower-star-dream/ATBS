import axios from 'axios'
import type * as T from '@/types/auth'
import config from '@/config'

/**
 * OAuth2认证相关API
 * 注意：OAuth2接口不遵循新的路径规范，保持原有格式
 */

// 创建独立的axios实例用于OAuth2请求（不添加默认拦截器）
const oauth2Request = axios.create({
  baseURL: config.baseUrl,
  timeout: config.timeout,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
})

/**
 * OAuth2令牌端点 - RFC 6749 3.2
 * POST /oauth/token
 * @param params 令牌请求参数
 * @returns 令牌响应
 */
export const oauth2Token = (params: {
  grant_type: string
  client_id: string
  client_secret: string
  scope?: string
  username?: string
  password?: string
  refresh_token?: string
}): Promise<T.LoginTokenRES> => {
  const formData = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, value)
    }
  })
  return oauth2Request.post('/oauth/token', formData).then(res => res.data)
}

/**
 * 密码授权登录
 * @param username 用户名或手机号
 * @param password 密码（已加密）
 * @returns 登录令牌响应
 */
export const passwordLogin = (username: string, password: string): Promise<T.LoginTokenRES> => {
  return oauth2Token({
    grant_type: 'password',
    client_id: 'admin_web',
    client_secret: 'admin_secret',
    scope: 'read write',
    username,
    password
  })
}

/**
 * 刷新令牌
 * @param refreshToken 刷新令牌
 * @returns 新令牌响应
 */
export const refreshToken = (refreshToken: string): Promise<T.TokenRES> => {
  return oauth2Token({
    grant_type: 'refresh_token',
    client_id: 'admin_web',
    client_secret: 'admin_secret',
    refresh_token: refreshToken
  })
}

/**
 * OAuth2令牌撤销 - RFC 7009
 * POST /oauth/revoke
 * @param token 要撤销的令牌
 * @param tokenTypeHint 令牌类型提示（access_token/refresh_token）
 */
export const revokeToken = (token: string, tokenTypeHint?: string): Promise<void> => {
  const formData = new URLSearchParams()
  formData.append('token', token)
  if (tokenTypeHint) {
    formData.append('token_type_hint', tokenTypeHint)
  }
  return oauth2Request.post('/oauth/revoke', formData).then(res => res.data)
}

/**
 * 获取OAuth2服务端元数据 - RFC 8414
 * GET /.well-known/oauth-authorization-server
 * @returns 授权服务器元数据
 */
export const getOAuth2Metadata = (): Promise<T.AuthorizationServerMetadata> => {
  return oauth2Request.get('/.well-known/oauth-authorization-server').then(res => res.data)
}

/**
 * 获取JWK公钥集 - RFC 7517
 * GET /.well-known/jwks.json
 * @returns JWK公钥集
 */
export const getJwks = (): Promise<Record<string, any>> => {
  return oauth2Request.get('/.well-known/jwks.json').then(res => res.data)
}
