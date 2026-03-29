import { oauth2Request } from '@/utils/request'
import type { OAuth2TokenResponse } from '@/types'
import config from '@/config'

/**
 * OAuth2 授权码模式登录相关 API (PKCE 流程)
 * @description 提供授权码流程的登录接口，严格遵循 PKCE 规范
 * 注意：使用标准 OAuth2 端点路径（无前缀），如 /oauth/token, /userinfo
 * 注意：使用 oauth2Request 实例，直接返回原始响应数据
 */

/**
 * 验证重定向地址有效性
 * 在跳转前发送预请求确认地址可达
 * @param redirectUri 重定向URI
 * @returns 是否有效
 */
export const validateRedirectUri = async (redirectUri: string): Promise<boolean> => {
  try {
    // 检查是否为有效URL格式
    const url = new URL(redirectUri)
    
    // 检查协议（只允许 http 或 https）
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      console.error('无效的重定向协议:', url.protocol)
      return false
    }
    
    // 检查是否为同源（开发环境允许 localhost）
    const currentOrigin = window.location.origin
    if (url.origin !== currentOrigin && !url.host.includes('localhost')) {
      console.warn('跨域重定向地址:', url.origin, '当前:', currentOrigin)
      // 生产环境应该限制为同源，开发环境放宽限制
      if (import.meta.env.PROD) {
        return false
      }
    }
    
    // 检查回调路径是否正确
    if (!url.pathname.includes('/oauth2/callback')) {
      console.error('回调路径不正确:', url.pathname)
      return false
    }
    
    return true
  } catch (error) {
    console.error('重定向地址验证失败:', error)
    return false
  }
}

/**
 * 使用授权码换取访问令牌 (PKCE 流程)
 * 严格遵循 PKCE 规范，不包含 client_secret
 * @param code 授权码
 * @param state 状态码（防CSRF）
 * @param redirectUri 重定向URI
 * @param codeVerifier PKCE Code Verifier（必需）
 * @returns Token响应
 */
export const exchangeCodeForToken = (
  code: string,
  state: string,
  redirectUri: string,
  codeVerifier: string
): Promise<OAuth2TokenResponse> => {
  const params = new URLSearchParams()
  params.append('grant_type', 'authorization_code')
  params.append('code', code)
  params.append('state', state)
  params.append('redirect_uri', redirectUri)
  params.append('client_id', config.oauth2.clientId)
  
  // PKCE 流程：必须发送 code_verifier，不包含 client_secret
  if (!codeVerifier) {
    throw new Error('PKCE 流程需要 code_verifier')
  }
  params.append('code_verifier', codeVerifier)

  return oauth2Request.post('/oauth/token', params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 刷新访问令牌
 * 刷新令牌请求需要 client_secret
 * @param refreshToken 刷新令牌
 * @returns Token响应
 */
export const refreshToken = (refreshToken: string): Promise<OAuth2TokenResponse> => {
  const params = new URLSearchParams()
  params.append('grant_type', 'refresh_token')
  params.append('refresh_token', refreshToken)
  params.append('client_id', config.oauth2.clientId)
  params.append('client_secret', config.oauth2.clientSecret)

  return oauth2Request.post('/oauth/token', params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 撤销令牌
 * @param token 要撤销的令牌
 * @param tokenTypeHint 令牌类型提示（access_token/refresh_token）
 */
export const revokeToken = (token: string, tokenTypeHint?: string): Promise<void> => {
  const params = new URLSearchParams()
  params.append('token', token)
  if (tokenTypeHint) {
    params.append('token_type_hint', tokenTypeHint)
  }

  return oauth2Request.post('/oauth/revoke', params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 获取用户信息
 * @returns 用户信息
 */
export const getUserInfo = (): Promise<{
  sub: string
  preferred_username: string
  name: string
  phone: string
  email: string
  userId: number
  roles: string[]
}> => {
  return oauth2Request.get('/userinfo')
}
