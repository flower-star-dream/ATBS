<template>
  <ListPage
    title="路线站点管理"
    :show-tabs="true"
    :tabs="tabs"
    :active-tab="statusFilter"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="routeStationList"
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
    <template #actions>
      <el-button type="primary" @click="handleAdd">新增路线站点</el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        批量删除
        <span class="selected-count">({{ selectedRows.length }})</span>
      </el-button>
    </template>

    <template #column-createTime="{ row }">
      {{ formatDate(row.createTime) }}
    </template>
    <template #column-updateTime="{ row }">
      {{ formatDate(row.updateTime) }}
    </template>

    <template #column-action="{ row }">
      <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
      <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
    </template>
  </ListPage>

  <DialogForm
    v-model:visible="dialogVisible"
    :title="dialogTitle"
    :form-data="routeStationForm"
    :fields="formFields"
    :rules="formRules"
    :is-edit="isEdit"
    :confirm-text="isEdit ? '更新' : '新增'"
    :loading="submitLoading"
    @submit="handleFormSubmit"
  />
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmployeeStore } from '@/stores'
import DialogForm from '@/components/DialogForm/DialogForm.vue'
import ListPage from '@/components/ListPage/ListPage.vue'
import {
  getRouteStationList,
  addRouteStation,
  updateRouteStation,
  deleteRouteStation
} from '@/api/route-station'
import { getRouteList } from '@/api/route'
import { getStationList } from '@/api/station'
import { formatDate } from '@/utils/formatDate'

const employeeStore = useEmployeeStore()
const employeeInfo = computed(() => employeeStore.employeeInfo)

// 表单相关响应式数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const dialogTitle = computed(() => isEdit.value ? '编辑路线站点' : '新增路线站点')

// 路线站点表单数据
const routeStationForm = ref({
  id: '',
  routeId: '',
  stationId: '',
  stationSorting: 1
})

// 表格相关响应式数据
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const routeStationList = ref([])
const statusFilter = ref('all')

// 下拉选项数据
const routeOptions = ref([])
const stationOptions = ref([])

// 表单字段配置
const formFields = computed(() => [
  {
    prop: 'routeId',
    label: '选择路线',
    type: 'select',
    placeholder: '请选择路线',
    options: routeOptions.value,
    disabled: (isEditMode) => isEditMode
  },
  {
    prop: 'stationId',
    label: '选择站点',
    type: 'select',
    placeholder: '请选择站点',
    options: stationOptions.value,
    disabled: (isEditMode) => isEditMode
  },
  {
    prop: 'stationSorting',
    label: '排序位置',
    type: 'number',
    placeholder: '请输入排序位置',
    min: 1
  }
])

// 表单验证规则
const formRules = computed(() => ({
  routeId: [
    { required: true, message: '请选择路线', trigger: 'change' }
  ],
  stationId: [
    { required: true, message: '请选择站点', trigger: 'change' }
  ],
  stationSorting: [
    { required: true, message: '请输入排序位置', trigger: 'blur' },
    { type: 'number', min: 1, message: '排序位置必须大于0', trigger: 'blur' }
  ]
}))

// 表格列配置
const tableColumns = [
  {
    prop: 'routeName',
    label: '路线名称',
    minWidth: 180,
    align: 'center'
  },
  {
    prop: 'stationName',
    label: '站点名称',
    minWidth: 180,
    align: 'center'
  },
  {
    prop: 'stationSorting',
    label: '排序位置',
    width: 120,
    align: 'center'
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 180,
    align: 'center'
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 180,
    align: 'center'
  },
  {
    prop: 'createPerson',
    label: '创建人',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'updatePerson',
    label: '更新者',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    align: 'center',
    fixed: 'right'
  }
]

// 搜索字段配置
const searchFields = [
  {
    prop: 'routeName',
    label: '路线名称',
    type: 'input',
    placeholder: '请输入路线名称',
    clearable: true
  },
  {
    prop: 'stationName',
    label: '站点名称',
    type: 'input',
    placeholder: '请输入站点名称',
    clearable: true
  }
]

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})

// 标签页配置
const tabs = [
  {
    name: 'all',
    label: '全部',
    count: 0,
    lazy: true
  }
]



/**
 * 加载下拉选项数据
 */
const loadOptions = async () => {
  try {
    // 加载路线选项
      const routesRes = await getRouteList()
      routeOptions.value = routesRes.data?.records?.map(route => ({
      label: route.routeName,
      value: route.id
    })) || []

    // 加载站点选项
      const stationsRes = await getStationList({ page: 1, pageSize: 100 })
      stationOptions.value = stationsRes.data?.records?.map(station => ({
      label: station.stationName,
      value: station.id
    })) || []
  } catch (error) {
    ElMessage.error('加载选项数据失败')
  }
}

/**
 * 获取路线站点列表
 */
const fetchRouteStationList = async (searchParams = {}) => {
  loading.value = true
  try {
      const params = {
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        ...searchParams
      }
      
      const response = await getRouteStationList(params)
      routeStationList.value = response.data?.records || []
      total.value = response.data?.total || 0
  } catch (error) {
    routeStationList.value = []
    total.value = 0
    ElMessage.error('获取路线站点列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 处理查询操作
 */
const handleSearch = (formData) => {
  currentPage.value = 1
  selectedRows.value = []
  fetchRouteStationList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  currentPage.value = 1
  selectedRows.value = []
  fetchRouteStationList({})
}

/**
 * 处理状态标签页切换
 */
const handleStatusTabChange = (tabName) => {
  statusFilter.value = tabName
  currentPage.value = 1
  selectedRows.value = []
  fetchRouteStationList()
}

/**
 * 处理分页大小变化
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchRouteStationList()
}

/**
 * 处理分页切换
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchRouteStationList()
}

/**
 * 处理表格选择变化
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

/**
 * 新增路线站点
 */
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  routeStationForm.value = {
    id: '',
    routeId: '',
    stationId: '',
    stationSorting: 1
  }
}

/**
 * 批量删除路线站点
 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的路线站点')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个路线站点吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const ids = selectedRows.value.map(row => row.id)
    await deleteRouteStation(ids)
    ElMessage.success(`成功删除 ${selectedRows.value.length} 个路线站点`)
    
    fetchRouteStationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 编辑路线站点
 */
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  routeStationForm.value = {
    id: row.id,
    routeId: row.routeId,
    stationId: row.stationId,
    stationSorting: row.stationSorting
  }
}

/**
 * 处理表单提交
 */
const handleFormSubmit = async (formData) => {
  try {
    submitLoading.value = true
    
    const submitData = { ...formData }
    
    if (isEdit.value) {
      submitData.updatePerson = employeeInfo.value?.nickname || employeeInfo.value?.username
      await updateRouteStation(submitData)
      ElMessage.success('更新路线站点成功')
    } else {
      submitData.createPerson = employeeInfo.value?.nickname || employeeInfo.value?.username
      await addRouteStation(submitData)
      ElMessage.success('新增路线站点成功')
    }
    
    dialogVisible.value = false
    fetchRouteStationList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除路线站点
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除路线站点吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteRouteStation([row.id])
    ElMessage.success('删除成功')
    fetchRouteStationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 初始化函数
 */
const init = async () => {
  await loadOptions()
  await fetchRouteStationList()
}

// 组件挂载后加载数据
onMounted(() => {
  init()
})
</script>

<style scoped lang="scss">
.selected-count {
  color: #f56c6c;
  font-weight: bold;
}
</style>