# 班次管理模块

本模块提供了完整的班次管理功能，包括班次列表展示、新增、编辑、删除等操作。

## 功能特性

### 1. 班次列表页面 (Schedule-list-view.vue)
- **列表展示**: 展示班次号、飞机信息、线路信息、机长、余票数、出发/到达时间等
- **搜索筛选**: 支持按飞机、线路、机长进行筛选
- **批量操作**: 支持批量删除班次
- **分页功能**: 支持分页展示和数据量控制
- **状态显示**: 余票数量使用不同颜色的标签进行状态提示

### 2. 班次表单组件
提供了两种表单实现方式：

#### 方式一：通用DialogForm组件集成
- 使用现有的`DialogForm`组件，通过自定义插槽实现时间选择器
- 代码更简洁，复用性高
- 适合快速开发和标准化场景

#### 方式二：独立ScheduleDialog组件
- 专门为班次管理定制的对话框组件
- 提供更丰富的交互功能和验证逻辑
- 包含时间验证、信息预览等高级功能
- 适合复杂业务场景

## 技术实现

### 前端技术栈
- Vue 3 + TypeScript
- Element Plus UI组件库
- Pinia状态管理
- Axios HTTP客户端

### 组件结构
```
Schedule/
├── Schedule-list-view/          # 班次列表页面
│   ├── Schedule-list-view.vue   # 主列表组件
│   └── Schedule-list-view.scss  # 样式文件
├── Schedule-dialog-view/        # 独立对话框组件
│   └── Schedule-dialog-view.vue # 专用班次表单对话框
├── Schedule-dialog-example.vue  # 使用示例
└── README.md                    # 说明文档
```

### API接口
班次管理模块提供了完整的RESTful API：

#### 班次管理接口
- `GET /schedule/list` - 分页查询班次列表
- `GET /schedule/detail/{id}` - 获取班次详情
- `POST /schedule/create` - 创建班次
- `PUT /schedule/update/{id}` - 更新班次
- `DELETE /schedule/delete/{id}` - 删除班次

#### 下拉框数据接口
- `GET /schedule/airplane-options` - 获取飞机选项列表
- `GET /schedule/route-options` - 获取线路选项列表

## 数据模型

### 班次表 (atbs_schedule)
```sql
CREATE TABLE `atbs_schedule` (
  `id` bigint NOT NULL COMMENT '班次号',
  `airplane_id` bigint NOT NULL COMMENT '飞机号',
  `route_id` bigint NOT NULL COMMENT '线路号',
  `conductor` varchar(10) DEFAULT NULL COMMENT '机长',
  `available_tickets` int NOT NULL COMMENT '余票',
  `start_time` datetime NOT NULL COMMENT '出发时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person` varchar(30) NOT NULL COMMENT '创建人',
  `update_person` varchar(30) NOT NULL COMMENT '更新者',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_班次';
```

## 使用说明

### 1. 路由配置
在路由配置文件中添加班次管理路由：
```javascript
{
  path: '/schedule',
  name: 'Schedule',
  meta: { title: '班次管理', icon: 'Calendar' },
  children: [
    {
      path: '/schedule/list',
      name: 'ScheduleList',
      component: () => import('@/views/Schedule/Schedule-list-view/Schedule-list-view.vue'),
      meta: { title: '班次列表' }
    }
  ]
}
```

### 2. 使用示例
查看`Schedule-dialog-example.vue`文件了解不同的使用方式：

```vue
<!-- 使用独立对话框 -->
<ScheduleDialog
  v-model:visible="dialogVisible"
  :title="dialogTitle"
  :form-data="scheduleForm"
  :airplane-options="airplaneOptions"
  :route-options="routeOptions"
  :is-edit="isEdit"
  @submit="handleSubmit"
/>

<!-- 使用集成对话框 -->
<DialogForm
  v-model:visible="integratedDialogVisible"
  :form-data="integratedForm"
  :fields="integratedFields"
  :rules="integratedRules"
  @submit="handleIntegratedSubmit"
>
  <template #field-startTime="{ field }">
    <el-date-picker v-model="integratedForm.startTime" type="datetime" />
  </template>
</DialogForm>
```

### 3. 主要特性

#### 表单验证
- 飞机和线路为必填项
- 机长姓名长度限制2-10个字符
- 余票数量范围0-2000
- 出发时间必须早于到达时间

#### 关联数据展示
- 自动关联显示飞机名称和型号
- 自动关联显示线路信息和起终点站
- 支持模糊搜索机长和线路信息

#### 用户体验
- 响应式布局适配不同屏幕尺寸
- 加载状态提示
- 操作确认对话框
- 友好的错误提示信息

## 开发规范

### 组件开发
1. 使用TypeScript进行类型定义
2. 遵循Vue 3 Composition API规范
3. 使用Element Plus组件库保持UI一致性
4. 添加必要的注释和文档

### API设计
1. 遵循RESTful设计原则
2. 使用统一的响应格式
3. 添加Swagger文档注释
4. 实现完整的参数验证

### 数据管理
1. 使用Pinia进行状态管理
2. 实现完整的CRUD操作
3. 添加必要的业务逻辑验证
4. 考虑性能和缓存优化

## 注意事项

1. **时间验证**: 确保到达时间晚于出发时间
2. **数据关联**: 班次关联的飞机和线路必须存在
3. **并发处理**: 考虑多用户同时操作的并发问题
4. **性能优化**: 大数据量时考虑分页和缓存策略
5. **权限控制**: 确保只有授权用户才能进行增删改操作

## 后续优化

1. 添加班次状态管理（启用/禁用）
2. 实现班期管理（工作日、周末等）
3. 添加票价管理功能
4. 实现班次的批量导入导出
5. 添加班次的统计分析功能
6. 优化大数据量的性能表现