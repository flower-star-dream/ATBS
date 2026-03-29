<template>
  <div class="callback-container">
    <div class="callback-content">
      <el-result
        v-if="loading"
        icon="info"
        title="正在处理登录..."
        sub-title="请稍候，正在完成身份验证"
      >
        <template #icon>
          <el-icon class="loading-icon"><Loading /></el-icon>
        </template>
      </el-result>

      <el-result
        v-else-if="success"
        icon="success"
        title="登录成功"
        sub-title="正在跳转至首页..."
      >
        <template #extra>
          <el-button type="primary" @click="goToHome">立即跳转</el-button>
        </template>
      </el-result>

      <el-result
        v-else
        icon="error"
        title="登录失败"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button @click="goToLogin">返回登录页</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * OAuth2 授权码回调页面
 * @description 处理授权码回调，用授权码换取访问令牌
 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { useOAuth2Store } from '@/stores'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const oauth2Store = useOAuth2Store()

// 状态
const loading = ref(true)
const success = ref(false)
const errorMessage = ref('登录过程中发生错误')

/**
 * 处理回调
 */
const handleCallback = async () => {
  try {
    // 从 URL 参数中获取授权码和状态
    const code = route.query.code as string
    const state = route.query.state as string
    const error = route.query.error as string
    const errorDescription = route.query.error_description as string

    // 检查是否有错误
    if (error) {
      errorMessage.value = errorDescription || '授权失败'
      loading.value = false
      success.value = false
      return
    }

    // 检查授权码是否存在
    if (!code) {
      errorMessage.value = '未收到授权码'
      loading.value = false
      success.value = false
      return
    }

    // 获取重定向URI（必须与请求时一致）
    const redirectUri = `${window.location.origin}/oauth2/callback`

    // 使用授权码换取令牌
    const result = await oauth2Store.handleCallback(code, state, redirectUri)

    if (result) {
      success.value = true
      loading.value = false

      // 延迟跳转到首页
      setTimeout(() => {
        goToHome()
      }, 1500)
    } else {
      errorMessage.value = '令牌交换失败'
      loading.value = false
      success.value = false
    }
  } catch (error: any) {
    console.error('回调处理失败:', error)
    errorMessage.value = error?.message || '登录处理失败'
    loading.value = false
    success.value = false
  }
}

/**
 * 跳转到首页
 */
const goToHome = () => {
  router.push('/home')
}

/**
 * 跳转到登录页
 */
const goToLogin = () => {
  router.push('/login')
}

// 页面加载时处理回调
onMounted(() => {
  handleCallback()
})
</script>

<style scoped>
.callback-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.callback-content {
  width: 100%;
  max-width: 480px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.loading-icon {
  font-size: 48px;
  color: #667eea;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
