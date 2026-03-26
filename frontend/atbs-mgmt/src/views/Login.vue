<template>
  <div class="login-container">
    <div class="login-form">
      <div class="login-header">
        <div class="logo">
          <!-- <img src="/vite.svg" alt="logo" /> -->
          <h2>飞机订票系统 - 员工登录</h2>
        </div>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form-content"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入员工账号/手机号"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores'
import { md5ToHex } from '@/utils/md5'
import { strToBase64 } from '@/utils/base64'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  phone: '',
  password: ''
})

// 手机号验证规则
const validatePhone = (rule: any, value: string, callback: any) => {
  // 简单的手机号正则验证
  const phoneRegex = /^1[3-9]\d{9}$/;
  // 如果输入内容符合手机号格式，通过验证
  if (value && phoneRegex.test(value)) {
    callback();
  } else {
    // 如果不符合手机号格式，让用户名验证规则继续验证
    callback(new Error('手机号格式不正确，请重新输入'));
  }
};

// 员工账号验证规则
  const validateEmployeename = (rule: any, value: string, callback: any) => {
  // 简单的手机号正则验证
  const phoneRegex = /^1[3-9]\d{9}$/;
  // 如果输入内容符合手机号格式，不进行用户名验证
  if (value && phoneRegex.test(value)) {
    callback();
  } else if (value && (value.length < 3 || value.length > 20)) {
    callback(new Error('员工账号长度在 3 到 20 个字符'));
  } else {
    callback();
  }
};

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入员工账号/手机号', trigger: 'blur' },
    // 使用自定义验证规则，支持用户名或手机号格式
    { validator: validateEmployeename || validatePhone, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 判断输入是否为手机号格式
        const isPhone = /^1[3-9]\d{9}$/.test(loginForm.username)
        // 根据输入类型构建不同的登录参数，只传递必要字段
        const loginParams = {
          password: md5ToHex(strToBase64(loginForm.password)),
          // 如果是手机号，只传递phone字段；否则只传递username字段
          ...(isPhone ? { phone: loginForm.username, username: ''} : { username: loginForm.username, phone: '' })
        }

        await authStore.loginAction(loginParams)
      ElMessage.success('登录成功')
      router.push('/home')
      } catch (error: any) {
        ElMessage.error(error?.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-form {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo img {
  width: 60px;
  height: 60px;
  margin-bottom: 10px;
}

.logo h2 {
  color: #333;
  margin: 0;
  font-size: 24px;
  font-weight: 500;
}

.login-form-content {
  width: 100%;
}
</style>
