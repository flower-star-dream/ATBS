<template>
    <div class="employee-info-view">
        <el-card class="page-container" shadow="hover">
            <div class="header">
                <h2>个人中心 - 员工信息</h2>
            </div>
            <!-- 水平布局，头像在左侧，表单在右侧 -->
            <div class="horizontal-layout">
                <!-- 头像部分 - 固定在左侧，增加宽度和容器底板 -->
                <div class="avatar-section">
                    <div class="avatar-container">
                        <img v-if="employeeInfo.avatar" :src="employeeInfo.avatar" class="avatar" />
                        <el-icon v-else class="avatar-uploader-icon">
                            <Plus />
                        </el-icon>
                        <el-upload class="avatar-uploader" :show-file-list="false" :auto-upload="true"
                            :action="uploadAction" name="file"
                            :headers="uploadHeaders"
                            :on-success="uploadSuccess" accept=".jpg,.jpeg,.png,.gif" :before-upload="beforeUpload">
                            <div class="avatar-upload-overlay">
                                <el-icon class="upload-icon">
                                    <Camera />
                                </el-icon>
                                <span class="upload-text">更换头像</span>
                            </div>
                        </el-upload>
                    </div>
                    <!-- 在头像下方显示昵称 -->
                    <div class="nickname-display">
                        <div class="nickname-label">昵称</div>
                        <div class="nickname-value">{{ employeeInfo.nickname || '未设置昵称' }}</div>
                    </div>
                </div>

                <!-- 表单部分 - 在右侧 -->
                <div class="form-section">
                    <el-form :model="employeeInfo" label-width="120px" size="large" class="form-container"
                        :rules="rules" ref="formRef">
                        <el-form-item label="用户名" prop="username">
                            <el-input v-model="employeeInfo.username" disabled></el-input>
                        </el-form-item>
                        <el-form-item label="员工昵称" prop="nickname">
                            <el-input v-model="employeeInfo.nickname" placeholder="请输入员工昵称"></el-input>
                        </el-form-item>
                        <el-form-item label="员工手机号" prop="phone">
                            <el-input v-model="employeeInfo.phone" placeholder="请输入手机号" type="tel"></el-input>
                        </el-form-item>
                        <el-form-item label="所属站点" prop="affiliatedSite">
                            <el-input v-model="employeeInfo.affiliatedSite" placeholder="请输入所属站点"></el-input>
                        </el-form-item>
                        <el-form-item label="权限等级" prop="permissionLevel">
                            <el-input v-model="employeeInfo.permissionLevel" disabled></el-input>
                        </el-form-item>
                        <el-form-item class="form-actions">
                            <el-button @click="cancelEdit">取消</el-button>
                            <el-button type="primary" @click="updateEmployeeInfo">提交修改</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
// 引入 Vue 相关 API
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { updateEmployeeInfoService } from '@/api/employee'
import { useOAuth2Store, useEmployeeStore } from '@/stores'
import type { EmployeeInfo } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Camera } from '@element-plus/icons-vue'
import config from '@/config'

// 定义响应式数据
const oauth2Store = useOAuth2Store()
const employeeStore = useEmployeeStore()

/**
 * 计算上传请求头
 * 包含 Authorization (Bearer token) 和 X-Client-Type
 */
const uploadHeaders = computed(() => {
  const token = oauth2Store.accessToken
  return {
    'Authorization': token ? `Bearer ${token}` : '',
    'X-Client-Type': 1
  }
})

/**
 * 计算上传地址
 * 使用配置中的 baseUrl 和 apiPrefix
 */
const uploadAction = computed(() => {
  return `${config.baseUrl}/api/base/v1/common/file/upload`
})
const formRef = ref()
// 提供初始值以避免null/undefined问题
const employeeInfo = ref<EmployeeInfo>(employeeStore.employeeInfo || {
    id: '',
    username: '',
    nickname: '',
    phone: '',
    avatar: '',
    affiliatedSite: '',
    permissionLevel: ''
})

// 存储原始员工信息，用于取消操作
const originalEmployeeInfo = ref<EmployeeInfo>({ ...employeeInfo.value })

// 表单验证规则
const rules = {
    nickname: [
        { required: true, message: '请输入员工昵称', trigger: 'blur' },
        { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
    ],
    phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ]
}

/**
 * 监听昵称变化，实时更新显示
 */
watch(() => employeeInfo.value.nickname, (newNickname) => {
    // 昵称变化时无需额外操作，视图会自动更新
})

/**
 * 头像上传前的验证
 * @param file 上传的文件
 * @returns 是否允许上传
 */
const beforeUpload = (file: File) => {
    const isIMAGE = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
    if (!isIMAGE) {
        ElMessage.error('只允许上传 JPG、PNG、GIF 格式的图片!')
        return false
    }
    const isLt2M = file.size / 1024 / 1024 < 2
    if (!isLt2M) {
        ElMessage.error('上传图片大小不能超过 2MB!')
        return false
    }
    return true
}

/**
 * 头像上传成功处理函数
 * @param res 上传成功返回的数据
 */
const uploadSuccess = (res: any) => {
    employeeInfo.value = { ...employeeInfo.value, avatar: res.data }
    ElMessage.success('头像上传成功')
}

/**
 * 取消编辑，恢复原始数据
 */
const cancelEdit = () => {
    ElMessageBox.confirm('确定要取消修改吗？未保存的更改将丢失。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        employeeInfo.value = { ...originalEmployeeInfo.value }
        formRef.value?.resetFields()
    }).catch(() => { })
}

/**
 * 更新员工信息
 */
const updateEmployeeInfo = async () => {
    try {
        // 表单验证
        await formRef.value.validate()

        // 确保employeeInfo.value存在
        if (!employeeInfo.value) {
            ElMessage.error('员工信息不存在')
            return
        }

        await updateEmployeeInfoService(employeeInfo.value)
        employeeStore.setEmployeeInfo(employeeInfo.value)
        // 更新原始数据，以便下次取消操作时使用
        originalEmployeeInfo.value = { ...employeeInfo.value }
        ElMessage.success('修改成功')
    } catch (error: any) {
        // 如果是验证失败的错误，不显示额外错误提示
        if (error.name !== 'ValidationError') {
            console.error('更新员工信息失败:', error)
            ElMessage.error('修改失败，请重试')
        }
    }
}

// 组件挂载时执行
onMounted(() => {
    // 确保originalEmployeeInfo有初始值
    originalEmployeeInfo.value = { ...employeeInfo.value }
})
</script>

<style scoped lang="scss">
@use './Employee-info-view.scss';
</style>
