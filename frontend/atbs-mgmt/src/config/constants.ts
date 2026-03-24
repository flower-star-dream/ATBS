// 系统常量配置

// 路由白名单 - 不需要登录的路由
export const WHITE_LIST = ['/login', '/register', '/404', '/500']

// 缓存键名
export const CACHE_KEY = {
  TOKEN: 'atbs-token',
  USER_INFO: 'atbs-user-info',
  PERMISSIONS: 'atbs-permissions',
  ROLES: 'atbs-roles',
  SETTINGS: 'atbs-settings',
  TABS: 'atbs-tabs',
  LANGUAGE: 'atbs-language',
  THEME: 'atbs-theme',
}

// 分页常量
export const PAGE_CONSTANTS = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 10,
  PAGE_SIZES: [10, 20, 50, 100],
  LAYOUT: 'total, sizes, prev, pager, next, jumper',
}

// 日期格式
export const DATE_FORMAT = {
  DATE: 'YYYY-MM-DD',
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  TIME: 'HH:mm:ss',
  MONTH: 'YYYY-MM',
}

// 正则表达式
export const REGEXP = {
  EMAIL: /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/,
  PHONE: /^1[3-9]\d{9}$/,
  URL: /^https?:\/\/(([a-zA-Z0-9_-])+(\.)?)*(:(\d+))?((\/)([a-zA-Z0-9._-])*)*(\?([a-zA-Z0-9._%-]+=[a-zA-Z0-9._%-]+)(&[a-zA-Z0-9._%-]+=[a-zA-Z0-9._%-]+)*)?(#([a-zA-Z0-9._-]+))?$/,
  PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[^]{8,16}$/,
  ID_CARD: /(^[1-9]\d{5}(18|19|([23]\d))\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$)|(^[1-9]\d{5}\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\d{3}$)/,
}

// HTTP状态码
export const HTTP_STATUS = {
  SUCCESS: 200,
  CREATED: 201,
  ACCEPTED: 202,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  METHOD_NOT_ALLOWED: 405,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504,
}

// 业务状态码
export const BUSINESS_CODE = {
  SUCCESS: 0,
  ERROR: 1,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  VALIDATE_ERROR: 422,
  BUSINESS_ERROR: 500,
}

// 主题配置
export const THEME = {
  LIGHT: 'light',
  DARK: 'dark',
  AUTO: 'auto',
}

// 语言配置
export const LANGUAGE = {
  ZH_CN: 'zh-CN',
  EN_US: 'en-US',
}

// 按钮权限前缀
export const PERMISSION_PREFIX = {
  ADD: 'add',
  EDIT: 'edit',
  DELETE: 'delete',
  VIEW: 'view',
  EXPORT: 'export',
  IMPORT: 'import',
}

// 常用字典
export const DICT = {
  STATUS: [
    { label: '启用', value: 1, color: 'success' },
    { label: '禁用', value: 0, color: 'danger' },
  ],
  GENDER: [
    { label: '男', value: 1 },
    { label: '女', value: 2 },
    { label: '未知', value: 0 },
  ],
  BOOLEAN: [
    { label: '是', value: true },
    { label: '否', value: false },
  ],
}