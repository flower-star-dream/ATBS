import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginForm, LoginTokenRES, UserInfoRES } from '@/types/auth'
import { passwordLogin, revokeToken } from '@/api/auth'

/**
 * 认证状态管理Store
 * 管理用户登录状态、令牌信息和用户信息
 */
export const useAuthStore = defineStore('auth', () => {
  // 访问令牌
  const accessToken = ref(localStorage.getItem('access_token') || '')
  // 刷新令牌
  const refreshToken = ref(localStorage.getItem('refresh_token') || '')
  // 令牌过期时间
  const expiresIn = ref<number>(parseInt(localStorage.getItem('expires_in') || '0'))
  // 用户信息
  const userInfo = ref<Partial<UserInfoRES> | null>(null)

  /**
   * 登录动作
   * @param loginForm 登录表单数据
   */
  const loginAction = async (loginForm: LoginForm) => {
    try {
      // 判断输入是否为手机号格式
      const isPhone = /^1[3-9]\d{9}$/.test(loginForm.username)
      // 根据输入类型构建不同的登录参数
      const username = isPhone ? loginForm.phone : loginForm.username

      const response: LoginTokenRES = await passwordLogin(username, loginForm.password)

      // 保存令牌信息
      accessToken.value = response.access_token
      refreshToken.value = response.refresh_token
      expiresIn.value = response.expires_in

      // 保存到localStorage
      localStorage.setItem('access_token', response.access_token)
      localStorage.setItem('refresh_token', response.refresh_token)
      localStorage.setItem('expires_in', String(response.expires_in))
      localStorage.setItem('token', response.access_token) // 兼容旧代码

      // 设置用户信息
      userInfo.value = {
        user_id: response.id,
        preferred_username: response.username,
        sub: response.id
      }

      localStorage.setItem('user_info', JSON.stringify(userInfo.value))

      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 设置用户信息
   * @param info 用户信息
   */
  const setUserInfo = (info: Partial<UserInfoRES>) => {
    userInfo.value = info
    if (info) {
      localStorage.setItem('user_info', JSON.stringify(info))
    }
  }

  /**
   * 登出动作
   */
  const logoutAction = async () => {
    try {
      // 调用后端撤销令牌接口
      if (accessToken.value) {
        await revokeToken(accessToken.value, 'access_token')
      }
    } catch (error) {
      console.error('撤销令牌失败:', error)
    } finally {
      // 清除所有状态
      accessToken.value = ''
      refreshToken.value = ''
      expiresIn.value = 0
      userInfo.value = null

      // 清除localStorage
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('expires_in')
      localStorage.removeItem('token')
      localStorage.removeItem('user_info')
      localStorage.removeItem('employeeInfo') // 兼容旧代码
    }
  }

  /**
   * 更新访问令牌
   * @param token 新访问令牌
   */
  const updateAccessToken = (token: string) => {
    accessToken.value = token
    localStorage.setItem('access_token', token)
    localStorage.setItem('token', token) // 兼容旧代码
  }

  /**
   * 检查是否已登录
   */
  const isAuthenticated = () => {
    return !!accessToken.value
  }

  return {
    accessToken,
    refreshToken,
    expiresIn,
    userInfo,
    loginAction,
    setUserInfo,
    logoutAction,
    updateAccessToken,
    isAuthenticated
  }
})

export default useAuthStore
