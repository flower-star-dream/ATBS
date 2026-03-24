<template>
  <div class="reset-password-container">
    <div class="reset-password-form">
      <h2 class="form-title">重置密码</h2>
      <el-form
        :model="resetPasswordForm"
        ref="resetPasswordRef"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input
            v-model="resetPasswordForm.oldPwd"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input
            v-model="resetPasswordForm.newPwd"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input
            v-model="resetPasswordForm.confirmPwd"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleResetPassword" :loading="loading">
            确认重置
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { resetPasswordService } from '@/api/employee'
import type { ResetPasswordForm } from '@/types/employee'
import { useEmployeeStore } from '@/stores/employee'
import { useRouter } from 'vue-router'
import { strToBase64 } from '@/utils/base64'
import { md5ToHex } from '@/utils/md5'

// 初始化表单数据
const resetPasswordForm = reactive<ResetPasswordForm>({
  id: '',
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

// 表单引用
const resetPasswordRef = ref<FormInstance>()

// 加载状态
const loading = ref(false)

// 员工 store
const employeeStore = useEmployeeStore()

// 路由
const router = useRouter()

// 表单验证规则
const rules = reactive<FormRules>({
  oldPwd: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === resetPasswordForm.oldPwd) {
          callback(new Error('新密码不能与旧密码相同'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPasswordForm.newPwd) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

/**
 * 初始化员工ID
 */
const initUserId = () => {
  // 从员工store中获取员工ID
  if (employeeStore.employeeInfo && employeeStore.employeeInfo.id) {
    resetPasswordForm.id = employeeStore.employeeInfo.id
  } else {
    // 尝试从localStorage中获取员工信息
    const storedEmployeeInfo = localStorage.getItem('employeeInfo')
    if (storedEmployeeInfo) {
      const employeeInfo = JSON.parse(storedEmployeeInfo)
      resetPasswordForm.id = employeeInfo.id || ''
    }
  }
  
  // 如果仍然没有获取到员工ID，提示用户重新登录
  if (!resetPasswordForm.id) {
    ElMessage.error('员工信息获取失败，请重新登录')
    router.push('/login')
  }
}

/**
 * 处理重置密码
 */
const handleResetPassword = async () => {
  // 表单验证
  if (!resetPasswordRef.value) return
  
  try {
    await resetPasswordRef.value.validate()
    loading.value = true
    resetPasswordForm.oldPwd = md5ToHex(strToBase64(resetPasswordForm.oldPwd))
    resetPasswordForm.newPwd = md5ToHex(strToBase64(resetPasswordForm.newPwd))
    resetPasswordForm.confirmPwd = md5ToHex(strToBase64(resetPasswordForm.confirmPwd))

    // 调用重置密码接口
    await resetPasswordService(resetPasswordForm)
    ElMessage.success('密码重置成功，请重新登录')
    resetPasswordRef.value.resetFields()
    employeeStore.logoutAction()
    router.push('/login')
  } catch (error: any) {
    ElMessage.error(error.message || error.msg || '密码重置失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 处理取消
 */
const handleCancel = () => {
  // 重置表单
  resetPasswordRef.value?.resetFields()
  // 返回上一页
  router.back()
}

// 组件挂载时初始化员工ID
onMounted(() => {
  initUserId()
})
</script>

<style lang="scss" scoped>
@use './Employee-resetPwd-view.scss';
</style>