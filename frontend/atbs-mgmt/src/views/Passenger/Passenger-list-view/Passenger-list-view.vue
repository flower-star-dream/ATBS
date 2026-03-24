<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage
    title="乘车人列表"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="passengerList"
    :table-columns="tableColumns"
    :search-fields="searchFields"
    :initial-search-form="initialSearchForm"
    @size-change="handleSizeChange"
    @current-change="handlePageChange"
    @search="handleSearch"
    @reset="handleReset"
    @selection-change="handleSelectionChange"
  >
    <!-- 操作按钮区域 -->
    <template #actions>
      <!-- 乘车人不提供操作按钮 -->
    </template>

    <!-- 自定义创建时间列 -->
    <template #column-createTime="{ row }">
      {{ formatDate(row.createTime) }}
    </template>
    <template #column-updateTime="{ row }">
      {{ formatDate(row.updateTime) }}
    </template>

    <!-- 自定义证件类型列 -->
    <template #column-cardType="{ row }">
      {{ getCardTypeText(row.cardType) }}
    </template>

    <!-- 自定义操作列 -->
    <template #column-action="{ row }">
      <el-button type="primary" text size="small" @click="handleViewDetail(row)">查看详情</el-button>
    </template>
  </ListPage>

  <!-- 使用通用表单弹窗组件 -->
  <DialogForm
    v-model:visible="detailDialogVisible"
    :title="'乘车人详情'"
    :form-data="passengerDetail"
    :fields="detailFields"
    :is-edit="false"
    :show-footer="false"
  />
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getPassengerListService, getPassengerDetailService } from '@/api/passenger'
import { ElMessage, ElMessageBox } from 'element-plus'
import DialogForm from '@/components/DialogForm/DialogForm.vue'
import { formatDate } from '@/utils/formatDate'

// 证件类型常量
const CARD_TYPE = {
  ID_CARD: '身份证',
  PASSPORT: '护照',
  // 获取证件类型文本
  getText: (type) => {
    const typeMap = {
      [CARD_TYPE.ID_CARD]: '身份证',
      [CARD_TYPE.PASSPORT]: '护照'
    }
    return typeMap[type] || '未知类型'
  }
}

// 表单相关响应式数据
const detailDialogVisible = ref(false)
const passengerDetail = ref({
  id: '',
  realName: '',
  cardType: '',
  idCard: '',
  createTime: '',
  updateTime: '',
  createPerson: '',
  updatePerson: ''
})

// 详情表单字段配置
const detailFields = [
  {
    prop: 'realName',
    label: '真实姓名',
    type: 'input',
    disabled: true
  },
  {
    prop: 'cardType',
    label: '证件类型',
    type: 'input',
    disabled: true
  },
  {
    prop: 'idCard',
    label: '证件号码',
    type: 'input',
    disabled: true
  },
  {
    prop: 'createTime',
    label: '创建时间',
    type: 'input',
    disabled: true
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    type: 'input',
    disabled: true
  },
  {
    prop: 'createPerson',
    label: '创建人',
    type: 'input',
    disabled: true
  },
  {
    prop: 'updatePerson',
    label: '更新人',
    type: 'input',
    disabled: true
  }
]

// 响应式数据
const passengerList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 表格列配置
const tableColumns = [
  { prop: 'id', label: '乘车人编号', width: 200, align: 'center' },
  { prop: 'realName', label: '真实姓名', minWidth: 120, align: 'center' },
  { prop: 'cardType', label: '证件类型', minWidth: 120, align: 'center' },
  { prop: 'idCard', label: '证件号码', minWidth: 200, align: 'center', showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', minWidth: 200, align: 'center' },
  { prop: 'updateTime', label: '更新时间', minWidth: 200, align: 'center' },
  { prop: 'createPerson', label: '创建人', minWidth: 150, align: 'center' },
  { prop: 'updatePerson', label: '更新人', minWidth: 150, align: 'center' },
  { prop: 'action', label: '操作', width: 100, align: 'center', fixed: 'right' }
]

// 搜索字段配置
const searchFields = [
  {
    prop: 'realName',
    label: '真实姓名',
    type: 'input',
    placeholder: '请输入真实姓名',
    clearable: true
  },
  {
    prop: 'cardType',
    label: '证件类型',
    type: 'select',
    placeholder: '请选择证件类型',
    clearable: true,
    width: '150px',
    options: [
      { label: '身份证', value: 'ID_CARD' },
      { label: '护照', value: 'PASSPORT' }
    ]
  },
  {
    prop: 'idCard',
    label: '证件号码',
    type: 'input',
    placeholder: '请输入证件号码',
    clearable: true
  }
]

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})



/**
 * 获取乘车人列表
 * @param {Object} searchParams - 搜索参数
 */
const fetchPassengerList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    // API调用
    const response = await getPassengerListService(params)
    const data = response.records
    
    // 模拟分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    const paginatedData = data.slice(start, end)
    
    passengerList.value = paginatedData
    total.value = Number(response.total)
  } catch (error) {
    passengerList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/**
 * 获取证件类型文本
 * @param {string} type - 证件类型
 * @returns {string} 证件类型文本
 */
const getCardTypeText = (type) => {
  return CARD_TYPE.getText(type)
}

/**
 * 处理查询操作
 * @param {Object} formData - 搜索表单数据
 */
const handleSearch = (formData) => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchPassengerList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchPassengerList({}) // 重置后自动查询
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
  fetchPassengerList()
}

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchPassengerList()
}

/**
 * 查看乘车人详情
 * @param {Object} row - 乘车人数据
 */
const handleViewDetail = async (row) => {
  if (!row) {
    ElMessage.warning('请选择一个乘车人查看详情')
    return
  }
  
  try {
    loading.value = true
    const detail = await getPassengerDetailService(row.id)
    passengerDetail.value = {
      ...detail,
      cardType: getCardTypeText(detail.cardType) // 格式化证件类型为可读文本
    }
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取乘车人详情失败')
  } finally {
    loading.value = false
  }
}

// 组件挂载后加载数据
onMounted(() => {
  fetchPassengerList()
})
</script>

<style scoped lang="scss">
/* 使用ListPage组件的样式，无需额外样式 */
@use './Passenger-list-view.scss';
</style>