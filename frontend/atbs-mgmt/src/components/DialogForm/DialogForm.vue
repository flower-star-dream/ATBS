<template>
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
    >
      <template v-for="field in fields" :key="field.prop">
        <!-- 输入框 -->
        <el-form-item
          v-if="field.type === 'input'"
          :label="field.label"
          :prop="field.prop"
          :required="isFieldRequired(field)"
        >
          <el-input
            v-if="field.inputType !== 'number'"
            v-model="formData[field.prop]"
            :placeholder="field.placeholder || `请输入${field.label}`"
            :disabled="isFieldDisabled(field)"
            :type="field.inputType || 'text'"
            :maxlength="field.maxlength"
            :show-word-limit="field.showWordLimit || false"
            :clearable="field.clearable || false"
          />
          <el-input
            v-if="field.inputType === 'number'"
            v-model.number="formData[field.prop]"
            :placeholder="field.placeholder || `请输入${field.label}`"
            :disabled="isFieldDisabled(field)"
            :type="field.inputType || 'number'"
            :maxlength="field.maxlength"
            :show-word-limit="field.showWordLimit || false"
            :clearable="field.clearable || false"
          />
        </el-form-item>
        
        <!-- 选择器 -->
        <el-form-item
          v-else-if="field.type === 'select'"
          :label="field.label"
          :prop="field.prop"
          :required="isFieldRequired(field)"
        >
          <el-select
            v-model="formData[field.prop]"
            :placeholder="field.placeholder || `请选择${field.label}`"
            :disabled="isFieldDisabled(field)"
            :clearable="field.clearable || false"
            :filterable="field.filterable || false"
            :remote="field.remote || false"
            :remote-method="field.remoteMethod || null"
            :loading="field.loading || false"
            :remote-show-suffix="field.remote || false"
            :style="{ width: field.width || '100%' }"
            @visible-change="(visible) => handleRemoteSelectVisibleChange(field, visible)"
          >
            <el-option
              v-for="option in (isRef(field.options) ? field.options.value : field.options)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        
        <!-- 开关 -->
        <el-form-item
          v-else-if="field.type === 'switch'"
          :label="field.label"
          :prop="field.prop"
        >
          <el-switch
            v-model="formData[field.prop]"
            :active-value="field.activeValue || true"
            :inactive-value="field.inactiveValue || false"
            :active-text="field.activeText"
            :inactive-text="field.inactiveText"
            :disabled="isFieldDisabled(field)"
          />
        </el-form-item>
        
        <!-- 自定义内容 -->
        <el-form-item
          v-else-if="field.type === 'custom'"
          :label="field.label"
          :prop="field.prop"
          :required="isFieldRequired(field)"
        >
          <slot :name="`field-${field.prop}`">{{ field.defaultContent }}</slot>
        </el-form-item>
      </template>
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
import { ref, watch, computed, isRef } from 'vue'
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
    default: '表单'
  },
  // 表单数据
  formData: {
    type: Object,
    default: () => ({})
  },
  // 表单项配置
  fields: {
    type: Array,
    default: () => []
  },
  // 表单验证规则
  rules: {
    type: Object,
    default: () => ({})
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
    default: '120px'
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
  }
})

// Emits定义
const emit = defineEmits([
  'update:visible',
  'submit',
  'cancel',
  'close'
])

// 响应式数据
const formRef = ref(null)
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 监听表单数据变化，更新表单验证状态
watch(() => props.formData, () => {
  if (formRef.value) {
    // 延迟重置表单验证状态，确保DOM已更新
    setTimeout(() => {
      formRef.value.clearValidate()
    }, 0)
  }
}, { deep: true })

/**
 * 检查字段是否必填
 * @param {Object} field - 字段配置
 * @returns {boolean} 是否必填
 */
const isFieldRequired = (field) => {
  // 优先检查字段自身的required属性
  if (field.required !== undefined) {
    return field.required
  }
  
  // 其次检查验证规则
  if (props.rules[field.prop]) {
    return props.rules[field.prop].some(rule => rule.required)
  }
  
  return false
}

/**
 * 检查字段是否禁用
 * @param {Object} field - 字段配置
 * @returns {boolean} 是否禁用
 */
const isFieldDisabled = (field) => {
  // 检查字段自身的disabled属性或disabled函数
  if (typeof field.disabled === 'function') {
    return field.disabled(props.isEdit, props.formData)
  }
  return field.disabled || false
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    // 触发提交事件
    emit('submit', { ...props.formData })
  } catch (error) {
    // 表单验证失败，不进行任何操作
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
}

/**
 * 处理远程搜索下拉框的显示状态变化
 * 避免在组件内部直接修改props数据导致的循环引用问题
 * @param {Object} field - 字段配置对象
 * @param {boolean} visible - 是否显示下拉框
 */
const handleRemoteSelectVisibleChange = (field, visible) => {
  // 不自动触发远程搜索，避免循环引用问题
  // 让父组件自己控制远程搜索的触发时机
  // 这样可以避免当远程搜索方法更新选项时可能导致的循环引用
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
  }
})
</script>

<style scoped lang="scss">
@use './DialogForm.scss';
</style>