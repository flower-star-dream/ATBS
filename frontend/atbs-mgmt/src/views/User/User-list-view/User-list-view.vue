<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage
    title="用户列表"
    :show-tabs="true"
    :tabs="tabs"
    :active-tab="statusFilter"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="userList"
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
    <!-- 操作按钮区域 -->
    <template #actions>
      <!-- 用户列表暂不提供新增功能 -->
    </template>

    <!-- 自定义状态列 -->
    <template #column-status="{ row }">
      <el-tag :type="getStatusTagType(row.status)" size="small">
        {{ getStatusText(row.status) }}
      </el-tag>
    </template>

    <!-- 自定义创建时间列 -->
    <template #column-createTime="{ row }">
      {{ formatDate(row.createTime) }}
    </template>
    <template #column-updateTime="{ row }">
      {{ formatDate(row.updateTime) }}
    </template>

    <!-- 自定义操作列 -->
    <template #column-action="{ row }">
      <el-button type="primary" text size="small" @click="handleStatusChange(row)">{{ row.status === USER_STATUS.ENABLED ? '冻结' : '解冻' }}</el-button>
    </template>
  </ListPage>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getUserListService, updateUserStatusService } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/formatDate'

// 用户状态枚举常量
const USER_STATUS = {
  ENABLED: 1,
  DISABLED: 0,
  // 获取状态文本
  getText: (status) => {
    const statusMap = {
      [USER_STATUS.ENABLED]: '正常',
      [USER_STATUS.DISABLED]: '已冻结'
    }
    return statusMap[status] || '未知状态'
  },
  // 获取状态标签类型
  getTagType: (status) => {
    const typeMap = {
      [USER_STATUS.ENABLED]: 'success',
      [USER_STATUS.DISABLED]: 'danger'
    }
    return typeMap[status] || 'info'
  }
}

// 响应式数据
const userList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const statusFilter = ref('all')

// 状态数量统计
const statusCounts = ref({
  all: 0,
  enabled: 0,
  disabled: 0
})

// 标签页配置（计算属性）
const tabs = computed(() => [
  {
    name: 'all',
    label: '全部',
    count: statusCounts.value.all,
    lazy: true
  },
  {
    name: 'enabled',
    label: '正常',
    count: statusCounts.value.enabled,
    lazy: true
  },
  {
    name: 'disabled',
    label: '已冻结',
    count: statusCounts.value.disabled,
    lazy: true
  }
])

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '用户编号',
    width: 150,
    align: 'center'
  },
  {
    prop: 'openid',
    label: '微信用户OpenID',
    minWidth: 180,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'username',
    label: '用户名',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'email',
    label: '邮箱',
    minWidth: 180,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'phone',
    label: '手机号',
    minWidth: 120,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'passengerId',
    label: '关联乘车人ID',
    minWidth: 150,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 200,
    align: 'center'
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 200,
    align: 'center'
  },
  {
    prop: 'createPerson',
    label: '创建人',
    minWidth: 150,
    align: 'center'
  },
  {
    prop: 'updatePerson',
    label: '更新人',
    minWidth: 150,
    align: 'center'
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    align: 'center'
  },
  {
    prop: 'action',
    label: '操作',
    width: 100,
    align: 'center',
    fixed: 'right'
  }
]

// 搜索字段配置
const searchFields = [
  {
    prop: 'username',
    label: '用户名',
    type: 'input',
    placeholder: '请输入用户名',
    clearable: true
  },
  {
    prop: 'phone',
    label: '手机号',
    type: 'input',
    placeholder: '请输入手机号',
    clearable: true
  }
]

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})

/**
 * 获取用户列表
 * @param {Object} searchParams - 搜索参数
 */
const fetchUserList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    // API调用
    const response = await getUserListService(params)
    const data = response.records
    
    // 按状态筛选
    let filteredData = filterDataByStatus(data)
    userList.value = filteredData
    total.value = Number(response.total)
    
    // 更新状态数量统计
    updateStatusCounts(data)
  } catch (error) {
    userList.value = []
    total.value = 0
    
    // 重置状态统计
    statusCounts.value = {
      all: 0,
      enabled: 0,
      disabled: 0
    }
  } finally {
    loading.value = false
  }
}

/**
 * 根据状态筛选数据
 * @param {Array} data - 原始数据
 * @returns {Array} 筛选后的数据
 */
const filterDataByStatus = (data) => {
  if (statusFilter.value === 'all') {
    return data
  }
  // 根据筛选条件返回对应状态的数据
  if (statusFilter.value === 'enabled') {
    return data.filter(item => item.status === USER_STATUS.ENABLED)
  }
  if (statusFilter.value === 'disabled') {
    return data.filter(item => item.status === USER_STATUS.DISABLED)
  }
  return data
}

/**
 * 更新状态数量统计
 * @param {Array} list - 用户列表数据
 */
const updateStatusCounts = (list) => {
  const enabledCount = list.filter(item => item.status === USER_STATUS.ENABLED).length
  const disabledCount = list.filter(item => item.status === USER_STATUS.DISABLED).length
  
  statusCounts.value = {
    all: list.length,
    enabled: enabledCount,
    disabled: disabledCount
  }
}

/**
 * 获取状态文本
 * @param {number} status - 状态值
 * @returns {string} 状态文本
 */
const getStatusText = (status) => {
  return USER_STATUS.getText(status)
}

/**
 * 获取状态标签类型
 * @param {number} status - 状态值
 * @returns {string} 标签类型
 */
const getStatusTagType = (status) => {
  return USER_STATUS.getTagType(status)
}

/**
 * 处理查询操作
 * @param {Object} formData - 搜索表单数据
 */
const handleSearch = (formData) => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchUserList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  statusFilter.value = 'all' // 重置状态筛选
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchUserList({}) // 重置后自动查询
}

/**
 * 处理状态标签页切换
 * @param {string} tabName - 切换到的标签页名称
 */
const handleStatusTabChange = (tabName) => {
  statusFilter.value = tabName // 更新状态筛选
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchUserList()
}

/**
 * 处理表格选择变化
 * @param {Array} selection - 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

/**
 * 分页大小变化
 * @param {number} size - 每页条数
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchUserList()
}

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchUserList()
}

/**
 * 切换用户状态
 * @param {Object} row - 用户数据
 */
const handleStatusChange = async (row) => {
  const newStatus = row.status === USER_STATUS.ENABLED ? USER_STATUS.DISABLED : USER_STATUS.ENABLED
  const actionText = newStatus === USER_STATUS.ENABLED ? '解冻' : '冻结'
  
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}用户「${row.username}」吗？`,
      '操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const data = { id: row.id, status: newStatus }
    
    await updateUserStatusService(data)
    ElMessage.success(`${actionText}成功`)
    
    // 刷新列表
    fetchUserList()
  } catch (error) {
    // 用户取消操作或发生错误
    if (error !== 'cancel') {
      ElMessage.error(`${actionText}失败`)
    }
  }
}

// 组件挂载后加载数据
onMounted(() => {
  fetchUserList()
})
</script>

<style scoped lang="scss">
/* 使用ListPage组件的样式，无需额外样式 */
@use './User-list-view.scss';
</style>