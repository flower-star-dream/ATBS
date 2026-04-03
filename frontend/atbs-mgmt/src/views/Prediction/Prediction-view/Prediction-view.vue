<template>
  <div class="prediction-view">
    <el-card class="page-container" shadow="hover">
      <div class="header">
        <h2>客流预测分析</h2>
        <div class="header-actions">
          <el-button type="primary" @click="handleTrainModel" :loading="trainingLoading">
            <el-icon><Refresh /></el-icon>
            训练模型
          </el-button>
          <el-button @click="showTaskListDialog">
            <el-icon><List /></el-icon>
            训练任务
          </el-button>
          <el-button @click="showAutoRetrainDialog">
            <el-icon><Timer /></el-icon>
            自动重训
          </el-button>
        </div>
      </div>

      <!-- 模型信息卡片 -->
      <el-row :gutter="20" class="model-info-row">
        <el-col :span="24">
          <el-card shadow="never" class="model-status-card">
            <template #header>
              <div class="card-header">
                <span>模型状态</span>
                <div class="header-actions">
                  <el-button
                    type="primary"
                    link
                    :loading="refreshLoading"
                    @click="handleRefreshModelInfo"
                  >
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                  <el-tag :type="modelStatusTagType" size="small">{{ modelStatusText }}</el-tag>
                </div>
              </div>
            </template>
            <div v-if="modelInfo?.model_loaded" class="model-details">
              <el-descriptions :column="3" border>
                <!-- 模型基本信息 -->
                <el-descriptions-item label="模型阶数">{{ modelInfo.order?.join(', ') }}</el-descriptions-item>
                <el-descriptions-item label="AIC值">{{ modelInfo.aic?.toFixed(2) }}</el-descriptions-item>
                <el-descriptions-item label="BIC值">{{ modelInfo.bic?.toFixed(2) }}</el-descriptions-item>
                <el-descriptions-item label="噪声方差">{{ modelInfo.sigma2?.toFixed(4) }}</el-descriptions-item>
                <el-descriptions-item label="预测时间">{{ formatDateTime(modelInfo.prediction_time) }}</el-descriptions-item>
                <el-descriptions-item label="参数">ARIMA({{ modelInfo.training_history?.params?.p }},{{ modelInfo.training_history?.params?.d }},{{ modelInfo.training_history?.params?.q }})</el-descriptions-item>
                <!-- 训练历史信息 -->
                <el-descriptions-item label="训练开始时间">{{ formatDateTime(modelInfo.training_history?.start_time) }}</el-descriptions-item>
                <el-descriptions-item label="训练结束时间">{{ formatDateTime(modelInfo.training_history?.end_time) }}</el-descriptions-item>
                <el-descriptions-item label="数据长度">{{ modelInfo.training_history?.data_length }}</el-descriptions-item>
                <!-- 验证指标 -->
                <el-descriptions-item label="MAE">{{ modelInfo.training_history?.validation?.mae?.toFixed(4) }}</el-descriptions-item>
                <el-descriptions-item label="RMSE">{{ modelInfo.training_history?.validation?.rmse?.toFixed(4) }}</el-descriptions-item>
                <el-descriptions-item label="MAPE">{{ modelInfo.training_history?.validation?.mape?.toFixed(4) }}%</el-descriptions-item>
                <el-descriptions-item label="测试集大小">{{ modelInfo.training_history?.validation?.test_size }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <el-empty v-else description="模型尚未加载，请先训练模型" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 预测配置 -->
      <el-row :gutter="20" class="prediction-config-row">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>预测配置</span>
              </div>
            </template>
            <el-form :model="predictionForm" label-width="120px" inline>
              <el-form-item label="预测天数">
                <el-slider v-model="predictionForm.days" :min="1" :max="365" show-input style="width: 300px" />
              </el-form-item>
              <el-form-item label="置信水平">
                <el-slider v-model="predictionForm.confidence_level" :min="0.5" :max="0.99" :step="0.01" show-input style="width: 300px" />
              </el-form-item>
              <el-form-item label="自动训练">
                <el-switch v-model="predictionForm.auto_train" active-text="是" inactive-text="否" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handlePredict" :loading="predictLoading">
                  <el-icon><TrendCharts /></el-icon>
                  开始预测
                </el-button>
                <el-button @click="handleQuickPredict" :loading="predictLoading">
                  快速预测(20天)
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>

      <!-- 预测结果图表 -->
      <el-row v-if="predictionResult" :gutter="20" class="prediction-result-row">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>预测结果</span>
                <span class="result-meta">
                  预测天数: {{ predictionResult.days }}天 |
                  预测时间: {{ formatDateTime(predictionResult.prediction_time) }} |
                  模型阶数: {{ predictionResult.model_info?.order?.join(', ') }}
                </span>
              </div>
            </template>
            <div ref="chartRef" class="prediction-chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 预测数据表格 -->
      <el-row v-if="predictionResult" :gutter="20" class="prediction-table-row">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>预测数据明细</span>
              </div>
            </template>
            <el-table :data="predictionResult.predictions" stripe style="width: 100%">
              <el-table-column prop="prediction_date" label="日期" width="120" />
              <el-table-column prop="day_of_week" label="星期" width="100" />
              <el-table-column prop="predicted_passengers" label="预测客流(千人)" width="120">
                <template #default="{ row }">
                  <span class="forecast-value">{{ row.predicted_passengers.toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="lower_bound" label="置信区间下限(千人)" width="140">
                <template #default="{ row }">
                  <span class="bound-value lower">{{ row.lower_bound.toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="upper_bound" label="置信区间上限(千人)" width="140">
                <template #default="{ row }">
                  <span class="bound-value upper">{{ row.upper_bound.toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="置信区间范围" min-width="150">
                <template #default="{ row }">
                  <el-progress
                    :percentage="calculateConfidenceWidth(row)"
                    :color="confidenceProgressColor"
                    :show-text="false"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="is_weekday" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.is_weekday ? 'success' : 'info'" size="small">
                    {{ row.is_weekday ? '工作日' : '周末' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 训练任务进度对话框 -->
    <el-dialog
      v-model="trainingDialogVisible"
      title="模型训练"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="canCloseTrainingDialog"
    >
      <div v-if="currentTask" class="training-progress">
        <!-- 任务状态 -->
        <div class="task-status">
          <el-tag :type="getTaskStatusType(currentTask.status)" size="large">
            {{ getTaskStatusText(currentTask.status) }}
          </el-tag>
          <span class="task-id">任务ID: {{ currentTask.task_id }}</span>
        </div>

        <!-- 进度条 -->
        <div class="progress-section">
          <el-progress
            :percentage="currentTask.progress.percent"
            :status="getProgressStatus(currentTask.status)"
            :stroke-width="20"
            :text-inside="true"
          />
          <div class="progress-info">
            <span>阶段: {{ currentTask.progress.current_step }}</span>
            <span v-if="currentTask.progress.estimated_remaining_seconds">
              预计剩余: {{ formatDuration(currentTask.progress.estimated_remaining_seconds) }}
            </span>
          </div>
        </div>

        <!-- 详细信息 -->
        <el-descriptions :column="2" border size="small" class="progress-details">
          <el-descriptions-item label="当前阶段">{{ currentTask.progress.stage }}</el-descriptions-item>
          <el-descriptions-item label="完成步骤">{{ currentTask.progress.completed_steps }} / {{ currentTask.progress.total_steps }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentTask.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(currentTask.progress.updated_at) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 进度消息 -->
        <div v-if="currentTask.progress.message" class="progress-message">
          <el-alert :title="currentTask.progress.message" type="info" :closable="false" />
        </div>

        <!-- 训练结果 -->
        <div v-if="currentTask.status === 'completed' && currentTask.result" class="training-result">
          <el-divider>训练结果</el-divider>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="模型参数">ARIMA({{ currentTask.result.order.join(',') }})</el-descriptions-item>
            <el-descriptions-item label="AIC">{{ currentTask.result.aic?.toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="BIC">{{ currentTask.result.bic?.toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="MAPE">{{ currentTask.result.validation?.mape?.toFixed(4) }}%</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 错误信息 -->
        <div v-if="currentTask.status === 'failed' && currentTask.error_info" class="error-info">
          <el-divider>错误信息</el-divider>
          <el-alert
            :title="currentTask.error_info.error_type"
            :description="currentTask.error_info.error_message"
            type="error"
            :closable="false"
          />
        </div>
      </div>

      <template #footer>
        <el-button v-if="canCancelTask" @click="handleCancelTask" :loading="cancelLoading">取消任务</el-button>
        <el-button @click="trainingDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 训练任务列表对话框 -->
    <el-dialog
      v-model="taskListDialogVisible"
      title="训练任务列表"
      width="900px"
    >
      <el-table :data="taskList" stripe style="width: 100%" v-loading="taskListLoading">
        <el-table-column prop="task_id" label="任务ID" width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusType(row.status)" size="small">
              {{ getTaskStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress_percent" label="进度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.progress_percent" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column prop="current_stage" label="当前阶段" width="120" />
        <el-table-column prop="created_at" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column prop="completed_at" label="完成时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.completed_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewTaskDetail(row.task_id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="taskListDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="loadTaskList" :loading="taskListLoading">刷新</el-button>
      </template>
    </el-dialog>

    <!-- 自动重训状态对话框 -->
    <el-dialog
      v-model="autoRetrainDialogVisible"
      title="自动重训服务状态"
      width="600px"
    >
      <div v-loading="autoRetrainLoading">
        <div v-if="autoRetrainStatus" class="auto-retrain-status">
          <!-- 服务状态 -->
          <div class="status-header">
            <el-tag :type="autoRetrainStatus.running ? 'success' : 'info'" size="large">
              {{ autoRetrainStatus.running ? '运行中' : '未运行' }}
            </el-tag>
            <el-tag :type="autoRetrainStatus.enabled ? 'success' : 'danger'" size="large">
              {{ autoRetrainStatus.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </div>

          <!-- 统计信息 -->
          <el-descriptions :column="2" border size="small" class="status-details">
            <el-descriptions-item label="重训次数">{{ autoRetrainStatus.retrain_count }}</el-descriptions-item>
            <el-descriptions-item label="数据行数">{{ autoRetrainStatus.data_rows }}</el-descriptions-item>
            <el-descriptions-item label="定时执行">{{ autoRetrainStatus.schedule_time || '-' }}</el-descriptions-item>
            <el-descriptions-item label="重训周期">{{ autoRetrainStatus.retrain_cycle_days ? autoRetrainStatus.retrain_cycle_days + '天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="上次重训">{{ formatDateTime(autoRetrainStatus.last_retrain_date) }}</el-descriptions-item>
            <el-descriptions-item label="数据文件">{{ autoRetrainStatus.data_file || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <el-empty v-else description="无法获取自动重训状态" />
      </div>

      <template #footer>
        <el-button @click="autoRetrainDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="loadAutoRetrainStatus" :loading="autoRetrainLoading">刷新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, TrendCharts, List, Timer } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  forecast,
  quickForecast,
  trainModel,
  getModelInfo,
  createTrainingTask,
  getTrainingTaskStatus,
  getTrainingTaskList,
  cancelTrainingTask,
  getAutoRetrainStatus
} from '@/api/prediction'
import type {
  PredictionRequest,
  PredictionResponse,
  ModelInfoResponse,
  QuickForecastParams,
  TrainingTaskResponse,
  TrainingTaskListItem,
  AutoRetrainStatusResponse
} from '@/types'

// 图表DOM引用
const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 加载状态
const trainingLoading = ref(false)
const predictLoading = ref(false)
const refreshLoading = ref(false)
const cancelLoading = ref(false)
const taskListLoading = ref(false)

// 模型信息
const modelInfo = ref<ModelInfoResponse | null>(null)

// 预测表单
const predictionForm = ref<PredictionRequest>({
  days: 20,
  confidence_level: 0.95,
  auto_train: false
})

// 预测结果
const predictionResult = ref<PredictionResponse | null>(null)

// 训练任务相关
const trainingDialogVisible = ref(false)
const taskListDialogVisible = ref(false)
const currentTask = ref<TrainingTaskResponse | null>(null)
const taskList = ref<TrainingTaskListItem[]>([])
let pollingTimer: ReturnType<typeof setInterval> | null = null

// 自动重训相关
const autoRetrainDialogVisible = ref(false)
const autoRetrainStatus = ref<AutoRetrainStatusResponse | null>(null)
const autoRetrainLoading = ref(false)

// 模型状态文本
const modelStatusText = computed(() => {
  return modelInfo.value?.model_loaded ? '已加载' : '未加载'
})

// 模型状态标签类型
const modelStatusTagType = computed(() => {
  return modelInfo.value?.model_loaded ? 'success' : 'warning'
})

// 是否可以关闭训练对话框
const canCloseTrainingDialog = computed(() => {
  if (!currentTask.value) return true
  return ['completed', 'failed', 'cancelled'].includes(currentTask.value.status)
})

// 是否可以取消任务
const canCancelTask = computed(() => {
  if (!currentTask.value) return false
  return ['pending', 'processing'].includes(currentTask.value.status)
})

// 置信区间进度条颜色
const confidenceProgressColor = [
  { color: '#67C23A', percentage: 20 },
  { color: '#E6A23C', percentage: 50 },
  { color: '#F56C6C', percentage: 100 }
]

/**
 * 格式化日期时间
 * @param dateStr 日期字符串
 * @returns 格式化后的日期时间
 */
const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

/**
 * 格式化持续时间
 * @param seconds 秒数
 * @returns 格式化后的时间
 */
const formatDuration = (seconds: number) => {
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  return `${Math.floor(seconds / 3600)}小时${Math.floor((seconds % 3600) / 60)}分钟`
}

/**
 * 获取任务状态文本
 * @param status 状态码
 */
const getTaskStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

/**
 * 获取任务状态标签类型
 * @param status 状态码
 */
const getTaskStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    pending: 'info',
    processing: 'warning',
    completed: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return typeMap[status] || 'info'
}

/**
 * 获取进度条状态
 * @param status 任务状态
 */
const getProgressStatus = (status: string) => {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return ''
}

/**
 * 计算置信区间宽度百分比
 * @param row 预测数据行
 * @returns 百分比值
 */
const calculateConfidenceWidth = (row: { lower_bound: number; upper_bound: number; predicted_passengers: number }) => {
  const width = ((row.upper_bound - row.lower_bound) / row.predicted_passengers) * 100
  return Math.min(Math.max(width, 0), 100)
}

/**
 * 初始化图表
 */
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '客流预测趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['预测值', '置信区间上限', '置信区间下限'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: []
    },
    yAxis: {
      type: 'value',
      name: '客流量'
    },
    series: []
  }

  chartInstance.setOption(option)
}

/**
 * 更新图表数据
 */
const updateChart = () => {
  if (!chartInstance || !predictionResult.value) return

  const dates = predictionResult.value.predictions.map(p => p.prediction_date)
  const forecasts = predictionResult.value.predictions.map(p => p.predicted_passengers)
  const upperBounds = predictionResult.value.predictions.map(p => p.upper_bound)
  const lowerBounds = predictionResult.value.predictions.map(p => p.lower_bound)

  const option: echarts.EChartsOption = {
    xAxis: {
      data: dates
    },
    series: [
      {
        name: '预测值',
        type: 'line',
        data: forecasts,
        smooth: true,
        itemStyle: {
          color: '#409EFF'
        },
        lineStyle: {
          width: 3
        }
      },
      {
        name: '置信区间上限',
        type: 'line',
        data: upperBounds,
        smooth: true,
        lineStyle: {
          type: 'dashed',
          color: '#67C23A'
        },
        symbol: 'none'
      },
      {
        name: '置信区间下限',
        type: 'line',
        data: lowerBounds,
        smooth: true,
        lineStyle: {
          type: 'dashed',
          color: '#F56C6C'
        },
        symbol: 'none'
      }
    ]
  }

  chartInstance.setOption(option)
}

/**
 * 获取模型信息
 */
const fetchModelInfo = async () => {
  const res = await getModelInfo()
  modelInfo.value = res
}

/**
 * 处理刷新模型信息
 */
const handleRefreshModelInfo = async () => {
  refreshLoading.value = true
  try {
    await fetchModelInfo()
    ElMessage.success('模型信息已刷新')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || error.message || '刷新失败')
  } finally {
    refreshLoading.value = false
  }
}

/**
 * 轮询任务状态
 * @param taskId 任务ID
 */
const startPollingTaskStatus = (taskId: string) => {
  // 清除之前的定时器
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }

  // 立即查询一次
  pollTaskStatus(taskId)

  // 设置轮询（增加间隔到3秒，减少服务器压力）
  pollingTimer = setInterval(() => {
    pollTaskStatus(taskId)
  }, 3000) // 每3秒查询一次
}

/**
 * 查询任务状态
 * @param taskId 任务ID
 */
const pollTaskStatus = async (taskId: string) => {
  try {
    const res = await getTrainingTaskStatus(taskId)
    currentTask.value = res

    // 如果任务已完成或失败，停止轮询
    if (['completed', 'failed', 'cancelled'].includes(res.status)) {
      if (pollingTimer) {
        clearInterval(pollingTimer)
        pollingTimer = null
      }

      // 刷新模型信息
      if (res.status === 'completed') {
        await fetchModelInfo()
        ElMessage.success('模型训练成功！')
      } else if (res.status === 'failed') {
        ElMessage.error(res.error_info?.error_message || '训练失败')
      }
    }
  } catch (error: any) {
    console.error('查询任务状态失败:', error)
  }
}

/**
 * 停止轮询
 */
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

/**
 * 处理训练模型（异步版本）
 */
const handleTrainModel = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要开始训练模型吗？训练将在后台异步执行，您可以随时查看进度。',
      '训练确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    trainingLoading.value = true

    // 创建异步训练任务
    const res = await createTrainingTask()

    if (res.task_id) {
      ElMessage.success('训练任务已创建')
      trainingDialogVisible.value = true
      currentTask.value = {
        task_id: res.task_id,
        status: res.status,
        progress: {
          stage: 'initializing',
          percent: 0,
          current_step: '初始化',
          total_steps: 7,
          completed_steps: 0,
          message: '任务已创建，等待处理...',
          updated_at: res.created_at
        },
        created_at: res.created_at
      }

      // 开始轮询任务状态
      startPollingTaskStatus(res.task_id)
    } else {
      ElMessage.error('创建训练任务失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '创建训练任务失败')
    }
  } finally {
    trainingLoading.value = false
  }
}

/**
 * 处理取消任务
 */
const handleCancelTask = async () => {
  if (!currentTask.value) return

  try {
    await ElMessageBox.confirm(
      '确定要取消当前训练任务吗？',
      '取消确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    cancelLoading.value = true
    const res = await cancelTrainingTask(currentTask.value.task_id)

    if (res.cancelled) {
      ElMessage.success('任务已取消')
      stopPolling()
      // 刷新任务状态
      const taskRes = await getTrainingTaskStatus(currentTask.value.task_id)
      currentTask.value = taskRes
    } else {
      ElMessage.error('取消任务失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消任务失败')
    }
  } finally {
    cancelLoading.value = false
  }
}

/**
 * 显示任务列表对话框
 */
const showTaskListDialog = () => {
  taskListDialogVisible.value = true
  loadTaskList()
}

/**
 * 加载任务列表
 */
const loadTaskList = async () => {
  taskListLoading.value = true
  try {
    const res = await getTrainingTaskList({ limit: 20 })
    taskList.value = res.tasks
  } catch (error: any) {
    ElMessage.error(error.message || '加载任务列表失败')
  } finally {
    taskListLoading.value = false
  }
}

/**
 * 查看任务详情
 * @param taskId 任务ID
 */
const viewTaskDetail = async (taskId: string) => {
  try {
    const res = await getTrainingTaskStatus(taskId)
    currentTask.value = res
    trainingDialogVisible.value = true

    // 如果任务还在进行中，开始轮询
    if (['pending', 'processing'].includes(res.status)) {
      startPollingTaskStatus(taskId)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务详情失败')
  }
}

/**
 * 显示自动重训对话框
 */
const showAutoRetrainDialog = () => {
  autoRetrainDialogVisible.value = true
  loadAutoRetrainStatus()
}

/**
 * 加载自动重训状态
 */
const loadAutoRetrainStatus = async () => {
  autoRetrainLoading.value = true
  try {
    const res = await getAutoRetrainStatus()
    autoRetrainStatus.value = res
  } catch (error: any) {
    ElMessage.error(error.message || '加载自动重训状态失败')
  } finally {
    autoRetrainLoading.value = false
  }
}

/**
 * 处理预测
 */
const handlePredict = async () => {
  if (!modelInfo.value?.model_loaded && !predictionForm.value.auto_train) {
    ElMessage.warning('模型尚未加载，请勾选"自动训练"选项或先训练模型')
    return
  }

  try {
    predictLoading.value = true
    // 确保参数类型正确
    const requestData: PredictionRequest = {
      days: Number(predictionForm.value.days),
      confidence_level: Number(predictionForm.value.confidence_level),
      auto_train: Boolean(predictionForm.value.auto_train)
    }
    const res = await forecast(requestData)

    if (res.status === 'success') {
      predictionResult.value = res
      ElMessage.success('预测完成')

      // 等待DOM更新后初始化/更新图表
      await nextTick()
      if (!chartInstance) {
        initChart()
      }
      updateChart()

      // 如果自动训练了模型，刷新模型信息
      if (predictionForm.value.auto_train) {
        await fetchModelInfo()
      }
    } else {
      ElMessage.error(res.message || '预测失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || error.message || '预测失败')
  } finally {
    predictLoading.value = false
  }
}

/**
 * 处理快速预测
 */
const handleQuickPredict = async () => {
  if (!modelInfo.value?.model_loaded) {
    ElMessage.warning('模型尚未加载，请先训练模型')
    return
  }

  try {
    predictLoading.value = true
    // 使用当前表单中的参数进行快速预测
    const params: QuickForecastParams = {
      days: Number(20),
      confidence_level: Number(0.95),
      auto_train: Boolean(false)
    }
    const res = await quickForecast(params)

    if (res.status === 'success') {
      predictionResult.value = res
      predictionForm.value.days = res.days
      ElMessage.success('快速预测完成')

      // 等待DOM更新后初始化/更新图表
      await nextTick()
      if (!chartInstance) {
        initChart()
      }
      updateChart()
    } else {
      ElMessage.error(res.message || '预测失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || error.message || '预测失败')
  } finally {
    predictLoading.value = false
  }
}

// 组件挂载时获取模型信息
onMounted(() => {
  fetchModelInfo()

  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})

// 组件卸载时清理
onUnmounted(() => {
  stopPolling()
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>

<style scoped lang="scss">
.prediction-view {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .model-info-row,
  .prediction-config-row,
  .prediction-result-row,
  .prediction-table-row {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .result-meta {
      font-size: 14px;
      color: #909399;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .model-status-card {
    .model-details {
      padding: 10px 0;
    }
  }

  .prediction-chart {
    width: 100%;
    height: 400px;
  }

  .forecast-value {
    font-weight: 600;
    color: #409EFF;
  }

  .bound-value {
    &.lower {
      color: #F56C6C;
    }
    &.upper {
      color: #67C23A;
    }
  }
}

// 训练进度对话框样式
.training-progress {
  .task-status {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .task-id {
      font-size: 12px;
      color: #909399;
    }
  }

  .progress-section {
    margin-bottom: 20px;

    .progress-info {
      display: flex;
      justify-content: space-between;
      margin-top: 10px;
      font-size: 14px;
      color: #606266;
    }
  }

  .progress-details {
    margin-bottom: 20px;
  }

  .progress-message {
    margin-bottom: 20px;
  }

  .training-result,
  .error-info {
    margin-top: 20px;
  }
}

// 自动重训状态对话框样式
.auto-retrain-status {
  .status-header {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    justify-content: center;
  }

  .status-details {
    margin-bottom: 20px;
  }

  .config-section {
    margin-top: 20px;
  }
}
</style>
