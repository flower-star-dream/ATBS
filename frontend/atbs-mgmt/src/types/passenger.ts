// 乘机人相关类型定义
import type { PageQuery } from './index'

/**
 * 乘机人基本信息
 */
export interface Passenger {
  id: string
  realName: string
  cardType: string
  idCard: string
  createdTime: string
  updatedTime: string
  createdPerson: string
  updatedPerson: string
}

/**
 * 乘机人分页查询参数
 */
export interface PassengerPageQueryREQ extends PageQuery {
  realName?: string
  cardType?: string
  idCard?: string
}
