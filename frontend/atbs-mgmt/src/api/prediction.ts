import { predictionRequest } from '@/utils/request'
import type {
  PredictionRequest,
  QuickForecastParams,
  PredictionResponse,
  TrainingResponse,
  ModelInfoResponse,
  HealthResponse,
  TrainingTaskCreateResponse,
  TrainingTaskResponse,
  TrainingTaskListResponse,
  GetTrainingTaskListParams,
  CreateTrainingTaskParams,
  AutoRetrainStatusResponse
} from '@/types'

/**
 * 预测服务相关 API
 * 对应后端 prediction-service 的接口定义
 * 基础路径: /api/mgmt/v1/prediction
 */

/**
 * 预测未来客流
 * POST /forecast
 * @param request 预测请求参数
 * @returns 预测响应数据
 */
export const forecast = (request: PredictionRequest): Promise<PredictionResponse> => {
  return predictionRequest.post('/forecast', request)
}

/**
 * 快速预测（默认20天）
 * GET /forecast?days=20&confidence_level=0.95&auto_train=false
 * @param params 查询参数
 * @returns 预测响应数据
 */
export const quickForecast = (params?: QuickForecastParams): Promise<PredictionResponse> => {
  return predictionRequest.get('/forecast', { params })
}

/**
 * 训练模型（同步版本，需要登录权限）
 * POST /train
 * @returns 训练响应数据
 * @deprecated 建议使用 createTrainingTask 异步训练接口
 */
export const trainModel = (): Promise<TrainingResponse> => {
  return predictionRequest.post('/train')
}

// ==================== 异步训练任务相关接口 ====================

/**
 * 创建异步训练任务（需要登录权限）
 * POST /train/async?priority=0
 * 立即返回任务ID，训练任务在后台异步执行
 * @param params 创建任务参数（可选，包含优先级）
 * @returns 任务创建响应数据
 */
export const createTrainingTask = (params?: CreateTrainingTaskParams): Promise<TrainingTaskCreateResponse> => {
  return predictionRequest.post('/train/async', null, { params })
}

/**
 * 获取训练任务状态和进度
 * GET /train/task/{task_id}
 * @param taskId 任务ID
 * @returns 任务状态响应数据
 */
export const getTrainingTaskStatus = (taskId: string): Promise<TrainingTaskResponse> => {
  return predictionRequest.get(`/train/task/${taskId}`)
}

/**
 * 获取训练任务列表
 * GET /train/tasks
 * @param params 查询参数
 * @returns 任务列表响应数据
 */
export const getTrainingTaskList = (params?: GetTrainingTaskListParams): Promise<TrainingTaskListResponse> => {
  return predictionRequest.get('/train/tasks', { params })
}

/**
 * 取消训练任务（需要登录权限）
 * POST /train/task/{task_id}/cancel
 * @param taskId 任务ID
 * @returns 取消结果
 */
export const cancelTrainingTask = (taskId: string): Promise<{ task_id: string; cancelled: boolean }> => {
  return predictionRequest.post(`/train/task/${taskId}/cancel`)
}

// ==================== 模型信息相关接口 ====================

/**
 * 获取模型信息
 * GET /model-info
 * @returns 模型信息响应数据
 */
export const getModelInfo = (): Promise<ModelInfoResponse> => {
  return predictionRequest.get('/model-info')
}

/**
 * 健康检查
 * GET /health
 * @returns 健康检查响应
 */
export const healthCheck = (): Promise<HealthResponse> => {
  return predictionRequest.get('/health')
}

/**
 * 获取自动重训服务状态
 * GET /auto-retrain/status
 * @returns 自动重训服务状态
 */
export const getAutoRetrainStatus = (): Promise<AutoRetrainStatusResponse> => {
  return predictionRequest.get('/auto-retrain/status')
}
