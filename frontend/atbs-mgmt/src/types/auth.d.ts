/**
 * 客户端类型枚举
 * ADMIN_WEB = 1（管理后台Web端）
 * APPLET = 2（小程序端）
 */
export enum ClientType {
  ADMIN_WEB = 1,
  APPLET = 2
}

/**
 * OAuth2令牌响应 (RFC 6749 5.1)
 */
export interface TokenRES {
  access_token: string
  token_type: string
  expires_in: number
  refresh_token: string
  scope: string
  refresh_recommended?: boolean
}

/**
 * 登录返回参数
 */
export interface LoginTokenRES extends TokenRES {
  id: string
  username: string
}

/**
 * OAuth2Token请求参数
 */
export interface OAuth2TokenREQ {
  grantType: string
  clientId: string
  clientSecret: string
  scope: string
  extraParams?: Record<string, string>
}

/**
 * OAuth2授权请求参数
 */
export interface OAuth2AuthorizeREQ {
  responseType: string
  clientId: string
  redirectUri?: string
  scope?: string
  state?: string
  codeChallenge?: string
  codeChallengeMethod?: string
}

/**
 * 用户信息响应 (OIDC)
 */
export interface UserInfoRES {
  sub: string
  preferred_username: string
  name: string
  phone: string
  email: string
  picture: string
  updated_at: number
  user_id: string
  client_type: string
  roles: string[]
}

/**
 * 用户新增/修改请求
 */
export interface AuthUserREQ {
  id?: string
  username: string
  password?: string
  phone?: string
  email?: string
  status?: number
}

/**
 * 用户信息请求
 */
export interface UserInfoREQ {
  id?: string
  username?: string
  phone?: string
  email?: string
  nickname?: string
  avatarUrl?: string
}

/**
 * 用户分页查询请求
 */
export interface UserPageQueryREQ {
  page: number
  pageSize: number
  username?: string
  phone?: string
}

/**
 * 重置密码请求
 */
export interface ResetPwdREQ {
  id: string
  oldPwd?: string
  newPwd: string
  confirmPwd: string
}

/**
 * 登录表单数据
 */
export interface LoginForm {
  username: string
  phone: string
  password: string
}

/**
 * OAuth2授权服务器元数据
 */
export interface AuthorizationServerMetadata {
  issuer: string
  authorization_endpoint: string
  token_endpoint: string
  revocation_endpoint: string
  introspection_endpoint: string
  userinfo_endpoint: string
  jwks_uri: string
  scopes_supported: string[]
  response_types_supported: string[]
  grant_types_supported: string[]
  token_endpoint_auth_methods_supported: string[]
}

/**
 * 令牌内省响应
 */
export interface IntrospectRES {
  active: boolean
  scope?: string
  client_id?: string
  username?: string
  token_type?: string
  exp?: number
  iat?: number
  sub?: string
}
