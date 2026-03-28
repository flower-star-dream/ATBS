import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginForm, EmployeeInfo } from '@/types/employee'
import { login } from '@/api/employee'

export const useEmployeeStore = defineStore('employee', () => {
  // 初始化时从localStorage读取数据
  const token = ref(localStorage.getItem('token') || '')
  const employeeInfo = ref<EmployeeInfo | null>()

  const loginAction = async (loginForm: LoginForm) => {
    try {
      // 根据后端返回的LoginRES结构，直接从response中获取token
      // 因为request.ts的响应拦截器已经返回了data部分
      const response = await login(loginForm)
      token.value = response.token
      // 设置用户信息，提供完整的EmployeeInfo类型属性
      employeeInfo.value = {
        id: response.id,
        username: response.username,
        nickname: '', // 提供默认值
        phone: '', // 提供默认值
        avatar: '', // 提供默认值
        affiliatedSite: '', // 提供默认值
        permissionLevel: '' // 提供默认值
      }
      // 手动保存到localStorage
      localStorage.setItem('token', response.token)
      localStorage.setItem('employeeInfo', JSON.stringify(employeeInfo.value))
      return response
    } catch (error) {
      throw error
    }
  }

  const setEmployeeInfo = (newEmployeeInfo: EmployeeInfo) => {
    employeeInfo.value = newEmployeeInfo
    // 手动保存到localStorage
    if (newEmployeeInfo) {
      localStorage.setItem('employeeInfo', JSON.stringify(newEmployeeInfo))
    }
  }

  const logoutAction = () => {
    token.value = ''
    employeeInfo.value = null
    // 手动从localStorage移除
    localStorage.removeItem('token')
    localStorage.removeItem('employeeInfo')
  }

  return {
    token,
    employeeInfo,
    setEmployeeInfo,
    loginAction,
    logoutAction
  }
})

export default useEmployeeStore