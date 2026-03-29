import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  exchangeCodeForToken,
  refreshToken as refreshTokenApi,
  revokeToken,
  getUserInfo as getUserInfoApi,
  validateRedirectUri
} from '@/api/oauth2'
import type { OAuth2TokenResponse, OAuth2UserInfo } from '@/types'
import config from '@/config'

/**
 * OAuth2 授权码登录 Store
 * @description 管理 OAuth2 授权码登录流程的状态和操作
 */
export const useOAuth2Store = defineStore('oauth2', () => {
  // ==================== State ====================
  /** 访问令牌 */
  const accessToken = ref<string>(localStorage.getItem('access_token') || '')
  /** 刷新令牌 */
  const refreshTokenValue = ref<string>(localStorage.getItem('refresh_token') || '')
  /** 令牌过期时间 */
  const expiresAt = ref<number>(Number(localStorage.getItem('expires_at') || '0'))
  /** 用户信息 */
  const userInfo = ref<OAuth2UserInfo | null>(null)
  /** 登录状态 */
  const isLoggedIn = computed(() => !!accessToken.value && Date.now() < expiresAt.value)

  // ==================== Getters ====================
  /**
   * 获取当前访问令牌
   */
  const getAccessToken = computed(() => accessToken.value)

  /**
   * 获取当前刷新令牌
   */
  const getRefreshToken = computed(() => refreshTokenValue.value)

  /**
   * 获取当前用户信息
   */
  const getUserInfo = computed(() => userInfo.value)

  /**
   * 检查令牌是否即将过期（5分钟内）
   */
  const isTokenExpiringSoon = computed(() => {
    if (!expiresAt.value) return false
    const fiveMinutes = 5 * 60 * 1000
    return Date.now() + fiveMinutes > expiresAt.value
  })

  // ==================== Actions ====================

  /**
   * 生成随机状态码（防CSRF）
   * @returns 随机字符串
   */
  const generateState = (): string => {
    const array = new Uint8Array(16)
    window.crypto.getRandomValues(array)
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('')
  }

  /**
   * 生成 PKCE Code Verifier
   * @returns Code Verifier
   */
  const generateCodeVerifier = (): string => {
    const array = new Uint8Array(32)
    window.crypto.getRandomValues(array)
    return base64URLEncode(array)
  }

  /**
   * 生成 PKCE Code Challenge
   * @param verifier Code Verifier
   * @returns Code Challenge
   */
  const generateCodeChallenge = async (verifier: string): Promise<string> => {
    const encoder = new TextEncoder()
    const data = encoder.encode(verifier)
    const digest = await window.crypto.subtle.digest('SHA-256', data)
    return base64URLEncode(new Uint8Array(digest))
  }

  /**
   * Base64URL 编码
   * @param buffer 字节数组
   * @returns Base64URL 字符串
   */
  const base64URLEncode = (buffer: Uint8Array): string => {
    return btoa(String.fromCharCode(...buffer))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '')
  }

  /**
   * 构建授权端点 URL
   * 在跳转前验证重定向地址有效性
   * @param redirectUri 重定向URI
   * @returns 授权URL
   */
  const buildAuthorizeUrl = async (redirectUri: string): Promise<string> => {
    // 预请求验证重定向地址
    const isValid = await validateRedirectUri(redirectUri)
    if (!isValid) {
      throw new Error('重定向地址验证失败，请检查配置')
    }
    
    const { oauth2 } = config
    const state = generateState()
    const codeVerifier = generateCodeVerifier()
    const codeChallenge = await generateCodeChallenge(codeVerifier)

    // 保存 state 和 codeVerifier 用于后续验证
    sessionStorage.setItem('oauth2_state', state)
    sessionStorage.setItem('oauth2_code_verifier', codeVerifier)

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: oauth2.clientId,
      redirect_uri: redirectUri,
      scope: oauth2.scope,
      state: state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256'
    })

    // 使用后端托管的登录页面
    return `${oauth2.authBaseUrl}/login?${params.toString()}`
  }

  /**
   * 处理授权码回调
   * @param code 授权码
   * @param state 状态码
   * @param redirectUri 重定向URI
   */
  const handleCallback = async (
    code: string,
    state: string,
    redirectUri: string
  ): Promise<boolean> => {
    try {
      // 验证 state 防止 CSRF 攻击
      const savedState = sessionStorage.getItem('oauth2_state')
      if (savedState && savedState !== state) {
        ElMessage.error('安全验证失败，请重新登录')
        return false
      }

      // 获取 code_verifier（PKCE 需要）
      const codeVerifier = sessionStorage.getItem('oauth2_code_verifier')

      // 清除 sessionStorage 中的临时数据
      sessionStorage.removeItem('oauth2_state')
      sessionStorage.removeItem('oauth2_code_verifier')

      // 使用授权码换取令牌
      const tokenResponse: OAuth2TokenResponse = await exchangeCodeForToken(
        code,
        state,
        redirectUri,
        codeVerifier || ''
      )

      // 保存令牌
      setTokens(tokenResponse)

      // 获取用户信息
      await fetchUserInfo()

      ElMessage.success('登录成功')
      return true
    } catch (error: any) {
      ElMessage.error(error?.message || '登录失败，请重试')
      return false
    }
  }

  /**
   * 设置令牌
   * @param tokenResponse 令牌响应
   */
  const setTokens = (tokenResponse: OAuth2TokenResponse) => {
    accessToken.value = tokenResponse.access_token
    refreshTokenValue.value = tokenResponse.refresh_token

    // 计算过期时间戳
    const expiresInMs = tokenResponse.expires_in * 1000
    expiresAt.value = Date.now() + expiresInMs

    // 持久化存储
    localStorage.setItem('access_token', tokenResponse.access_token)
    localStorage.setItem('refresh_token', tokenResponse.refresh_token)
    localStorage.setItem('expires_at', expiresAt.value.toString())
  }

  /**
   * 获取用户信息
   */
  const fetchUserInfo = async () => {
    try {
      const info = await getUserInfoApi()
      userInfo.value = info as OAuth2UserInfo
      localStorage.setItem('user_info', JSON.stringify(info))
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  /**
   * 刷新访问令牌
   */
  const doRefreshToken = async (): Promise<boolean> => {
    try {
      if (!refreshTokenValue.value) {
        return false
      }

      const tokenResponse = await refreshTokenApi(refreshTokenValue.value)
      setTokens(tokenResponse)
      return true
    } catch (error) {
      console.error('刷新令牌失败:', error)
      // 刷新失败，清除登录状态
      logout()
      return false
    }
  }

  /**
   * 登出
   */
  const logout = async () => {
    try {
      // 撤销令牌（可选，根据业务需求）
      if (accessToken.value) {
        await revokeToken(accessToken.value, 'access_token')
      }
    } catch (error) {
      console.error('撤销令牌失败:', error)
    } finally {
      // 清除状态
      accessToken.value = ''
      refreshTokenValue.value = ''
      expiresAt.value = 0
      userInfo.value = null

      // 清除存储
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('expires_at')
      localStorage.removeItem('user_info')

      // 清除 sessionStorage
      sessionStorage.removeItem('oauth2_state')
      sessionStorage.removeItem('oauth2_code_verifier')
    }
  }

  /**
   * 初始化（从 localStorage 恢复状态）
   */
  const init = () => {
    const savedAccessToken = localStorage.getItem('access_token')
    const savedRefreshToken = localStorage.getItem('refresh_token')
    const savedExpiresAt = localStorage.getItem('expires_at')
    const savedUserInfo = localStorage.getItem('user_info')

    if (savedAccessToken) {
      accessToken.value = savedAccessToken
    }
    if (savedRefreshToken) {
      refreshTokenValue.value = savedRefreshToken
    }
    if (savedExpiresAt) {
      expiresAt.value = Number(savedExpiresAt)
    }
    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo)
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  }

  return {
    // State
    accessToken,
    refreshTokenValue,
    expiresAt,
    userInfo,
    isLoggedIn,

    // Getters
    getAccessToken,
    getRefreshToken,
    getUserInfo,
    isTokenExpiringSoon,

    // Actions
    buildAuthorizeUrl,
    handleCallback,
    doRefreshToken,
    logout,
    init
  }
})

export default useOAuth2Store
