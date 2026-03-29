<template>
  <!-- 班次表单对话框组件 -->
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-width="labelWidth"
      label-position="right"
    >
      <!-- 列车选择 -->
      <el-form-item
        label="列车"
        prop="airplaneId"
        :required="true"
      >
        <el-select
          v-model="formData.airplaneId"
          placeholder="请选择列车"
          :disabled="isFieldDisabled('airplaneId')"
          :clearable="true"
          style="width: 100%"
          @change="handleAirplaneChange"
        >
          <el-option
            v-for="option in airplaneOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <span style="float: left">{{ option.label }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">
              {{ option.airplaneModel }}
            </span>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 线路选择 -->
      <el-form-item
        label="线路"
        prop="routeId"
        :required="true"
      >
        <el-select
          v-model="formData.routeId"
          placeholder="请选择线路"
          :disabled="isFieldDisabled('routeId')"
          :clearable="true"
          style="width: 100%"
          @change="handleRouteChange"
        >
          <el-option
            v-for="option in routeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <span style="float: left">{{ option.label }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">
              {{ option.departureStation }} - {{ option.arrivalStation }}
            </span>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 列车长 -->
      <el-form-item
        label="列车长"
        prop="conductor"
        :required="true"
      >
        <el-input
          v-model="formData.conductor"
          placeholder="请输入列车长姓名"
          :disabled="isFieldDisabled('conductor')"
          maxlength="10"
          show-word-limit
          clearable
        />
      </el-form-item>

      <!-- 余票数 -->
      <el-form-item
        label="余票数"
        prop="availableTickets"
        :required="true"
      >
        <el-input-number
          v-model="formData.availableTickets"
          :min="0"
          :max="2000"
          :step="1"
          :disabled="isFieldDisabled('availableTickets')"
          controls-position="right"
          style="width: 100%"
        />
        <div class="input-tip">余票数范围为 0-2000</div>
      </el-form-item>

      <!-- 出发时间 -->
      <el-form-item
        label="出发时间"
        prop="startTime"
        :required="true"
      >
        <el-date-picker
          v-model="formData.startTime"
          type="datetime"
          placeholder="请选择出发时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          :disabled="isFieldDisabled('startTime')"
          style="width: 100%"
          @change="handleStartTimeChange"
        />
      </el-form-item>

      <!-- 到达时间 -->
      <el-form-item
        label="到达时间"
        prop="endTime"
        :required="true"
      >
        <el-date-picker
          v-model="formData.endTime"
          type="datetime"
          placeholder="请选择到达时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          :disabled="isFieldDisabled('endTime')"
          style="width: 100%"
          @change="handleEndTimeChange"
        />
      </el-form-item>

      <!-- 时间验证提示 -->
      <el-form-item v-if="timeValidationMessage" class="time-validation-item">
        <el-alert
          :title="timeValidationMessage"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form-item>

      <!-- 信息预览 -->
      <el-form-item v-if="showPreview" label="信息预览" class="preview-item">
        <el-card shadow="never" class="preview-card">
          <div class="preview-content">
            <div class="preview-row">
              <span class="preview-label">班次信息：</span>
              <span class="preview-value">{{ getPreviewText() }}</span>
            </div>
            <div v-if="durationText" class="preview-row">
              <span class="preview-label">运行时长：</span>
              <span class="preview-value">{{ durationText }}</span>
            </div>
          </div>
        </el-card>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          {{ confirmText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// Props定义
const props = defineProps({
  // 控制弹窗显示
  visible: {
    type: Boolean,
    default: false
  },
  // 弹窗标题
  title: {
    type: String,
    default: '班次表单'
  },
  // 表单数据
  formData: {
    type: Object,
    default: () => ({
      id: '',
      airplaneId: '',
      routeId: '',
      conductor: '',
      availableTickets: 0,
      startTime: '',
      endTime: ''
    })
  },
  // 列车选项列表
  airplaneOptions: {
    type: Array,
    default: () => []
  },
  // 线路选项列表
  routeOptions: {
    type: Array,
    default: () => []
  },
  // 表单验证规则
  rules: {
    type: Object,
    default: () => ({
      airplaneId: [{ required: true, message: '请选择列车', trigger: 'change' }],
      routeId: [{ required: true, message: '请选择线路', trigger: 'change' }],
      conductor: [
        { required: true, message: '请输入列车长姓名', trigger: 'blur' },
        { min: 2, max: 10, message: '列车长姓名长度在 2 到 10 个字符', trigger: 'blur' }
      ],
      availableTickets: [
        { required: true, message: '请输入余票数', trigger: 'blur' },
        { type: 'number', min: 0, max: 2000, message: '余票数必须在 0 到 2000 之间', trigger: 'blur' }
      ],
      startTime: [{ required: true, message: '请选择出发时间', trigger: 'change' }],
      endTime: [{ required: true, message: '请选择到达时间', trigger: 'change' }]
    })
  },
  // 是否为编辑模式
  isEdit: {
    type: Boolean,
    default: false
  },
  // 确认按钮文本
  confirmText: {
    type: String,
    default: '确定'
  },
  // 弹窗宽度
  width: {
    type: String,
    default: '600px'
  },
  // 标签宽度
  labelWidth: {
    type: String,
    default: '100px'
  },
  // 是否在关闭时销毁内容
  destroyOnClose: {
    type: Boolean,
    default: true
  },
  // 点击遮罩层是否可以关闭
  closeOnClickModal: {
    type: Boolean,
    default: false
  },
  // 是否显示加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 是否显示信息预览
  showPreview: {
    type: Boolean,
    default: true
  },
  // 禁用的字段
  disabledFields: {
    type: Array,
    default: () => []
  }
})

// Emits定义
const emit = defineEmits([
  'update:visible',
  'submit',
  'cancel',
  'close',
  'airplane-change',
  'route-change',
  'start-time-change',
  'end-time-change'
])

// 响应式数据
const formRef = ref(null)
const timeValidationMessage = ref('')

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 计算运行时长
const durationText = computed(() => {
  if (!props.formData.startTime || !props.formData.endTime) return ''
  
  const start = new Date(props.formData.startTime)
  const end = new Date(props.formData.endTime)
  
  if (end <= start) return ''
  
  const duration = end - start
  const hours = Math.floor(duration / (1000 * 60 * 60))
  const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60))
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else {
    return `${minutes}分钟`
  }
})

// 监听表单数据变化，验证时间逻辑
watch(
  () => [props.formData.startTime, props.formData.endTime],
  ([start, end]) => {
    if (start && end) {
      const startTime = new Date(start)
      const endTime = new Date(end)
      
      if (endTime <= startTime) {
        timeValidationMessage.value = '到达时间必须晚于出发时间'
      } else {
        timeValidationMessage.value = ''
      }
    } else {
      timeValidationMessage.value = ''
    }
  }
)

/**
 * 检查字段是否禁用
 * @param {string} fieldName - 字段名
 * @returns {boolean} 是否禁用
 */
const isFieldDisabled = (fieldName) => {
  return props.disabledFields.includes(fieldName)
}

/**
 * 获取预览文本
 * @returns {string} 预览文本
 */
const getPreviewText = () => {
  const airplaneName = props.airplaneOptions.find(item => item.value === props.formData.airplaneId)?.label || '未知列车'
  const routeName = props.routeOptions.find(item => item.value === props.formData.routeId)?.label || '未知线路'
  return `${airplaneName} - ${routeName}`
}

/**
 * 处理列车选择变化
 * @param {number} value - 选中的列车ID
 */
const handleAirplaneChange = (value) => {
  emit('airplane-change', value)
}

/**
 * 处理线路选择变化
 * @param {number} value - 选中的线路ID
 */
const handleRouteChange = (value) => {
  emit('route-change', value)
}

/**
 * 处理出发时间变化
 * @param {string} value - 出发时间
 */
const handleStartTimeChange = (value) => {
  emit('start-time-change', value)
}

/**
 * 处理到达时间变化
 * @param {string} value - 到达时间
 */
const handleEndTimeChange = (value) => {
  emit('end-time-change', value)
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    // 验证时间逻辑
    if (props.formData.startTime && props.formData.endTime) {
      const startTime = new Date(props.formData.startTime)
      const endTime = new Date(props.formData.endTime)
      
      if (endTime <= startTime) {
        ElMessage.warning('到达时间必须晚于出发时间')
        return
      }
    }
    
    // 验证表单
    await formRef.value.validate()
    // 触发提交事件
    emit('submit', { ...props.formData })
  } catch (error) {
    // 表单验证失败，不进行任何操作
    console.log('表单验证失败:', error)
  }
}

/**
 * 取消操作
 */
const handleCancel = () => {
  emit('cancel')
  dialogVisible.value = false
}

/**
 * 关闭弹窗
 */
const handleClose = () => {
  emit('close')
  // 清除表单验证状态
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  // 清除时间验证消息
  timeValidationMessage.value = ''
}

// 暴露方法给父组件
defineExpose({
  // 清除表单验证状态
  clearValidate: () => {
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  },
  // 重置表单
  resetFields: () => {
    if (formRef.value) {
      formRef.value.resetFields()
    }
  },
  // 验证时间逻辑
  validateTime: () => {
    if (props.formData.startTime && props.formData.endTime) {
      const startTime = new Date(props.formData.startTime)
      const endTime = new Date(props.formData.endTime)
      return endTime > startTime
    }
    return true
  }
})
</script>

<style scoped lang="scss">
.schedule-dialog {
  // 输入提示样式
  .input-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    line-height: 1.2;
  }
  
  // 时间验证项样式
  .time-validation-item {
    margin-bottom: 0;
    
    :deep(.el-form-item__content) {
      margin-left: 0 !important;
    }
  }
  
  // 预览项样式
  .preview-item {
    margin-bottom: 0;
    
    :deep(.el-form-item__content) {
      margin-left: 0 !important;
    }
  }
  
  // 预览卡片样式
  .preview-card {
    background-color: #f5f7fa;
    border: 1px solid #e4e7ed;
    
    .preview-content {
      .preview-row {
        display: flex;
        align-items: center;
        margin-bottom: 8px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .preview-label {
          font-weight: 500;
          color: #606266;
          margin-right: 8px;
          min-width: 80px;
        }
        
        .preview-value {
          color: #303133;
          flex: 1;
        }
      }
    }
  }
  
  // 对话框底部样式
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
  
  // 数字输入框样式
  :deep(.el-input-number) {
    width: 100%;
  }
  
  // 下拉选择器选项样式
  :deep(.el-select-dropdown__item) {
    height: auto;
    line-height: 1.5;
    padding: 8px 12px;
  }
}
</style>