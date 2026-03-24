import { StringDecoder } from "node:string_decoder"

// 员工登录表单数据结构
export interface LoginForm {
  username: string
  phone: string
  password: string
}

// 登录响应数据结构
export interface LoginResponse {
  token: string
  id: string
  username: string
}

// 当前员工信息数据结构
export interface EmployeeInfo {
  id: string
  username: string
  nickname: string
  phone: string
  avatar: string
  affiliatedSite: string
  permissionLevel: string
}

// 员工信息数据结构
export interface Employee {
  id: string
  username: string
  nickname: string
  password: string
  phone: string
  affiliatedSite: string
  permissionLevel: StringDecoder
  status: number
}

// 员工列表数据结构
export interface EmployeeList {
  id: string
  username: string
  nickname: string
  phone: string
  affiliatedSite: string
  permissionLevel: string
  createdTime: string
  updatedTime: string
  createdPerson: string
  updatedPerson: string
  status: number
}

// 重置员工密码表单数据结构
export interface ResetPasswordForm {
  id: string
  oldPwd: string
  newPwd: string
  confirmPwd: string
}

