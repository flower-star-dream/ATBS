# 座位预订管理模块

## 概述

座位预订管理模块是火车订票系统后台管理的重要组成部分，用于管理列车班次的座位预订信息。该模块提供了座位预订的增删改查、状态管理、批量操作等功能。

## 功能特性

### 1. 座位预订列表页面 (SeatReservation-list-view.vue)

**主要功能：**
- 座位预订信息的分页展示
- 多条件搜索（班次、座位号、预订状态）
- 批量操作（批量状态更新、批量删除）
- 座位状态管理（可预订 → 已预订 → 已锁定）
- 实时座位可用性检查

**界面特性：**
- 使用 Element Plus 表格组件，支持排序和筛选
- 状态标签显示，不同状态使用不同颜色区分
- 响应式设计，适配不同屏幕尺寸
- 操作按钮权限控制

### 2. 座位预订对话框组件 (SeatReservation-dialog-view.vue)

**主要功能：**
- 班次选择下拉框，显示班次详细信息
- 座位号输入和可用性检查
- 预订状态选择和说明
- 座位状态统计信息展示
- 表单验证和错误提示

**界面特性：**
- 分组表单字段布局
- 实时座位可用性检查
- 班次详情展示
- 状态说明和提示信息

### 3. 数据类型定义 (seat-reservation.ts)

**核心类型：**
- `SeatReservation`: 座位预订基础类型
- `SeatReservationList`: 列表展示类型
- `SeatReservationForm`: 表单数据类型
- `BookingStatus`: 预订状态枚举
- `ScheduleOption`: 班次选择项类型

## 技术实现

### 组件架构

```
SeatReservation/
├── SeatReservation-list-view.vue          # 列表页面组件
├── SeatReservation-dialog-view.vue        # 对话框组件
├── SeatReservation-dialog-example.vue     # 组件演示页面
├── README.md                             # 文档说明
```

### 依赖关系

- **UI 框架**: Element Plus
- **状态管理**: Pinia (员工信息)
- **HTTP 客户端**: Axios (通过 trainSeatRequest 封装)
- **路由**: Vue Router
- **类型定义**: TypeScript

### API 接口

```typescript
// 座位预订相关 API
getSeatReservationList(params: SeatReservationQuery)     // 获取列表
getSeatReservationDetail(id: number)                    // 获取详情
createSeatReservation(data: Partial<SeatReservationForm>) // 创建
updateSeatReservation(id: number, data: Partial<SeatReservationForm>) // 更新
deleteSeatReservation(id: number)                       // 删除
batchDeleteSeatReservation(ids: number[])               // 批量删除
getScheduleOptions()                                    // 获取班次选项
updateSeatStatus(id: number, status: number)            // 更新状态
batchUpdateSeatStatus(ids: number[], status: number)    // 批量更新状态
checkSeatAvailability(scheduleId: number, seatNumber: number) // 检查可用性
```

## 使用说明

### 列表页面使用

1. **访问路径**: `/seat-reservation/list`
2. **主要操作**:
   - 搜索：通过班次、座位号、预订状态进行筛选
   - 新增：点击"新增座位预订"按钮
   - 编辑：点击行内的"编辑"按钮
   - 状态更新：点击相应的状态变更按钮
   - 批量操作：选择多行后进行批量状态更新或删除

### 对话框组件使用

```vue
<template>
  <SeatReservationDialogView
    v-model:visible="dialogVisible"
    :is-edit="isEdit"
    :seat-reservation-data="currentData"
    @submit="handleSubmit"
    @cancel="handleCancel"
  />
</template>

<script setup>
import SeatReservationDialogView from '@/views/SeatReservation/SeatReservation-dialog-view/SeatReservation-dialog-view.vue'

const dialogVisible = ref(false)
const isEdit = ref(false)
const currentData = ref({})

const handleSubmit = (formData) => {
  // 处理表单提交
}

const handleCancel = () => {
  // 处理取消操作
}
</script>
```

### 状态管理规则

座位预订状态遵循以下转换规则：

- **可预订 (0)**: 初始状态，可以转换为"已预订"或"已锁定"
- **已预订 (1)**: 可以转换回"可预订"或转为"已锁定"
- **已锁定 (2)**: 可以从任何状态转换而来，通常需要手动解锁

状态转换的业务逻辑：
```
可预订 → 已预订：乘客预订座位
可预订 → 已锁定：管理员锁定座位
已预订 → 可预订：乘客取消预订
已预订 → 已锁定：特殊情况锁定
已锁定 → 可预订：管理员解锁
```

## 数据模型

### 数据库表结构 (hcd_seat_reservation)

| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | bigint | 座位预订号（主键） |
| schedule_id | bigint | 班次号（外键） |
| seat_number | int | 座位号 |
| booking_status | int | 预订状态（0:可预订, 1:已预订, 2:已锁定） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| create_person | varchar | 创建人 |
| update_person | varchar | 更新者 |

### 前端类型定义

```typescript
interface SeatReservation {
  id: number
  scheduleId: number
  seatNumber: number
  bookingStatus: number
  createTime?: string
  updateTime?: string
  createPerson?: string
  updatePerson?: string
}
```

## 最佳实践

### 1. 组件使用
- 使用 ListPage 组件模式，保持一致的用户体验
- 合理使用插槽自定义列和操作按钮
- 遵循现有的表单验证和错误处理模式

### 2. 状态管理
- 使用枚举定义状态值，避免硬编码
- 状态转换要有明确的业务逻辑
- 提供状态转换的确认对话框

### 3. 性能优化
- 分页加载大量数据
- 搜索条件变更时重置页码
- 批量操作要有进度提示

### 4. 用户体验
- 提供清晰的状态标识和说明
- 操作按钮要有适当的禁用状态
- 错误信息要友好且具体

## 常见问题

### Q: 座位号范围是如何确定的？
A: 座位号范围基于班次的 availableTickets 字段，最大不超过 999。

### Q: 如何处理座位号冲突？
A: 系统会在保存前检查座位可用性，同一班次的相同座位号不允许重复预订。

### Q: 批量操作失败怎么办？
A: 批量操作会逐个处理，部分失败时会提示成功和失败的数量，成功的操作不会回滚。

### Q: 座位状态统计信息多久更新一次？
A: 每次选择班次时都会实时获取最新的座位状态统计信息。

## 更新日志

### v1.0.0 (2024-01-15)
- 初始版本发布
- 实现基本的座位预订管理功能
- 支持座位状态管理和批量操作
- 提供完整的类型定义和 API 接口