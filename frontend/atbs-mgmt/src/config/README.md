<<<<<<< HEAD
# 配置使用指南

## 目录结构

```
src/config/
├── index.ts          # 主配置文件，根据环境导出配置
├── types.ts          # 类型定义
├── constants.ts      # 常量配置
├── env.ts            # 环境工具函数
└── README.md         # 使用文档
```

## 使用方法

### 1. 导入配置

```typescript
import { config } from '@/config'
import { CACHE_KEY, HTTP_STATUS } from '@/config/constants'
import { isDev, isProd } from '@/config/env'
```

### 2. 使用配置

```typescript
// 使用基础配置
console.log(config.baseUrl)        // API基础地址
console.log(config.ossUrl)         // OSS地址
console.log(config.timeout)        // 请求超时时间

// 使用常量
console.log(CACHE_KEY.TOKEN)       // token缓存键名
console.log(HTTP_STATUS.SUCCESS)   // HTTP状态码

// 判断环境
if (isDev()) {
  console.log('开发环境')
}
```

### 3. 环境变量配置

#### 开发环境 (.env.development)
```bash
VITE_BASE_URL=http://localhost:8080
VITE_OSS_URL=https://dev-oss.example.com
VITE_MOCK=true
VITE_DEBUG=true
```

#### 生产环境 (.env.production)
```bash
VITE_BASE_URL=https://api.example.com
VITE_OSS_URL=https://oss.example.com
VITE_MOCK=false
VITE_DEBUG=false
```

#### 测试环境 (.env.staging)
```bash
VITE_BASE_URL=https://staging-api.example.com
VITE_OSS_URL=https://staging-oss.example.com
VITE_MOCK=false
VITE_DEBUG=true
```

## 配置项说明

### 基础配置 (config)
- `baseUrl`: API基础地址
- `ossUrl`: 对象存储地址
- `apiPrefix`: API前缀
- `timeout`: 请求超时时间(毫秒)
- `mock`: 是否启用mock数据
- `debug`: 是否启用调试模式
- `title`: 页面标题

### 常量配置 (constants)
- `CACHE_KEY`: 缓存键名
- `HTTP_STATUS`: HTTP状态码
- `PAGE_CONSTANTS`: 分页常量
- `DATE_FORMAT`: 日期格式
- `REGEXP`: 正则表达式
- `THEME`: 主题配置
- `LANGUAGE`: 语言配置

### 类型定义 (types)
- `Config`: 配置类型
- `ApiResponse`: API响应类型
- `PageParams`: 分页参数类型
- `PageResponse`: 分页响应类型
- `UserInfo`: 用户信息类型
- `TableColumn`: 表格列配置类型

## 示例代码

### 配置axios实例
```typescript
import axios from 'axios'
import { config } from '@/config'
import { CACHE_KEY } from '@/config/constants'

const service = axios.create({
  baseURL: config.baseUrl + config.apiPrefix,
  timeout: config.timeout,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(CACHE_KEY.TOKEN)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)
```

### 使用常量
```typescript
import { PAGE_CONSTANTS, DATE_FORMAT } from '@/config/constants'

// 分页配置
const pagination = {
  page: PAGE_CONSTANTS.DEFAULT_PAGE,
  size: PAGE_CONSTANTS.DEFAULT_SIZE,
  sizes: PAGE_CONSTANTS.PAGE_SIZES,
}

// 日期格式化
const format = DATE_FORMAT.DATETIME
```

### 环境判断
```typescript
import { isDev, isProd } from '@/config/env'

if (isDev()) {
  console.log('开发环境日志')
}

if (isProd()) {
  // 生产环境特有逻辑
}
```

## 扩展配置

### 添加新的环境变量

1. 在 `index.ts` 中添加新的配置项：
```typescript
// 在Config接口中添加
interface Config {
  newKey: string
}

// 在各个环境中配置值
const configs = {
  development: {
    newKey: 'dev-value',
  },
  production: {
    newKey: 'prod-value',
  },
}
```

2. 在环境变量文件(.env.*)中添加：
```bash
VITE_NEW_KEY=value
```

### 添加新的常量

1. 在 `constants.ts` 中添加：
```typescript
export const NEW_CONSTANT = {
  KEY1: 'value1',
  KEY2: 'value2',
}
```

2. 在项目中使用：
```typescript
import { NEW_CONSTANT } from '@/config/constants'
```

## 最佳实践

1. **环境变量命名规范**: 使用 `VITE_` 前缀，如 `VITE_API_URL`
2. **常量命名规范**: 使用全大写和下划线，如 `CACHE_KEY`
3. **类型定义**: 使用 PascalCase 命名，如 `UserInfo`
4. **环境判断**: 使用工具函数，如 `isDev()`, `isProd()`
5. **配置分离**: 将不同环境的配置分离到不同的文件中

## 注意事项

1. 环境变量文件(.env.*)不应该提交到版本控制
2. 敏感信息(如密码、密钥)应该使用环境变量
3. 生产环境配置应该经过严格测试
4. 配置变更时应该同步更新类型定义
=======
# 配置管理说明

## 概述

本项目采用了集中式的配置管理方案，将所有环境的配置集中在 `src/config/index.ts` 文件中，使配置修改更加简单高效，无需修改多个 `.env` 文件。

## 配置管理方式

所有配置都集中在 `src/config/index.ts` 文件中，采用了以下结构：

1. **默认配置** - 定义所有环境共享的基础配置
2. **环境特定配置** - 定义各环境特有的配置，用于覆盖默认配置

## 如何修改配置

现在你只需要修改 `src/config/index.ts` 文件即可，无需修改任何 `.env` 文件。

### 示例：修改所有环境的 API 前缀

1. 打开 `src/config/index.ts` 文件
2. 修改 `defaultConfig` 对象中的 `apiPrefix` 属性：
   ```typescript
   const defaultConfig: Config = {
     // ... 其他配置 ...
     apiPrefix: '/api/v2', // 修改为新的前缀
     // ... 其他配置 ...
   }
   ```

### 示例：修改测试环境的超时时间

1. 打开 `src/config/index.ts` 文件
2. 修改 `envConfigs.staging` 对象中的 `timeout` 属性：
   ```typescript
   const envConfigs: Record<string, Partial<Config>> = {
     // ... 其他环境配置 ...
     staging: {
       // ... 其他配置 ...
       timeout: 15000, // 修改为新的超时时间
       // ... 其他配置 ...
     },
     // ... 其他环境配置 ...
   }
   ```

## 可用的配置项

### API 配置
- `baseUrl` - API 基础地址
- `apiPrefix` - API 路径前缀
- `timeout` - 请求超时时间（毫秒）

### OSS 配置
- `ossUrl` - 文件存储服务地址

### 功能开关
- `mock` - 是否启用 Mock 数据（true/false）
- `debug` - 是否启用调试模式（true/false）

### 其他配置
- `title` - 应用标题

## 添加新的配置项

如果需要添加新的配置项，请按照以下步骤操作：

1. 在 `src/config/types.ts` 的 `Config` 接口中添加对应的类型定义
2. 在 `src/config/index.ts` 的 `defaultConfig` 对象中添加默认值
3. 在需要覆盖的环境中，在 `envConfigs` 对应的环境对象中添加特定值

## 环境判断

系统提供了三个辅助函数来判断当前环境：

- `isDev` - 是否为开发环境
- `isStaging` - 是否为测试环境
- `isProd` - 是否为生产环境

## 构建和运行时环境

- 开发环境：`npm run dev`
- 测试环境：`npm run build:staging`
- 生产环境：`npm run build:prod`

系统会根据构建命令自动选择对应的环境配置。
>>>>>>> 7194a667e73e05f6f820be501adf75d935dc6a3c
