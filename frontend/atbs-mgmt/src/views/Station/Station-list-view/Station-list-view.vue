<template>
  <ListPage
    title="站点管理"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="stationList"
    :table-columns="tableColumns"
    :search-fields="searchFields"
    :initial-search-form="initialSearchForm"
    :pagination="true"
    :show-pagination="true"
    @size-change="handleSizeChange"
    @current-change="handlePageChange"
    @search="handleSearch"
    @reset="handleReset"
    @selection-change="handleSelectionChange"
  >
    <template #actions>
      <el-button type="primary" @click="handleAdd">新增站点</el-button>
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
    :form-data="stationForm"
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
import { getStationList, addStation, updateStation, deleteStation } from '@/api/station'
import { formatDate } from '@/utils/formatDate'

const employeeStore = useEmployeeStore()
const employeeInfo = computed(() => employeeStore.employeeInfo)

// 表单相关响应式数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)

const stationForm = ref({
  id: '',
  stationName: '',
  address: ''
})

// 表单字段配置
const formFields = [
  {
    prop: 'stationName',
    label: '站点名',
    type: 'input',
    placeholder: '请输入站点名',
    maxlength: 50,
    showWordLimit: true,
    disabled: (isEditMode) => isEditMode
  },
  {
    prop: 'address',
    label: '地址',
    type: 'input',
    placeholder: '请输入站点地址',
    maxlength: 100,
    showWordLimit: true
  }
]

// 表单验证规则
const formRules = computed(() => ({
  stationName: [
    { required: true, message: '请输入站点名', trigger: 'blur' },
    { min: 2, max: 50, message: '站点名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入站点地址', trigger: 'blur' },
    { max: 100, message: '地址长度不能超过 100 个字符', trigger: 'blur' }
  ]
}))

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑站点' : '新增站点')

// 响应式数据
const stationList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '站点号',
    width: 120,
    align: 'center'
  },
  {
    prop: 'stationName',
    label: '站点名',
    minWidth: 150,
    align: 'center'
  },
  {
    prop: 'address',
    label: '地址',
    minWidth: 200,
    align: 'center',
    showOverflowTooltip: true
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
    prop: 'stationName',
    label: '站点名',
    type: 'input',
    placeholder: '请输入站点名',
    clearable: true
  },
  {
    prop: 'address',
    label: '地址',
    type: 'input',
    placeholder: '请输入地址',
    clearable: true
  }
]

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})

/**
 * 获取站点列表
 */
const fetchStationList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    const response = await getStationList(params)
    const data = response.records || []
    
    // 模拟分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    const paginatedData = data.slice(start, end)
    
    stationList.value = paginatedData
    total.value = data.length
  } catch (error) {
    stationList.value = []
    total.value = 0
    ElMessage.error('获取站点列表失败')
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
  fetchStationList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  currentPage.value = 1
  selectedRows.value = []
  fetchStationList({})
}

// 状态标签页切换处理函数已移除

/**
 * 处理分页大小变化
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchStationList()
}

/**
 * 处理分页切换
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchStationList()
}

/**
 * 处理表格选择变化
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

/**
 * 新增站点
 */
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  stationForm.value = {
    id: '',
    stationName: '',
    address: ''
  }
}

/**
 * 批量删除站点
 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的站点')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个站点吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const ids = selectedRows.value.map(row => row.id)
    await deleteStation(ids)
    ElMessage.success(`成功删除 ${selectedRows.value.length} 个站点`)
    
    fetchStationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 编辑站点
 */
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  stationForm.value = {
    id: row.id,
    stationName: row.stationName,
    address: row.address
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
        await updateStation(submitData)
        ElMessage.success('更新站点成功')
      } else {
        submitData.createPerson = employeeInfo.value?.nickname || employeeInfo.value?.username
        await addStation(submitData)
        ElMessage.success('新增站点成功')
      }
    
    dialogVisible.value = false
    fetchStationList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除站点
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除站点「${row.stationName}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteStation([row.id])
    ElMessage.success('删除成功')
    fetchStationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 组件挂载后加载数据
onMounted(() => {
  fetchStationList()
})
</script>

<style scoped lang="scss">
@use './Station-list-view.scss';
</style>