/**
 * 列车相关API
 * @deprecated 列车服务已更名为飞机服务，请使用 aircraft.ts 中的接口
 * 保留此文件用于向后兼容
 */

export {
  getAircraftList as getTrainList,
  addAircraft as addTrain,
  updateAircraft as updateTrain,
  deleteAircraft as deleteTrain
} from './aircraft'

// 为了类型兼容，导出旧名称
export type { Aircraft as Train, AircraftQuery as TrainQuery } from '@/types'
