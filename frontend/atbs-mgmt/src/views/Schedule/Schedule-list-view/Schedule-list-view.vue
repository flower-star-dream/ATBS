<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage
    title="班次列表"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="scheduleList"
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
    <!-- 操作按钮区域 -->
    <template #actions>
      <el-button type="primary" @click="handleAdd">新增班次</el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        批量删除
        <span class="selected-count">({{ selectedRows.length }})</span>
      </el-button>
    </template>

    <!-- 自定义列车名列 -->
    <template #column-trainName="{ row }">
      <el-tag type="info" size="small">{{ row.trainName || '-' }}</el-tag>
    </template>

    <!-- 自定义线路名列 -->
    <template #column-routeInfo="{ row }">
      <span>{{ row.routeInfo || row.routeName || '-' }}</span>
    </template>

    <!-- 自定义出发时间列 -->
    <template #column-startTime="{ row }">
      {{ formatDate(row.startTime) }}
    </template>

    <!-- 自定义结束时间列 -->
    <template #column-endTime="{ row }">
      {{ formatDate(row.endTime) }}
    </template>

    <!-- 自定义余票列 -->
    <template #column-availableTickets="{ row }">
      <el-tag 
        :type="getTicketStatusType(row.availableTickets)" 
        size="small"
      >
        {{ row.availableTickets }}
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
      <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
      <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
    </template>
  </ListPage>

  <!-- 使用通用表单弹窗组件 -->
  <DialogForm
    v-model:visible="dialogVisible"
    :title="dialogTitle"
    :form-data="scheduleForm"
    :fields="formFields"
    :rules="formRules"
    :is-edit="isEdit"
    :confirm-text="isEdit ? '更新' : '新增'"
    :loading="submitLoading"
    @submit="handleFormSubmit"
  >
    <!-- 自定义出发时间字段 -->
    <template #field-startTime>
      <el-date-picker
        v-model="scheduleForm.startTime"
        type="datetime"
        placeholder="请选择出发时间"
        format="YYYY-MM-DD HH:mm:ss"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 100%"
      />
    </template>
    
    <!-- 自定义到达时间字段 -->
    <template #field-endTime>
      <el-date-picker
        v-model="scheduleForm.endTime"
        type="datetime"
        placeholder="请选择到达时间"
        format="YYYY-MM-DD HH:mm:ss"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 100%"
      />
    </template>
  </DialogForm>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getScheduleList, addSchedule, updateSchedule, deleteSchedule } from '@/api/schedule'
import { getTrainList } from '@/api/train'
import { getRouteList } from '@/api/route'
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
const scheduleForm = ref({
  id: '',
  trainId: '',
  routeId: '',
  conductor: '',
  availableTickets: 0,
  startTime: '',
  endTime: ''
})

// 远程搜索相关响应式数据
const searchTrains = ref([])
const searchRoutes = ref([])
const trainLoading = ref(false)
const routeLoading = ref(false)
// 分页相关响应式数据
const trainCurrentPage = ref(1)
const routeCurrentPage = ref(1)
const trainTotal = ref(0)
const routeTotal = ref(0)
const trainKeyword = ref('')
const routeKeyword = ref('')
const trainAllLoaded = ref(false)
const routeAllLoaded = ref(false)
const isLoadingMore = ref(false)

/**
 * 列车远程搜索方法
 * @param {string} query - 搜索关键词
 */
const handleTrainRemoteSearch = async (query) => {
  // 重置状态
  trainCurrentPage.value = 1
  trainAllLoaded.value = false
  trainKeyword.value = query

  trainLoading.value = true
  try {
    // 调用带分页的API获取列车列表
    const response = await getTrainList({
      page: 1,
      pageSize: 10,
      keyword: query
    })

    // 更新总数和列车列表
    trainTotal.value = response.total
    searchTrains.value = response.records.map(train => ({
      value: train.id,
      label: train.trainName,
      trainModel: train.trainModel,
      seatNum: train.seatNum,
      serviceYears: train.serviceYears
    }))
  } catch (error) {
    ElMessage.error('获取列车列表失败')
    searchTrains.value = []
  } finally {
    trainLoading.value = false
    isLoadingMore.value = false
    // 添加滚动监听
    addScrollListener('train')
  }
}

/**
 * 线路远程搜索方法
 * @param {string} query - 搜索关键词
 */
const handleRouteRemoteSearch = async (query) => {
  // 重置状态
  routeCurrentPage.value = 1
  routeAllLoaded.value = false
  routeKeyword.value = query

  routeLoading.value = true
  try {
    // 调用带分页的API获取线路列表
    const response = await getRouteList({
      page: 1,
      pageSize: 10,
      keyword: query
    })

    // 更新总数和线路列表
    routeTotal.value = response.total
    searchRoutes.value = response.records.map(route => ({
      value: route.id,
      label: route.routeName,
      startStationName: route.startStationName,
      endStationName: route.endStationName,
      stationCount: route.stationCount
    }))
  } catch (error) {
    ElMessage.error('获取线路列表失败')
    searchRoutes.value = []
  } finally {
    routeLoading.value = false
    isLoadingMore.value = false
    // 添加滚动监听
    addScrollListener('route')
  }
}

// 表单字段配置
const formFields = computed(() => [
  {
    prop: 'trainId',
    label: '列车',
    type: 'select',
    placeholder: '请选择列车',
    options: searchTrains,
    required: true,
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleTrainRemoteSearch,
    loading: trainLoading.value,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'train-select',
    teleported: false,
    appendToBody: false
  },
  {
    prop: 'routeId',
    label: '线路',
    type: 'select',
    placeholder: '请选择线路',
    options: searchRoutes,
    required: true,
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleRouteRemoteSearch,
    loading: routeLoading.value,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'route-select',
    teleported: false,
    appendToBody: false
  },
  {
    prop: 'conductor',
    label: '列车长',
    type: 'input',
    placeholder: '请输入列车长姓名',
    maxlength: 10,
    showWordLimit: true,
    required: true
  },
  {
    prop: 'availableTickets',
    label: '余票数',
    type: 'input',
    placeholder: '请输入余票数',
    inputType: 'number',
    required: true
  },
  {
    prop: 'startTime',
    label: '出发时间',
    type: 'custom',
    required: true
  },
  {
    prop: 'endTime',
    label: '到达时间',
    type: 'custom',
    required: true
  }
])

// 表单验证规则
const formRules = computed(() => ({
  trainId: [
    { required: true, message: '请选择列车', trigger: 'change' }
  ],
  routeId: [
    { required: true, message: '请选择线路', trigger: 'change' }
  ],
  conductor: [
    { required: true, message: '请输入列车长姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '列车长姓名长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  availableTickets: [
    { required: true, message: '请输入余票数', trigger: 'blur' },
    { type: 'number', min: 0, max: 2000, message: '余票数必须在 0 到 2000 之间', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择出发时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择到达时间', trigger: 'change' }
  ]
}))

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑班次' : '新增班次')

// 响应式数据
const scheduleList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '班次号',
    width: 100,
    align: 'center'
  },
  {
    prop: 'trainName',
    label: '列车',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'routeInfo',
    label: '线路信息',
    minWidth: 150,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'conductor',
    label: '列车长',
    width: 100,
    align: 'center'
  },
  {
    prop: 'availableTickets',
    label: '余票数',
    width: 100,
    align: 'center'
  },
  {
    prop: 'startTime',
    label: '出发时间',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'endTime',
    label: '到达时间',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'createPerson',
    label: '创建人',
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
const searchFields = computed(() => [
  {
    prop: 'trainId',
    label: '列车',
    type: 'select',
    placeholder: '请选择列车',
    options: searchTrains,
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleTrainRemoteSearch,
    loading: trainLoading.value,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'train-select',
    teleported: false,
    appendToBody: false
  },
  {
    prop: 'routeId',
    label: '线路',
    type: 'select',
    placeholder: '请选择线路',
    options: searchRoutes,
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleRouteRemoteSearch,
    loading: routeLoading.value,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'route-select',
    teleported: false,
    appendToBody: false
  },
  {
    prop: 'conductor',
    label: '列车长',
    type: 'input',
    placeholder: '请输入列车长姓名',
    clearable: true
  }
])

// 初始搜索表单数据
const initialSearchForm = computed(() => searchFields.value.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {}))

/**
 * 获取余票状态标签类型
 * @param {number} tickets - 余票数
 * @returns {string} 标签类型
 */
const getTicketStatusType = (tickets) => {
  if (tickets <= 0) return 'danger'
  if (tickets <= 10) return 'warning'
  return 'success'
}

/**
 * 获取列车选项列表（默认加载第一页数据）
 */
const fetchTrainOptions = async () => {
  try {
    const response = await getTrainList({
      page: 1,
      pageSize: 10
    })
    
    trainTotal.value = response.total
    searchTrains.value = response.records.map(train => ({
      value: train.id,
      label: train.trainName,
      trainModel: train.trainModel,
      seatNum: train.seatNum,
      serviceYears: train.serviceYears
    }))
  } catch (error) {
    console.error('获取列车选项失败:', error)
    ElMessage.error('获取列车选项失败')
  }
}


/**
 * 加载更多数据（列车或线路）
 * @param {string} type - 类型：'train' 或 'route'
 */
const loadMoreData = async (type) => {
  // 避免重复加载
  if (isLoadingMore.value) return

  const isTrain = type === 'train'
  const currentPage = isTrain ? trainCurrentPage.value : routeCurrentPage.value
  const allLoaded = isTrain ? trainAllLoaded.value : routeAllLoaded.value
  const keyword = isTrain ? trainKeyword.value : routeKeyword.value

  // 如果已加载全部或无关键词，则不加载
  if (allLoaded || !keyword) return

  isLoadingMore.value = true
  try {
    // 调用API获取下一页数据
    const response = isTrain
      ? await getTrainList({
          page: currentPage + 1,
          pageSize: 10,
          keyword: keyword
        })
      : await getRouteList({
          page: currentPage + 1,
          pageSize: 10,
          keyword: keyword
        })

    // 检查是否还有更多数据
    const currentDataLength = isTrain ? searchTrains.value.length : searchRoutes.value.length
    const hasMore = currentDataLength + response.records.length < (isTrain ? trainTotal.value : routeTotal.value)

    // 更新状态
    if (isTrain) {
      trainCurrentPage.value++
      trainAllLoaded.value = !hasMore
    } else {
      routeCurrentPage.value++
      routeAllLoaded.value = !hasMore
    }

    // 合并新数据到现有列表
    const newOptions = isTrain
      ? response.records.map(train => ({
          value: train.id,
          label: train.trainName,
          trainModel: train.trainModel,
          seatNum: train.seatNum,
          serviceYears: train.serviceYears
        }))
      : response.records.map(route => ({
          value: route.id,
          label: route.routeName,
          startStationName: route.startStationName,
          endStationName: route.endStationName,
          stationCount: route.stationCount
        }))

    // 避免重复项
    const existingData = isTrain ? searchTrains.value : searchRoutes.value
    const existingValues = new Set(existingData.map(item => item.value))
    const filteredNewOptions = newOptions.filter(item => !existingValues.has(item.value))

    if (isTrain) {
      searchTrains.value = [...searchTrains.value, ...filteredNewOptions]
    } else {
      searchRoutes.value = [...searchRoutes.value, ...filteredNewOptions]
    }
  } catch (error) {
    ElMessage.error(`加载更多${isTrain ? '列车' : '线路'}失败`)
  } finally {
    isLoadingMore.value = false
  }
}

/**
 * 为Select组件添加滚动事件监听的方法
 * @param {string} type - 类型：'train' 或 'route'
 */
const addScrollListener = (type) => {
  // 使用setTimeout确保DOM已更新
  setTimeout(() => {
    // 为所有下拉框添加滚动监听
    const dropdowns = document.querySelectorAll(`.${type}-select .el-select-dropdown__wrap`)
    dropdowns.forEach(dropdown => {
      // 移除可能存在的旧监听
      dropdown.removeEventListener('scroll', scrollHandler)
      // 添加新监听
      dropdown.addEventListener('scroll', scrollHandler)
    })

    // 滚动处理函数
    function scrollHandler(e) {
      const { scrollTop, scrollHeight, clientHeight } = e.target
      // 当滚动到底部（距离底部10px内）时加载更多
      if (scrollTop + clientHeight >= scrollHeight - 10) {
        loadMoreData(type)
      }
    }
  }, 100)
}

/**
 * 获取线路选项列表（默认加载第一页数据）
 */
const fetchRouteOptions = async () => {
  try {
    const response = await getRouteList({
      page: 1,
      pageSize: 10
    })
    
    routeTotal.value = response.total
    searchRoutes.value = response.records.map(route => ({
      value: route.id,
      label: route.routeName,
      startStationName: route.startStationName,
      endStationName: route.endStationName,
      stationCount: route.stationCount
    }))
  } catch (error) {
    console.error('获取线路选项失败:', error)
    ElMessage.error('获取线路选项失败')
  }
}


/**
 * 获取班次列表
 * @param {Object} searchParams - 搜索参数
 */
const fetchScheduleList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    // API调用
    const response = await getScheduleList(params)
    const data = response.records || []
    
    // 模拟分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    const paginatedData = data.slice(start, end)
    
    scheduleList.value = paginatedData
    total.value = data.length
  } catch (error) {
    scheduleList.value = []
    total.value = 0
    ElMessage.error('获取班次列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 处理查询操作
 * @param {Object} formData - 搜索表单数据
 */
const handleSearch = (formData) => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchScheduleList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchScheduleList({}) // 重置后自动查询
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
  fetchScheduleList()
}

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchScheduleList()
}

/**
 * 新增班次
 */
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  // 重置表单数据
  scheduleForm.value = {
    id: '',
    trainId: '',
    routeId: '',
    conductor: '',
    availableTickets: 0,
    startTime: '',
    endTime: ''
  }

  // 在弹窗打开时重置并添加滚动监听
  trainCurrentPage.value = 1
  routeCurrentPage.value = 1
  trainAllLoaded.value = false
  routeAllLoaded.value = false
}

/**
 * 批量删除班次
 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的班次')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个班次吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const ids = selectedRows.value.map(row => row.id)
    await deleteSchedule(ids)
    ElMessage.success(`成功删除 ${selectedRows.value.length} 个班次`)
    
    // 删除成功后刷新列表
    fetchScheduleList()
  } catch (error) {
    // 用户取消删除或发生错误
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 编辑班次
 * @param {Object} row - 班次数据
 */
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  // 复制数据到表单
  scheduleForm.value = {
    id: row.id,
    trainId: row.trainId,
    routeId: row.routeId,
    conductor: row.conductor,
    availableTickets: row.availableTickets,
    startTime: row.startTime,
    endTime: row.endTime
  }

  // 在编辑弹窗打开时重置并添加滚动监听
  trainCurrentPage.value = 1
  routeCurrentPage.value = 1
  trainAllLoaded.value = false
  routeAllLoaded.value = false
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
        // 编辑班次
        await updateSchedule(submitData)
        ElMessage.success('更新班次成功')
      } else {
        // 新增班次
        await addSchedule(submitData)
        ElMessage.success('新增班次成功')
      }
    
    // 关闭弹窗
    dialogVisible.value = false
    // 刷新列表
    fetchScheduleList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除班次
 * @param {Object} row - 班次数据
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除班次「${row.id}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除接口
    await deleteSchedule([row.id])
    ElMessage.success('删除成功')
    // 删除成功后刷新列表
    fetchScheduleList()
  } catch (error) {
    // 用户取消删除或发生错误
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 组件挂载后加载数据
onMounted(() => {
  fetchScheduleList()
  fetchTrainOptions()
  fetchRouteOptions()
})
</script>

<style scoped lang="scss">
/* 使用ListPage组件的样式，无需额外样式 */
.selected-count {
  margin-left: 4px;
  font-weight: bold;
}
</style>