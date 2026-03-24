// 站点相关类型定义
import type { PageQuery } from './index'

/**
 * 站点基础类型（对应hcd_station表）
 */
export interface Station {
  id: number                    // 站点号（bigint）
  stationName: string          // 站点名（varchar(50)）
  address: string              // 地址（varchar(100)）
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 站点列表类型（包含分页信息）
 */
export interface StationList extends Station {
  // 继承Station的所有属性
}

/**
 * 站点查询参数类型
 */
export interface StationQuery extends PageQuery {
  stationName?: string         // 站点名（模糊查询）
  address?: string             // 地址（模糊查询）
}

/**
 * 站点表单数据类型（用于新增/编辑）
 */
export interface StationForm {
  id?: number                  // 站点号（编辑时必填）
  stationName: string         // 站点名
  address: string             // 地址
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}

/**
 * 站点下拉选项类型
 */
export interface StationOption {
  value: number               // 站点ID
  label: string               // 站点名称
  address: string             // 站点地址
}