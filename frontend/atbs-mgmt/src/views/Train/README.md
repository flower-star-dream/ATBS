# 列车管理组件

本目录包含列车管理相关的Vue组件，基于hcd_train数据库表结构设计。

## 数据库表结构

```sql
hcd_train表：
- id: 列车号（bigint）
- train_name: 列车名（varchar(10)）
- train_model: 列车型号（varchar(50)）
- seat_num: 座位数（int）
- service_years: 服务年数（int）
- create_time: 创建时间
- update_time: 更新时间
- create_person: 创建人
- update_person: 更新者
```

## 组件说明

### 1. TrainList.vue (Train-list-view)
列车列表页面组件，使用ListPage通用组件实现。

**功能特性：**
- ✅ 列车列表展示（支持分页）
- ✅ 状态标签页筛选（全部/已启用/已禁用）
- ✅ 搜索功能（按列车名、列车型号搜索）
- ✅ 批量操作（批量删除）
- ✅ 单条记录操作（编辑、启用/禁用、删除）
- ✅ 创建人、更新者信息展示

**使用示例：**
```vue
<ListPage
  title="列车列表"
  :show-tabs="true"
  :tabs="tabs"
  :active-tab="statusFilter"
  :total="total"
  :current-page="currentPage"
  :page-size="pageSize"
  :selected-count="selectedRows.length"
  :loading="loading"
  :show-selection="true"
  :table-data="trainList"
  :table-columns="tableColumns"
  :search-fields="searchFields"
  :initial-search-form="initialSearchForm"
  @tab-click="handleStatusTabChange"
  @size-change="handleSizeChange"
  @current-change="handlePageChange"
  @search="handleSearch"
  @reset="handleReset"
  @selection-change="handleSelectionChange"
>
  <!-- 操作按钮 -->
  <template #actions>
    <el-button type="primary" @click="handleAdd">新增列车</el-button>
    <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
      批量删除
    </el-button>
  </template>

  <!-- 自定义列模板 -->
  <template #column-status="{ row }">
    <el-tag :type="getStatusTagType(row.status)" size="small">
      {{ getStatusText(row.status) }}
    </el-tag>
  </template>
</ListPage>
```

### 2. 表单弹窗实现
推荐使用通用的DialogForm组件，无需单独创建TrainDialog.vue。

**使用示例：**
```vue
<DialogForm
  v-model:visible="dialogVisible"
  title="列车管理"
  :form-data="trainForm"
  :fields="formFields"
  :rules="formRules"
  :is-edit="isEdit"
  :confirm-text="isEdit ? '更新' : '新增'"
  :loading="submitLoading"
  @submit="handleFormSubmit"
/>
```

**表单字段配置：**
```javascript
const formFields = [
  {
    prop: 'trainName',
    label: '列车名',
    type: 'input',
    placeholder: '请输入列车名',
    maxlength: 10,
    showWordLimit: true,
    disabled: (isEditMode) => isEditMode // 编辑时禁用
  },
  {
    prop: 'trainModel',
    label: '列车型号',
    type: 'input',
    placeholder: '请输入列车型号',
    maxlength: 50,
    showWordLimit: true
  },
  {
    prop: 'seatNum',
    label: '座位数',
    type: 'input',
    placeholder: '请输入座位数',
    inputType: 'number'
  },
  {
    prop: 'serviceYears',
    label: '服务年数',
    type: 'input',
    placeholder: '请输入服务年数',
    inputType: 'number'
  },
  {
    prop: 'status',
    label: '状态',
    type: 'switch',
    activeValue: 1,
    inactiveValue: 0,
    activeText: '启用',
    inactiveText: '禁用',
    hidden: (isEditMode) => !isEditMode // 只在编辑时显示
  }
]
```

## API接口

### 列车管理API
```typescript
// 获取列车列表
getTrainList(params: PageQuery): Promise<PageResult<Train>>

// 获取列车详情
getTrainDetail(id: number): Promise<Train>

// 创建列车
createTrain(data: Partial<Train>): Promise<ApiResponse>

// 更新列车
updateTrain(id: number, data: Partial<Train>): Promise<ApiResponse>

// 删除列车
deleteTrain(id: number): Promise<ApiResponse>

// 更新列车状态
updateTrainStatus(data: { id: number; status: number }): Promise<ApiResponse>
```

## 类型定义

### Train接口
```typescript
interface Train {
  id: number                    // 列车号
  trainName: string            // 列车名
  trainModel: string           // 列车型号
  seatNum: number              // 座位数
  serviceYears: number         // 服务年数
  status: number               // 状态
  createTime?: string          // 创建时间
  updateTime?: string          // 更新时间
  createPerson?: string        // 创建人
  updatePerson?: string        // 更新者
}
```

## 使用步骤

### 1. 引入组件
```javascript
import { ListPage, DialogForm } from '@/components'
import { getTrainList, createTrain, updateTrain, deleteTrain, updateTrainStatus } from '@/api/train'
```

### 2. 基础数据配置
```javascript
// 列车状态枚举
const TRAIN_STATUS = {
  ENABLED: 1,
  DISABLED: 0,
  getText: (status) => {
    const statusMap = {
      [TRAIN_STATUS.ENABLED]: '已启用',
      [TRAIN_STATUS.DISABLED]: '已禁用'
    }
    return statusMap[status] || '未知状态'
  },
  getTagType: (status) => {
    const typeMap = {
      [TRAIN_STATUS.ENABLED]: 'success',
      [TRAIN_STATUS.DISABLED]: 'danger'
    }
    return typeMap[status] || 'info'
  }
}
```

### 3. 表格列配置
```javascript
const tableColumns = [
  { prop: 'id', label: '列车号', width: 120, align: 'center' },
  { prop: 'trainName', label: '列车名', minWidth: 120, align: 'center' },
  { prop: 'trainModel', label: '列车型号', minWidth: 150, align: 'center' },
  { prop: 'seatNum', label: '座位数', width: 100, align: 'center' },
  { prop: 'serviceYears', label: '服务年数', width: 100, align: 'center' },
  { prop: 'createTime', label: '创建时间', minWidth: 180, align: 'center' },
  { prop: 'updateTime', label: '更新时间', minWidth: 180, align: 'center' },
  { prop: 'createPerson', label: '创建人', minWidth: 120, align: 'center' },
  { prop: 'updatePerson', label: '更新者', minWidth: 120, align: 'center' },
  { prop: 'status', label: '状态', width: 100, align: 'center' },
  { prop: 'action', label: '操作', width: 200, align: 'center', fixed: 'right' }
]
```

### 4. 搜索字段配置
```javascript
const searchFields = [
  {
    prop: 'trainName',
    label: '列车名',
    type: 'input',
    placeholder: '请输入列车名',
    clearable: true
  },
  {
    prop: 'trainModel',
    label: '列车型号',
    type: 'input',
    placeholder: '请输入列车型号',
    clearable: true
  }
]
```

### 5. 实现业务逻辑
```javascript
// 获取列车列表
const fetchTrainList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    const response = await getTrainList(params)
    const data = response.records
    
    // 数据处理和分页逻辑
    trainList.value = data
    total.value = response.total
    
    // 更新状态统计
    updateStatusCounts(data)
  } catch (error) {
    // 错误处理
  } finally {
    loading.value = false
  }
}
```

## 样式定制

### 自定义列样式
```vue
<template #column-status="{ row }">
  <el-tag :type="getStatusTagType(row.status)" size="small" effect="plain">
    {{ getStatusText(row.status) }}
  </el-tag>
</template>

<template #column-createTime="{ row }">
  <span class="time-text">{{ formatDate(row.createTime) }}</span>
</template>
```

### 操作按钮样式
```vue
<template #column-action="{ row }">
  <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
  <el-button 
    type="primary" 
    text 
    size="small" 
    @click="handleStatusChange(row)"
  >
    {{ row.status === TRAIN_STATUS.ENABLED ? '禁用' : '启用' }}
  </el-button>
  <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
</template>
```

## 最佳实践

### 1. 数据验证
- 使用表单验证规则确保数据完整性
- 后端API调用前进行数据格式化
- 处理可能的异常情况

### 2. 用户体验
- 操作确认对话框提升安全性
- 加载状态提示提升响应性
- 成功/失败消息反馈

### 3. 权限控制
- 根据用户权限动态显示操作按钮
- 敏感操作需要权限验证
- 记录操作日志（创建人、更新者）

### 4. 性能优化
- 合理使用分页减少数据传输
- 搜索防抖避免频繁请求
- 缓存常用数据提升响应速度

## 注意事项

1. **字段长度限制**：
   - 列车名：最多10个字符
   - 列车型号：最多50个字符
   - 座位数：1-2000之间
   - 服务年数：0-50年之间

2. **状态管理**：
   - 新增列车默认状态为"启用"
   - 状态切换需要确认对话框
   - 禁用状态的列车在业务中如何处理

3. **数据安全**：
   - 删除操作需要二次确认
   - 批量操作需要选择验证
   - 敏感信息需要脱敏处理

## 扩展功能建议

1. **高级搜索**：按创建时间、服务年数范围搜索
2. **数据导出**：支持Excel导出功能
3. **批量导入**：支持Excel批量导入列车数据
4. **图表统计**：列车状态分布、服务年限统计等
5. **关联信息**：显示列车关联的座位类型、票价信息等