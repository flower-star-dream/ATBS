<template>
  <ListPage title="线路管理" :total="total" :current-page="currentPage" :page-size="pageSize"
    :selected-count="selectedRows.length" :loading="loading" :show-selection="true" :table-data="routeList"
    :table-columns="tableColumns" :search-fields="searchFields" :initial-search-form="initialSearchForm"
    :pagination="true" :show-pagination="true" @size-change="handleSizeChange" @current-change="handlePageChange"
    @search="handleSearch" @reset="handleReset" @selection-change="handleSelectionChange">
    <template #actions>
      <el-button type="primary" @click="handleAdd">新增线路</el-button>
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
      <el-button type="primary" text size="small" @click="handleManageStations(row)">站点管理</el-button>
      <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
    </template>
  </ListPage>

  <DialogForm v-model:visible="dialogVisible" :title="dialogTitle" :form-data="routeForm" :fields="formFields"
    :rules="formRules" :is-edit="isEdit" :confirm-text="isEdit ? '更新' : '新增'" :loading="submitLoading"
    @submit="handleFormSubmit" />

  <!-- 线路站点管理弹窗 -->
  <RouteStationDialog v-model:visible="routeStationDialogVisible" :route-id="currentRouteId"
    :route-name="currentRouteName" @close="handleRouteStationDialogClose" />
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmployeeStore } from '@/stores'
import DialogForm from '@/components/DialogForm/DialogForm.vue'
import RouteStationDialog from '../Route-Station-Dialog/Route-Station-Dialog.vue'
import ListPage from '@/components/ListPage/ListPage.vue'
import { getRouteList, addRoute, updateRoute, deleteRoute } from '@/api/route'
import { getStationList } from '@/api/station'
import { formatDate } from '@/utils/formatDate'

const employeeStore = useEmployeeStore()
const employeeInfo = computed(() => employeeStore.employeeInfo)

// 表单相关响应式数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const routeStationDialogVisible = ref(false)
const currentRouteId = ref(0)
const currentRouteName = ref('')

const routeForm = ref({
  id: '',
  routeName: '',
  startStationId: '',
  endStationId: ''
})

// 站点下拉选项
const stationOptions = ref([])

// 表单字段配置
const formFields = computed(() => [
  {
    prop: 'routeName',
    label: '线路名',
    type: 'input',
    placeholder: '请输入线路名',
    maxlength: 20,
    showWordLimit: true,
    disabled: (isEditMode) => isEditMode
  },
  {
    prop: 'startStationId',
    label: '起点站',
    type: 'select',
    placeholder: '请选择起点站',
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleStartStationRemoteSearch,
    loading: startStationLoading.value,
    options: searchStations,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'station-select',
    teleported: false,
    appendToBody: false
  },
  {
    prop: 'endStationId',
    label: '终点站',
    type: 'select',
    placeholder: '请选择终点站',
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleEndStationRemoteSearch,
    loading: endStationLoading.value,
    options: searchStations,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'station-select',
    teleported: false,
    appendToBody: false
  }
])

// 表单验证规则
const formRules = computed(() => ({
  routeName: [
    { required: true, message: '请输入路线名称', trigger: 'blur' },
    { min: 2, max: 20, message: '路线名称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  startStationId: [
    { required: true, message: '请选择起点站点', trigger: 'change' }
  ],
  endStationId: [
    { required: true, message: '请选择终点站点', trigger: 'change' }
  ]
}))

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑线路' : '新增线路')

// 响应式数据
const routeList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 远程搜索相关响应式数据
const searchStations = ref([])
const startStationLoading = ref(false)
const endStationLoading = ref(false)
// 分页相关响应式数据
const startStationCurrentPage = ref(1)
const endStationCurrentPage = ref(1)
const startStationTotal = ref(0)
const endStationTotal = ref(0)
const startStationKeyword = ref('')
const endStationKeyword = ref('')
const startStationAllLoaded = ref(false)
const endStationAllLoaded = ref(false)
const isLoadingMore = ref(false)

/**
 * 起点站远程搜索方法
 * @param {string} query - 搜索关键词
 */
const handleStartStationRemoteSearch = async (query) => {
  // 重置状态
  startStationCurrentPage.value = 1
  startStationAllLoaded.value = false
  startStationKeyword.value = query

  if (!query) {
    searchStations.value = []
  }

  startStationLoading.value = true
  try {
    // 调用带分页的API获取站点列表
    const response = await getStationList({
      currentPage: 1,
      pageSize: 10,
      stationName: query
    })

    // 更新总数和站点列表
    startStationTotal.value = response.total
    searchStations.value = response.records.map(station => ({
      value: station.id,
      label: station.stationName,
      name: station.stationName
    }))
  } catch (error) {
    ElMessage.error('获取站点列表失败')
    searchStations.value = []
  } finally {
    startStationLoading.value = false
    isLoadingMore.value = false
    // 添加滚动监听
    addScrollListener('start')
  }
}

/**
 * 终点站远程搜索方法
 * @param {string} query - 搜索关键词
 */
const handleEndStationRemoteSearch = async (query) => {
  // 重置状态
  endStationCurrentPage.value = 1
  endStationAllLoaded.value = false
  endStationKeyword.value = query

  if (!query) {
    searchStations.value = []
  }

  endStationLoading.value = true
  try {
    // 调用带分页的API获取站点列表
    const response = await getStationList({
      currentPage: 1,
      pageSize: 10,
      stationName: query
    })

    // 更新总数和站点列表
    endStationTotal.value = response.total
    searchStations.value = response.records.map(station => ({
      value: station.id,
      label: station.stationName,
      name: station.stationName
    }))
  } catch (error) {
    ElMessage.error('获取站点列表失败')
    searchStations.value = []
  } finally {
    endStationLoading.value = false
    isLoadingMore.value = false
    // 添加滚动监听
    addScrollListener('end')
  }
}

/**
 * 加载更多站点数据
 * @param {string} type - 站点类型：'start' 或 'end'
 */
const loadMoreStations = async (type) => {
  // 避免重复加载
  if (isLoadingMore.value) return

  const isStartStation = type === 'start'
  const currentPage = isStartStation ? startStationCurrentPage.value : endStationCurrentPage.value
  const allLoaded = isStartStation ? startStationAllLoaded.value : endStationAllLoaded.value
  const keyword = isStartStation ? startStationKeyword.value : endStationKeyword.value

  // 如果已加载全部或无关键词，则不加载
  if (allLoaded || !keyword) return

  isLoadingMore.value = true
  try {
    // 调用API获取下一页数据
    const response = await getStationList({
      currentPage: currentPage + 1,
      pageSize: 10,
      stationName: keyword
    })

    // 检查是否还有更多数据
    const hasMore = searchStations.value.length + response.rows.length < (isStartStation ? startStationTotal.value : endStationTotal.value)

    // 更新状态
    if (isStartStation) {
      startStationCurrentPage.value++
      startStationAllLoaded.value = !hasMore
    } else {
      endStationCurrentPage.value++
      endStationAllLoaded.value = !hasMore
    }

    // 合并新数据到现有列表
    const newOptions = response.records.map(station => ({
      value: station.id,
      label: station.stationName,
      name: station.stationName
    }))

    // 避免重复项
    const existingValues = new Set(searchStations.value.map(item => item.value))
    const filteredNewOptions = newOptions.filter(item => !existingValues.has(item.value))

    searchStations.value = [...searchStations.value, ...filteredNewOptions]
  } catch (error) {
    ElMessage.error('加载更多站点失败')
  } finally {
    isLoadingMore.value = false
  }
}



// 为Select组件添加滚动事件监听的方法
const addScrollListener = (type) => {
  // 使用setTimeout确保DOM已更新
  setTimeout(() => {
    // 为所有下拉框添加滚动监听
    const dropdowns = document.querySelectorAll('.station-select .el-select-dropdown__wrap')
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
        loadMoreStations(type)
      }
    }
  }, 100)
}

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '线路号',
    width: 120,
    align: 'center'
  },
  {
    prop: 'routeName',
    label: '线路名',
    minWidth: 150,
    align: 'center'
  },
  {
    prop: 'startStationName',
    label: '起点站',
    minWidth: 150,
    align: 'center'
  },
  {
    prop: 'endStationName',
    label: '终点站',
    minWidth: 150,
    align: 'center'
  },
  {
    prop: 'stationCount',
    label: '站点数',
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
    width: 250,
    align: 'center',
    fixed: 'right'
  }
]

// 搜索字段配置
const searchFields = [
  {
    prop: 'routeName',
    label: '线路名',
    type: 'input',
    placeholder: '请输入线路名',
    clearable: true
  },
  {
    prop: 'startStation',
    label: '起点站',
    type: 'select',
    placeholder: '请选择起点站',
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleStartStationRemoteSearch,
    loading: startStationLoading.value,
    options: searchStations,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'station-select',
    // Element Plus的Select组件需要通过自定义指令或popper-class来添加滚动事件
    // 这里我们在选项列表渲染后通过nextTick添加滚动监听
    teleported: false,
    // 自定义下拉框的append方法
    appendToBody: false
  },
  {
    prop: 'endStation',
    label: '终点站',
    type: 'select',
    placeholder: '请选择终点站',
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleEndStationRemoteSearch,
    loading: endStationLoading.value,
    options: searchStations,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'station-select',
    teleported: false,
    appendToBody: false
  }
]

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})



/**
 * 获取站点列表（用于下拉选择）
 */
const fetchStations = async () => {
  try {
    // 使用getStationList API替代getAllStations，并添加分页参数
    const response = await getStationList({
      currentPage: 1,
      pageSize: 100 // 设置较大的分页以获取足够多的站点选项
    })
    stationOptions.value = response.records.map(station => ({
      value: station.id,
      label: station.stationName,
      address: station.address
    }))
  } catch (error) {
    ElMessage.error('获取站点列表失败')
  }
}

/**
 * 获取线路列表
 */
const fetchRouteList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }

    const response = await getRouteList(params)
    const data = response.records || []

    // 模拟分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    const paginatedData = data.slice(start, end)

    routeList.value = paginatedData
    total.value = data.length
  } catch (error) {
    routeList.value = []
    total.value = 0
    ElMessage.error('获取线路列表失败')
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
  fetchRouteList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  currentPage.value = 1
  selectedRows.value = []
  fetchRouteList({})
}



/**
 * 处理分页大小变化
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchRouteList()
}

/**
 * 处理分页切换
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchRouteList()
}

/**
 * 处理表格选择变化
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

/**
 * 新增线路
 */
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  routeForm.value = {
    id: '',
    routeName: '',
    startStationId: '',
    endStationId: ''
  }

  // 在弹窗打开时重置并添加滚动监听
  startStationCurrentPage.value = 1
  endStationCurrentPage.value = 1
  startStationAllLoaded.value = false
  endStationAllLoaded.value = false
}

/**
 * 批量删除线路
 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的线路')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个线路吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedRows.value.map(row => row.id)
    await deleteRoute(ids)
    ElMessage.success(`成功删除 ${selectedRows.value.length} 个线路`)

    fetchRouteList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 编辑线路
 */
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  routeForm.value = {
    id: row.id,
    routeName: row.routeName,
    startStationId: row.startStationId,
    endStationId: row.endStationId
  }

  // 在编辑弹窗打开时重置并添加滚动监听
  startStationCurrentPage.value = 1
  endStationCurrentPage.value = 1
  startStationAllLoaded.value = false
  endStationAllLoaded.value = false
}

/**
 * 管理线路站点
 */
const handleManageStations = (row) => {
  currentRouteId.value = row.id
  currentRouteName.value = row.routeName
  routeStationDialogVisible.value = true
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
      await updateRoute(submitData)
      ElMessage.success('更新线路成功')
    } else {
      submitData.createPerson = employeeInfo.value?.nickname || employeeInfo.value?.username
      await addRoute(submitData)
      ElMessage.success('新增线路成功')
    }

    dialogVisible.value = false
    fetchRouteList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除线路
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除线路「${row.routeName}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteRoute([row.id])
    ElMessage.success('删除成功')
    fetchRouteList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 处理线路站点管理弹窗关闭
 */
const handleRouteStationDialogClose = () => {
  routeStationDialogVisible.value = false
  currentRouteId.value = 0
  currentRouteName.value = ''
  fetchRouteList()
}

// 组件挂载后加载数据
onMounted(() => {
  fetchStations()
  fetchRouteList()
})
</script>

<style scoped lang="scss">
@use './Route-list-view.scss';
</style>