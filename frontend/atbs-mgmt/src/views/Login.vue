<template>
  <div class="login-container">
    <div class="login-form">
      <div class="login-header">
        <div class="logo">
          <h2>火车订票系统 - 员工登录</h2>
        </div>
      </div>

      <div class="oauth2-login">
        <p class="login-desc">请使用统一认证平台登录</p>
        <el-button
          type="primary"
          size="large"
          style="width: 100%"
          :loading="loading"
          @click="handleOAuth2Login"
        >
          <el-icon class="login-icon"><User /></el-icon>
          前往登录
        </el-button>
      </div>

      <div class="footer">
        <p>© 2026 火车订票系统 - 版权所有</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 登录页面
 * @description 使用 OAuth2 授权码登录方式，统一认证
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { useOAuth2Store } from '@/stores'

const oauth2Store = useOAuth2Store()
const loading = ref(false)

/**
 * 处理 OAuth2 授权码登录
 * 跳转到后端托管的登录页面
 */
const handleOAuth2Login = async () => {
  try {
    loading.value = true

    // 构建回调地址
    const redirectUri = `${window.location.origin}/oauth2/callback`

    // 构建授权URL
    const authorizeUrl = await oauth2Store.buildAuthorizeUrl(redirectUri)

    // 跳转到后端登录页面
    window.location.href = authorizeUrl
  } catch (error: any) {
    ElMessage.error(error?.message || '登录跳转失败')
    loading.value = false
  }
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

.logo h2 {
  color: #333;
  margin: 0;
  font-size: 24px;
  font-weight: 500;
}

.oauth2-login {
  margin-bottom: 20px;
}

.login-desc {
  text-align: center;
  color: #666;
  margin-bottom: 16px;
  font-size: 14px;
}

.login-icon {
  margin-right: 8px;
}

.footer {
  text-align: center;
  margin-top: 24px;
  color: #999;
  font-size: 12px;
}
</style>