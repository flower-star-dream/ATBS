// 用户相关类型定义

/**
 * 用户基本信息
 */
export interface User {
  id: string
  openid: string
  username: string
  avatar: string
  email: string
  phone: string
  passengerId: string
  status: number
  createdTime: string
  updatedTime: string
  createdPerson: string
  updatedPerson: string
}

/**
 * 用户列表项
 */
export interface UserList extends User {}

/**
 * 用户分页查询参数
 */
export interface UserPageQueryREQ extends PageQuery {
  username?: string
  phone?: string
}