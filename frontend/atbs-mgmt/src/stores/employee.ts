import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { EmployeeInfo } from '@/types/employee'
import { getEmployeeInfoService } from '@/api/employee'

/**
 * 员工状态管理Store
 * 管理员工信息相关业务
 */
export const useEmployeeStore = defineStore('employee', () => {
  // 员工信息
  const employeeInfo = ref<EmployeeInfo | null>(null)

  /**
   * 获取员工信息
   */
  const fetchEmployeeInfo = async () => {
    try {
      const response = await getEmployeeInfoService()
      employeeInfo.value = response
      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 设置员工信息
   * @param newEmployeeInfo 员工信息
   */
  const setEmployeeInfo = (newEmployeeInfo: EmployeeInfo) => {
    employeeInfo.value = newEmployeeInfo
  }

  /**
   * 清除员工信息
   */
  const clearEmployeeInfo = () => {
    employeeInfo.value = null
  }

  return {
    employeeInfo,
    fetchEmployeeInfo,
    setEmployeeInfo,
    clearEmployeeInfo
  }
})

export default useEmployeeStore
