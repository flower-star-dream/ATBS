import { trainSeatRequest } from '@/utils/request'
import type { Train, TrainQuery, ApiResponse, PageQuery, PageResult } from '@/types'

/**
 * 获取列车列表
 * @param params 分页查询参数
 * @returns 列车分页列表
 */
export const getTrainList = (params: TrainQuery): Promise<PageResult<Train>> => {
  return trainSeatRequest.get('/train/getTrain', { params })
}

/**
 * 添加列车
 * @param data 列车信息
 * @returns 添加响应
 */
export const addTrain = (data: Partial<Train>): Promise<void> => {
  return trainSeatRequest.post('/train/addTrain', data)
}

/**
 * 更新列车
 * @param data 列车信息
 * @returns 更新响应
 */
export const updateTrain = (data: Partial<Train>): Promise<void> => {
  return trainSeatRequest.put('/train/updateTrain', data)
}

/**
 * 删除列车
 * @param ids 列车ID列表
 * @returns 删除响应
 */
export const deleteTrain = (ids: number[]): Promise<void> => {
  return trainSeatRequest.delete('/train/deleteTrain', { data: ids })
}