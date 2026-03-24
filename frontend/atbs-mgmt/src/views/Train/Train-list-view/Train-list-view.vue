<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage
    title="列车列表"
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
    @size-change="handleSizeChange"
    @current-change="handlePageChange"
    @search="handleSearch"
    @reset="handleReset"
    @selection-change="handleSelectionChange"
  >
    <!-- 操作按钮区域 -->
    <template #actions>
      <el-button type="primary" @click="handleAdd">新增列车</el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        批量删除
        <span class="selected-count">({{ selectedRows.length }})</span>
      </el-button>
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
      <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
      <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
    </template>
  </ListPage>

  <!-- 使用通用表单弹窗组件 -->
  <DialogForm
    v-model:visible="dialogVisible"
    :title="dialogTitle"
    :form-data="trainForm"
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
import { getTrainList, addTrain, updateTrain, deleteTrain } from '@/api/train'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmployeeStore } from '@/stores'
import DialogForm from '@/components/DialogForm/DialogForm.vue'
import { formatDate } from '@/utils/formatDate'

const employeeStore = useEmployeeStore()
const employeeInfo = computed(() => employeeStore.employeeInfo)

// 表单相关响应式数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const trainForm = ref({
  id: '',
  trainName: '',
  trainModel: '',
  seatNum: 1,
  serviceYears: 0
})

// 表单字段配置
const formFields = [
  {
    prop: 'trainName',
    label: '列车名',
    type: 'input',
    placeholder: '请输入列车名',
    maxlength: 10,
    showWordLimit: true,
    disabled: (isEditMode) => {
      return isEditMode 
    }
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
  }
]

// 表单验证规则
const formRules = computed(() => ({
  trainName: [
    { required: true, message: '请输入列车名', trigger: 'blur' },
    { min: 2, max: 10, message: '列车名长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  trainModel: [
    { required: true, message: '请输入列车型号', trigger: 'blur' },
    { max: 50, message: '列车型号长度不能超过 50 个字符', trigger: 'blur' }
  ],
  seatNum: [
    { required: true, message: '请输入座位数', trigger: 'blur' },
    { type: 'number', min: 1, max: 2000, message: '座位数必须在 1 到 2000 之间', trigger: 'blur' }
  ],
  serviceYears: [
    { required: true, message: '请输入服务年数', trigger: 'blur' },
    { type: 'number', min: 0, max: 50, message: '服务年数必须在 0 到 50 之间', trigger: 'blur' }
  ]
}))

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑列车' : '新增列车')

// 响应式数据
const trainList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 已删除状态标签页相关配置

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '列车号',
    width: 120,
    align: 'center'
  },
  {
    prop: 'trainName',
    label: '列车名',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'trainModel',
    label: '列车型号',
    minWidth: 150,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'seatNum',
    label: '座位数',
    width: 100,
    align: 'center'
  },
  {
    prop: 'serviceYears',
    label: '服务年数',
    width: 100,
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
    width: 160,
    align: 'center',
    fixed: 'right'
  }
]

// 搜索字段配置
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

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})



/**
 * 获取列车列表
 * @param {Object} searchParams - 搜索参数
 */
const fetchTrainList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    // API调用
    const response = await getTrainList(params)
    const data = response.records
    
    // 模拟分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    const paginatedData = data.slice(start, end)
    
    trainList.value = paginatedData
    total.value = data.length
  } catch (error) {
    trainList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 已移除状态相关函数

/**
 * 处理查询操作
 * @param {Object} formData - 搜索表单数据
 */
const handleSearch = (formData) => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchTrainList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchTrainList({}) // 重置后自动查询
}

/**
 * 处理表格选择变化
 * @param {Array} selection - 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

/**
 * 新增列车
 */
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  // 重置表单数据
  trainForm.value = {
    id: '',
    trainName: '',
    trainModel: '',
    seatNum: 0,
    serviceYears: 0,
    status: TRAIN_STATUS.ENABLED
  }
}

/**
 * 批量删除列车
 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的列车')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个列车吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const ids = selectedRows.value.map(row => row.id)
    for (const id of ids) {
      await deleteTrain(id)
    }
    ElMessage.success(`成功删除 ${selectedRows.value.length} 个列车`)
    
    // 删除成功后刷新列表
    fetchTrainList()
  } catch (error) {
    // 用户取消删除或发生错误
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 分页大小变化
 * @param {number} size - 每页条数
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchTrainList()
}

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchTrainList()
}

/**
 * 编辑列车
 * @param {Object} row - 列车数据
 */
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  // 复制数据到表单
  trainForm.value = {
    id: row.id,
    trainName: row.trainName,
    trainModel: row.trainModel,
    seatNum: row.seatNum,
    serviceYears: row.serviceYears,
    status: row.status
  }
}

/**
 * 处理表单提交
 * @param {Object} formData - 表单数据
 */
const handleFormSubmit = async (formData) => {
  try {
    submitLoading.value = true
    
    // 准备提交数据
    const submitData = { ...formData }
    
    // 设置创建人或更新人
    if (isEdit.value) {
      submitData.updatePerson = employeeInfo.value?.nickname || employeeInfo.value?.username
    } else {
      submitData.createPerson = employeeInfo.value?.nickname || employeeInfo.value?.username
    }
    
    if (isEdit.value) {
        // 编辑列车
        await updateTrain(submitData)
        ElMessage.success('更新列车成功')
      } else {
        // 新增列车
        await addTrain(submitData)
      ElMessage.success('新增列车成功')
    }
    
    // 关闭弹窗
    dialogVisible.value = false
    // 刷新列表
    fetchTrainList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

// 状态处理函数已删除

/**
 * 删除列车
 * @param {Object} row - 列车数据
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除列车「${row.trainName}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除接口
    await deleteTrain([row.id])
    ElMessage.success('删除成功')
    // 删除成功后刷新列表
    fetchTrainList()
  } catch (error) {
    // 用户取消删除或发生错误
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 组件挂载后加载数据
onMounted(() => {
  fetchTrainList()
})
</script>

<style scoped lang="scss">
/* 使用ListPage组件的样式，无需额外样式 */
@use './Train-list-view.scss';
</style>