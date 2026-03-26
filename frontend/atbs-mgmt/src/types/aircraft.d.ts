import { PageQuery } from './index'

/**
 * 飞机基础类型
 */
export interface Aircraft {
  id: number                    // 飞机号
  aircraftName: string         // 飞机名
  aircraftModel: string        // 飞机型号
  seatNum: number              // 座位数
  serviceYears: number         // 服务年数
  status: number               // 状态（0-禁用，1-启用）
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 飞机列表类型（包含分页信息）
 */
export interface AircraftList extends Aircraft {
  // 继承Aircraft的所有属性
}

/**
 * 飞机查询参数类型
 */
export interface AircraftQuery extends PageQuery {
  aircraftName?: string        // 飞机名（模糊查询）
}

/**
 * 飞机表单数据类型（用于新增/编辑）
 */
export interface AircraftForm {
  id?: number                  // 飞机号（编辑时必填）
  aircraftName: string        // 飞机名
  aircraftModel: string       // 飞机型号
  seatNum: number             // 座位数
  serviceYears: number        // 服务年数
  status?: number              // 状态
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * @deprecated 使用Aircraft替代Train
 */
export type Train = Aircraft

/**
 * @deprecated 使用AircraftQuery替代TrainQuery
 */
export type TrainQuery = AircraftQuery

/**
 * @deprecated 使用AircraftForm替代TrainForm
 */
export type TrainForm = AircraftForm
