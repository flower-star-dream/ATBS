<template>
  <DialogForm
    v-model:visible="dialogVisible"
    :title="dialogTitle"
    :form-data="seatReservationForm"
    :fields="formFields"
    :rules="formRules"
    :is-edit="isEdit"
    :confirm-text="isEdit ? '更新' : '新增'"
    :loading="submitLoading"
    width="700px"
    @submit="handleSubmit"
    @cancel="handleCancel"
  >
    <!-- 自定义班次选择字段 -->
    <template #field-scheduleId>
      <el-form-item label="班次" prop="scheduleId" :required="true">
        <el-select
          v-model="seatReservationForm.scheduleId"
          placeholder="请选择班次"
          style="width: 100%"
          @change="handleScheduleChange"
          :disabled="isEdit"
        >
          <el-option
            v-for="option in scheduleOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <div class="schedule-option">
              <span class="schedule-label">{{ option.label }}</span>
              <span class="schedule-info">{{ option.routeInfo }} | {{ option.departureTime }}</span>
            </div>
          </el-option>
        </el-select>
        <div v-if="selectedSchedule" class="schedule-detail">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="列车">{{ selectedSchedule.airplaneName }}</el-descriptions-item>
            <el-descriptions-item label="线路">{{ selectedSchedule.routeInfo }}</el-descriptions-item>
            <el-descriptions-item label="出发时间">{{ selectedSchedule.departureTime }}</el-descriptions-item>
            <el-descriptions-item label="到达时间">{{ selectedSchedule.arrivalTime }}</el-descriptions-item>
            <el-descriptions-item label="余票数">{{ selectedSchedule.availableTickets }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-form-item>
    </template>
    
    <!-- 自定义座位号字段 -->
    <template #field-seatNumber>
      <el-form-item label="座位号" prop="seatNumber" :required="true">
        <div class="seat-number-container">
          <el-input-number
            v-model="seatReservationForm.seatNumber"
            :min="1"
            :max="maxSeatNumber"
            controls-position="right"
            placeholder="请输入座位号"
            style="width: 150px"
          />
          <el-button 
            type="info" 
            size="small" 
            @click="checkSeatAvailability"
            :disabled="!seatReservationForm.scheduleId"
            :loading="checkingAvailability"
            class="check-btn"
          >
            检查可用性
          </el-button>
          <el-tag 
            v-if="availabilityCheckResult !== null" 
            :type="availabilityCheckResult ? 'success' : 'danger'"
            size="small"
            class="availability-tag"
          >
            {{ availabilityCheckResult ? '可用' : '不可用' }}
          </el-tag>
        </div>
        <div class="seat-number-hint">
          <el-text type="info" size="small">
            座位号范围：1 - {{ maxSeatNumber }}
          </el-text>
        </div>
      </el-form-item>
    </template>
    
    <!-- 自定义预订状态字段 -->
    <template #field-bookingStatus>
      <el-form-item label="预订状态" prop="bookingStatus" :required="true">
        <el-radio-group v-model="seatReservationForm.bookingStatus" class="booking-status-group">
          <el-radio :label="0" border>
            <el-tag type="success" size="small" effect="dark">可预订</el-tag>
          </el-radio>
          <el-radio :label="1" border>
            <el-tag type="warning" size="small" effect="dark">已预订</el-tag>
          </el-radio>
          <el-radio :label="2" border>
            <el-tag type="info" size="small" effect="dark">已锁定</el-tag>
          </el-radio>
        </el-radio-group>
        <div class="status-description">
          <el-text type="info" size="small">
            {{ getStatusDescription(seatReservationForm.bookingStatus) }}
          </el-text>
        </div>
      </el-form-item>
    </template>

    <!-- 座位状态统计 -->
    <template v-if="seatStats && selectedSchedule" #extra-content>
      <el-divider />
      <div class="seat-stats">
        <h4>座位状态统计</h4>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总座位数" :value="seatStats.total" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="可预订" :value="seatStats.available">
              <template #prefix>
                <el-icon style="vertical-align: middle">
                  <CircleCheck style="color: #67C23A" />
                </el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="已预订" :value="seatStats.reserved">
              <template #prefix>
                <el-icon style="vertical-align: middle">
                  <Warning style="color: #E6A23C" />
                </el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="已锁定" :value="seatStats.locked">
              <template #prefix>
                <el-icon style="vertical-align: middle">
                  <Lock style="color: #909399" />
                </el-icon>
              </template>
            </el-statistic>
          </el-col>
        </el-row>
      </div>
    </template>
  </DialogForm>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import {
  getScheduleOptions, 
  getSeatStatusStats, 
  addSeatReservation, 
  updateSeatReservation,
  checkSeatAvailability as apiCheckSeatAvailability 
} from '@/api/seat-reservation'
import { ElMessage } from 'element-plus'
import { CircleCheck, Warning, Lock } from '@element-plus/icons-vue'
import DialogForm from '@/components/DialogForm/DialogForm.vue'
import { 
  BOOKING_STATUS_LABELS, 
  BOOKING_STATUS_TYPES,
  BookingStatus 
} from '@/types'
import { formatDate } from '@/utils/formatDate'

// Props定义
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  seatReservationData: {
    type: Object,
    default: () => ({})
  }
})

// Emits定义
const emit = defineEmits([
  'update:visible',
  'submit',
  'cancel'
])

// 响应式数据
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const submitLoading = ref(false)
const checkingAvailability = ref(false)
const availabilityCheckResult = ref(null)
const scheduleOptions = ref([])
const seatStats = ref(null)

// 表单数据
const seatReservationForm = ref({
  id: '',
  scheduleId: '',
  seatNumber: 1,
  bookingStatus: BookingStatus.AVAILABLE
})

// 计算属性
const dialogTitle = computed(() => props.isEdit ? '编辑座位预订' : '新增座位预订')

const selectedSchedule = computed(() => {
  if (!seatReservationForm.value.scheduleId) return null
  return scheduleOptions.value.find(option => option.value === seatReservationForm.value.scheduleId)
})

const maxSeatNumber = computed(() => {
  return selectedSchedule.value?.availableTickets || 999
})

// 表单字段配置
const formFields = [
  {
    prop: 'scheduleId',
    label: '班次',
    type: 'custom',
    placeholder: '请选择班次',
    required: true,
    clearable: true
  },
  {
    prop: 'seatNumber',
    label: '座位号',
    type: 'custom',
    placeholder: '请输入座位号',
    required: true
  },
  {
    prop: 'bookingStatus',
    label: '预订状态',
    type: 'custom',
    placeholder: '请选择预订状态',
    required: true
  }
]

// 表单验证规则
const formRules = computed(() => ({
  scheduleId: [
    { required: true, message: '请选择班次', trigger: 'change' }
  ],
  seatNumber: [
    { required: true, message: '请输入座位号', trigger: 'blur' },
    { type: 'number', min: 1, max: maxSeatNumber.value, message: `座位号必须在 1 到 ${maxSeatNumber.value} 之间`, trigger: 'blur' }
  ],
  bookingStatus: [
    { required: true, message: '请选择预订状态', trigger: 'change' }
  ]
}))

/**
 * 监听班次选择变化，更新座位统计信息
 */
watch(() => seatReservationForm.value.scheduleId, async (newScheduleId) => {
  if (newScheduleId) {
    await fetchSeatStatusStats(newScheduleId)
  } else {
    seatStats.value = null
  }
})

/**
 * 监听可见性变化，初始化数据
 */
watch(() => props.visible, (visible) => {
  if (visible) {
    initData()
  }
})

/**
 * 获取班次选项列表
 */
const fetchScheduleOptions = async () => {
  try {
    const options = await getScheduleOptions()
    scheduleOptions.value = options.map(item => ({
      value: item.value,
      label: item.label,
      airplaneName: item.airplaneName,
      routeInfo: item.routeInfo,
      departureTime: item.departureTime,
      arrivalTime: item.arrivalTime,
      availableTickets: item.availableTickets
    }))
  } catch (error) {
    console.error('获取班次选项失败:', error)
    ElMessage.error('获取班次选项失败')
  }
}

/**
 * 获取座位状态统计
 */
const fetchSeatStatusStats = async (scheduleId) => {
  if (!scheduleId) return
  
  try {
    const stats = await getSeatStatusStats(scheduleId)
    seatStats.value = stats
  } catch (error) {
    console.error('获取座位状态统计失败:', error)
    seatStats.value = null
  }
}

/**
 * 检查座位可用性
 */
const checkSeatAvailability = async () => {
  if (!seatReservationForm.value.scheduleId || !seatReservationForm.value.seatNumber) {
    ElMessage.warning('请先选择班次和座位号')
    return
  }
  
  checkingAvailability.value = true
  try {
    const isAvailable = await apiCheckSeatAvailability(
      seatReservationForm.value.scheduleId,
      seatReservationForm.value.seatNumber,
      props.isEdit ? seatReservationForm.value.id : undefined
    )
    
    availabilityCheckResult.value = isAvailable
    if (isAvailable) {
      ElMessage.success('该座位可用')
    } else {
      ElMessage.warning('该座位已被预订或锁定')
    }
  } catch (error) {
    ElMessage.error('检查座位可用性失败')
    availabilityCheckResult.value = null
  } finally {
    checkingAvailability.value = false
  }
}

/**
 * 获取状态描述
 */
const getStatusDescription = (status) => {
  const descriptions = {
    [BookingStatus.AVAILABLE]: '座位开放预订，乘客可以预订此座位',
    [BookingStatus.RESERVED]: '座位已被乘客预订，不可重复预订',
    [BookingStatus.LOCKED]: '座位已被锁定，暂时不可预订'
  }
  return descriptions[status] || '未知状态'
}

/**
 * 处理班次变化
 */
const handleScheduleChange = () => {
  // 清空座位号，重新选择
  seatReservationForm.value.seatNumber = 1
  availabilityCheckResult.value = null
}

/**
 * 初始化数据
 */
const initData = () => {
  if (props.isEdit && props.seatReservationData) {
    // 编辑模式，填充表单数据
    seatReservationForm.value = {
      id: props.seatReservationData.id,
      scheduleId: props.seatReservationData.scheduleId,
      seatNumber: props.seatReservationData.seatNumber,
      bookingStatus: props.seatReservationData.bookingStatus
    }
  } else {
    // 新增模式，重置表单
    seatReservationForm.value = {
      id: '',
      scheduleId: '',
      seatNumber: 1,
      bookingStatus: BookingStatus.AVAILABLE
    }
  }
  availabilityCheckResult.value = null
}

/**
 * 处理表单提交
 */
const handleSubmit = async (formData) => {
    try {
      submitLoading.value = true
      
      if (props.isEdit) {
        // 编辑座位预订
        await updateSeatReservation(formData)
        ElMessage.success('更新座位预订成功')
      } else {
        // 新增座位预订
        await addSeatReservation(formData)
        ElMessage.success('新增座位预订成功')
      }
      
      // 关闭弹窗并触发提交事件
      dialogVisible.value = false
      emit('submit', formData)
    } catch (error) {
      ElMessage.error(props.isEdit ? '更新失败' : '新增失败')
    } finally {
      submitLoading.value = false
    }
  }

/**
 * 处理取消操作
 */
const handleCancel = () => {
  dialogVisible.value = false
  emit('cancel')
}

// 组件挂载后加载数据
onMounted(() => {
  fetchScheduleOptions()
})
</script>

<style scoped lang="scss">
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.schedule-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .schedule-label {
    font-weight: 500;
  }
  
  .schedule-info {
    font-size: 12px;
    color: #909399;
  }
}

.schedule-detail {
  margin-top: 10px;
  
  :deep(.el-descriptions) {
    margin-top: 10px;
  }
}

.seat-number-container {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .check-btn {
    margin-left: 10px;
  }
  
  .availability-tag {
    margin-left: 10px;
  }
}

.seat-number-hint {
  margin-top: 8px;
}

.booking-status-group {
  display: flex;
  gap: 15px;
  
  .el-radio {
    margin-right: 0;
  }
}

.status-description {
  margin-top: 10px;
  padding: 8px 12px;
  background-color: #f4f4f5;
  border-radius: 4px;
  border-left: 4px solid #909399;
}

.seat-stats {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 6px;
  
  h4 {
    margin: 0 0 15px 0;
    color: #303133;
    font-size: 14px;
    font-weight: 600;
  }
  
  :deep(.el-statistic) {
    .el-statistic__head {
      margin-bottom: 8px;
    }
    
    .el-statistic__content {
      font-size: 20px;
      font-weight: 600;
    }
  }
}
</style>