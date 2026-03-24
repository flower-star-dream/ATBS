// 列车相关类型定义
import { PageQuery } from './index'
/**
 * 列车基础类型（对应hcd_train表）
 */
export interface Train {
  id: number                    // 列车号（bigint）
  trainName: string            // 列车名（varchar(10)）
  trainModel: string           // 列车型号（varchar(50)）
  seatNum: number              // 座位数（int）
  serviceYears: number         // 服务年数（int）
  status: number               // 状态（0-禁用，1-启用）
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 列车列表类型（包含分页信息）
 */
export interface TrainList extends Train {
  // 继承Train的所有属性
}

/**
 * 列车查询参数类型
 */
export interface TrainQuery extends PageQuery {
  trainName?: string           // 列车名（模糊查询）
}

/**
 * 列车表单数据类型（用于新增/编辑）
 */
export interface TrainForm {
  id?: number                  // 列车号（编辑时必填）
  trainName: string           // 列车名
  trainModel: string          // 列车型号
  seatNum: number             // 座位数
  serviceYears: number        // 服务年数
  status?: number              // 状态
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}