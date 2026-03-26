// 班次相关类型定义
import type { PageQuery } from './index'

/**
 * 班次基础类型（对应hcd_schedule表）
 */
export interface Schedule {
  id: number                    // 班次号（bigint）
  trainId: number              // 列车号（bigint）- 外键关联hcd_train
  routeId: number              // 线路号（bigint）- 外键关联hcd_route
  conductor: string            // 列车长（varchar(10)）
  availableTickets: number     // 余票（int）
  startTime: string            // 出发时间（datetime）
  endTime: string              // 结束时间（datetime）
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 班次列表类型（包含分页信息）
 */
export interface ScheduleList extends Schedule {
  // 继承Schedule的所有属性
  trainName?: string           // 列车名称
  routeName?: string           // 线路名称
  routeInfo?: string           // 线路信息（出发站-到达站）
}

/**
 * 班次查询参数类型
 */
export interface ScheduleQuery extends PageQuery {
  trainId?: number            // 列车ID
  routeId?: number            // 线路ID
  conductor?: string          // 列车长（模糊查询）
  startTimeStart?: string     // 出发时间开始
  startTimeEnd?: string       // 出发时间结束
  availableTicketsMin?: number // 最小余票数
  availableTicketsMax?: number // 最大余票数
}

export interface RealTimeSchedulePageQueryREQ extends PageQuery {
  nowTime?: string
  startStationId?: string
  endStationId?: string
}

export interface RealTimeScheduleRES extends Schedule {

}

/**
 * 班次表单数据类型（用于新增/编辑）
 */
export interface ScheduleForm {
  id?: number                  // 班次号（编辑时必填）
  trainId: number             // 列车号
  routeId: number             // 线路号
  conductor: string           // 列车长
  availableTickets: number    // 余票
  startTime: string           // 出发时间
  endTime: string             // 结束时间
  createPerson?: string       // 创建人
  updatePerson?: string       // 更新者
}

/**
 * 列车选择项类型（用于下拉框）
 */
export interface TrainOption {
  value: number               // 列车ID
  label: string               // 列车名称
  trainModel?: string         // 列车型号
  seatNum?: number            // 座位数
}

/**
 * 线路选择项类型（用于下拉框）
 */
export interface RouteOption {
  value: number               // 线路ID
  label: string               // 线路名称
  departureStation?: string   // 出发站
  arrivalStation?: string     // 到达站
  distance?: number           // 距离
}
