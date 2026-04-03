/**
 * 预测服务相关类型定义
 * 对应后端 prediction-service 的接口定义
 */

/**
 * 预测请求参数
 */
export interface PredictionRequest {
  /** 预测天数 (1-365) */
  days: number
  /** 置信水平 (0-1) */
  confidence_level: number
  /** 是否自动训练（如果模型不存在） */
  auto_train: boolean
}

/**
 * 快速预测查询参数
 */
export interface QuickForecastParams {
  /** 预测天数，默认20天 */
  days?: number
  /** 置信水平，默认0.95 */
  confidence_level?: number
  /** 是否自动训练，默认false */
  auto_train?: boolean
}

/**
 * 预测响应中的模型信息
 */
export interface PredictionModelInfo {
  /** 模型阶数数组 [p, d, q] */
  order: number[]
  /** AIC值 */
  aic: number
  /** BIC值 */
  bic: number
  /** 训练时间 */
  training_time: string
}

/**
 * 预测数据点
 */
export interface PredictionPoint {
  /** 预测日期 */
  prediction_date: string
  /** 预测客流 */
  predicted_passengers: number
  /** 置信区间下限 */
  lower_bound: number
  /** 置信区间上限 */
  upper_bound: number
  /** 星期几 */
  day_of_week: string
  /** 是否工作日 */
  is_weekday: boolean
}

/**
 * 预测响应数据
 */
export interface PredictionResponse {
  /** 预测状态：success/error */
  status: string
  /** 状态消息 */
  message?: string
  /** 预测时间 */
  prediction_time: string
  /** 预测天数 */
  days: number
  /** 预测数据列表 */
  predictions: PredictionPoint[]
  /** 模型信息 */
  model_info: PredictionModelInfo
}

/**
 * 训练请求参数
 */
export interface TrainingRequest {
  /** 自回归阶数 p（仅在 auto_optimize=false 时使用） */
  p?: number
  /** 差分阶数 d（仅在 auto_optimize=false 时使用） */
  d?: number
  /** 移动平均阶数 q（仅在 auto_optimize=false 时使用） */
  q?: number
  /** 是否自动寻找最优参数（默认启用） */
  auto_optimize?: boolean
  /** 验证集大小（默认30天） */
  test_size?: number
}

/**
 * 验证指标
 */
export interface ValidationMetrics {
  /** 平均绝对误差 */
  mae: number
  /** 均方根误差 */
  rmse: number
  /** 平均绝对百分比误差 */
  mape: number
  /** 测试集大小 */
  test_size: number
}

/**
 * 训练响应数据
 */
export interface TrainingResponse {
  /** 训练状态：success/error */
  status: string
  /** 状态消息 */
  message?: string
  /** 使用的 ARIMA 参数 [p, d, q] */
  order: number[]
  /** AIC值（赤池信息准则） */
  aic?: number
  /** BIC值（贝叶斯信息准则） */
  bic?: number
  /** 训练时间 */
  training_time: string
  /** 验证指标 */
  validation?: ValidationMetrics
  /** 训练数据长度 */
  data_length: number
}

/**
 * 训练验证信息
 */
export interface TrainingValidation {
  /** 平均绝对误差 */
  mae: number
  /** 均方根误差 */
  rmse: number
  /** 平均绝对百分比误差 */
  mape: number
  /** 测试集大小 */
  test_size: number
}

/**
 * 模型参数
 */
export interface ModelParams {
  /** AR阶数 */
  p: number
  /** 差分阶数 */
  d: number
  /** MA阶数 */
  q: number
  /** 季节性AR阶数 */
  P?: number
  /** 季节性差分阶数 */
  D?: number
  /** 季节性MA阶数 */
  Q?: number
  /** 季节性周期 */
  s?: number
}

/**
 * 训练历史记录
 */
export interface TrainingHistory {
  /** 训练开始时间 */
  start_time: string
  /** 训练结束时间 */
  end_time: string
  /** AIC值 */
  aic: number
  /** BIC值 */
  bic: number
  /** 模型参数 */
  params: ModelParams
  /** 验证结果 */
  validation: TrainingValidation
  /** 数据长度 */
  data_length: number
  /** 网格搜索信息 */
  grid_search?: {
    best_score: number
    scoring_metric: string
    cv_config: {
      n_splits: number
      test_size: number
      gap: number
    }
    execution_time: number
  }
}

/**
 * 模型信息响应数据
 */
export interface ModelInfoResponse {
  /** 模型是否已加载 */
  model_loaded: boolean
  /** 预测时间 */
  prediction_time?: string
  /** 模型阶数数组 [p, d, q] */
  order?: number[]
  /** AIC值 */
  aic?: number
  /** BIC值 */
  bic?: number
  /** 噪声方差 */
  sigma2?: number
  /** 训练历史记录 */
  training_history?: TrainingHistory
}

/**
 * 健康检查响应
 */
export interface HealthResponse {
  /** 服务状态 */
  status: string
}

/**
 * 预测服务错误响应
 */
export interface PredictionError {
  /** 错误代码 */
  code: number
  /** 错误消息 */
  message: string
  /** 错误详情 */
  detail?: string
}

// ==================== 异步训练任务相关类型 ====================

/**
 * 任务进度信息
 */
export interface TaskProgressInfo {
  /** 当前阶段 */
  stage: string
  /** 完成百分比 (0-100) */
  percent: number
  /** 当前步骤描述 */
  current_step: string
  /** 总步骤数 */
  total_steps: number
  /** 已完成步骤数 */
  completed_steps: number
  /** 预计剩余时间(秒) */
  estimated_remaining_seconds?: number
  /** 进度消息 */
  message: string
  /** 更新时间 */
  updated_at: string
}

/**
 * 任务错误信息
 */
export interface TaskErrorInfo {
  /** 错误类型 */
  error_type: string
  /** 错误消息 */
  error_message: string
  /** 错误发生时间 */
  timestamp: string
}

/**
 * 训练任务响应
 */
export interface TrainingTaskResponse {
  /** 任务ID */
  task_id: string
  /** 任务状态 (pending/processing/completed/failed/cancelled) */
  status: string
  /** 任务进度信息 */
  progress: TaskProgressInfo
  /** 创建时间 */
  created_at: string
  /** 开始时间 */
  started_at?: string
  /** 完成时间 */
  completed_at?: string
  /** 训练结果（仅当status=completed时） */
  result?: TrainingResponse
  /** 错误信息（仅当status=failed时） */
  error_info?: TaskErrorInfo
}

/**
 * 训练任务创建响应
 */
export interface TrainingTaskCreateResponse {
  /** 任务ID */
  task_id: string
  /** 任务状态 */
  status: string
  /** 响应消息 */
  message: string
  /** 创建时间 */
  created_at: string
}

/**
 * 训练任务列表项
 */
export interface TrainingTaskListItem {
  /** 任务ID */
  task_id: string
  /** 任务状态 */
  status: string
  /** 进度百分比 */
  progress_percent: number
  /** 当前阶段 */
  current_stage: string
  /** 创建时间 */
  created_at: string
  /** 完成时间 */
  completed_at?: string
}

/**
 * 训练任务列表响应
 */
export interface TrainingTaskListResponse {
  /** 总任务数 */
  total: number
  /** 任务列表 */
  tasks: TrainingTaskListItem[]
}

/**
 * 获取任务列表参数
 */
export interface GetTrainingTaskListParams {
  /** 状态筛选 (pending/processing/completed/failed/cancelled/recovering) */
  status?: string
  /** 返回数量限制 */
  limit?: number
  /** 偏移量 */
  offset?: number
}

/**
 * 创建训练任务参数
 */
export interface CreateTrainingTaskParams {
  /** 任务优先级，数字越小优先级越高 (0-10) */
  priority?: number
}

/**
 * 自动重训服务状态响应
 */
export interface AutoRetrainStatusResponse {
  /** 是否启用 */
  enabled: boolean
  /** 服务是否运行中 */
  running: boolean
  /** 定时执行时间 */
  schedule_time?: string | null
  /** 重训周期(天) */
  retrain_cycle_days?: number | null
  /** 上次重训时间 */
  last_retrain_date?: string | null
  /** 重训次数 */
  retrain_count: number
  /** 数据文件路径 */
  data_file?: string | null
  /** 数据行数 */
  data_rows: number
}
