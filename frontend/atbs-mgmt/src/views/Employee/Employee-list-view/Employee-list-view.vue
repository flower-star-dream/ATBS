<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage
    title="员工列表"
    :show-tabs="true"
    :tabs="tabs"
    :active-tab="statusFilter"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="employeeList"
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
      <el-button type="primary" @click="handleAdd">新增员工</el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        批量删除
        <span class="selected-count">({{ selectedRows.length }})</span>
      </el-button>
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
      <el-button type="primary" text size="small" @click="handleEdit(row)" :disabled="row.permissionLevel === '超级管理员'">编辑</el-button>
      <el-button type="primary" text size="small" @click="handleStatusChange(row)" :disabled="disabledStatusChange(row)">{{ row.status === EMPLOYEE_STATUS.DISABLED ? '启用' : '禁用' }}</el-button>
      <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
    </template>
  </ListPage>

  <!-- 使用通用表单弹窗组件 -->
  <DialogForm
    v-model:visible="dialogVisible"
    :title="dialogTitle"
    :form-data="employeeForm"
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
import { getEmployeeListService, updateEmployeeService, deleteEmployeeService, createEmployeeService, startOrStopService } from '@/api/employee'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmployeeStore } from '@/stores'
import DialogForm from '@/components/DialogForm/DialogForm.vue'
import { strToBase64 } from '@/utils/base64'
import { md5ToHex } from '@/utils/md5'
import { formatDate } from '@/utils/formatDate'

const employeeStore = useEmployeeStore()
const employeeInfo = computed(() => employeeStore.employeeInfo)

// 员工状态枚举常量
const EMPLOYEE_STATUS = {
  ENABLED: 1,
  DISABLED: 0,
  // 获取状态文本
  getText: (status) => {
    const statusMap = {
      [EMPLOYEE_STATUS.ENABLED]: '已启用',
      [EMPLOYEE_STATUS.DISABLED]: '已禁用'
    }
    return statusMap[status] || '未知状态'
  },
  // 获取状态标签类型
  getTagType: (status) => {
    const typeMap = {
      [EMPLOYEE_STATUS.ENABLED]: 'success',
      [EMPLOYEE_STATUS.DISABLED]: 'danger'
    }
    return typeMap[status] || 'info'
  }
}

// 表单相关响应式数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const employeeForm = ref({
  id: '',
  username: '',
  nickname: '',
  phone: '',
  password: '',
  permissionLevel: '',
  affiliatedSite: '',
  status: EMPLOYEE_STATUS.ENABLED
})

// 表单字段配置
const formFields = [
  {
    prop: 'username',
    label: '用户名',
    type: 'input',
    placeholder: '请输入用户名',
    maxlength: 20,
    showWordLimit: true,
    disabled: (isEditMode) => {
      return isEditMode 
    }
  },
  {
    prop: 'nickname',
    label: '昵称',
    type: 'input',
    placeholder: '请输入昵称',
    maxlength: 50,
    showWordLimit: true
  },
  {
    prop: 'phone',
    label: '手机号',
    type: 'input',
    placeholder: '请输入手机号',
    disabled: (isEditMode) => {
      // 只有管理员级别以上才能修改手机号
      return isEditMode && !(employeeInfo.value.permissionLevel === '超级管理员' || 
                           employeeInfo.value.permissionLevel === '管理员')
    }
  },
  {
    prop: 'password',
    label: '密码',
    type: 'input',
    inputType: 'password',
    placeholder: '请输入密码',
    // 只在新增时显示密码字段
    hidden: (isEditMode) => isEditMode
  },
  {
    prop: 'permissionLevel',
    label: '权限等级',
    type: 'select',
    placeholder: '请选择权限等级',
    options: [
      { label: '成员', value: '成员' },
      { label: '管理员', value: '管理员' }
    ],
    disabled: (isEditMode) => {
      // 只有超级管理员或管理员可以修改权限等级
      // 管理员不能编辑管理员，但可以将成员更改为管理员
      if (isEditMode) {
        if (employeeInfo.value.permissionLevel === '超级管理员') return false
        if (employeeInfo.value.permissionLevel === '管理员') {
          // 管理员只能编辑“成员”级别的权限，不能编辑同级的“管理员”
          return employeeForm.value.permissionLevel === '管理员'
        }
        return true
      }
      return false
    }
  },
  {
    prop: 'affiliatedSite',
    label: '所属站点',
    type: 'input',
    placeholder: '请输入所属站点',
    disabled: (isEditMode) => {
      // 只有超级管理员可以修改所属站点
      return isEditMode && employeeInfo.value.permissionLevel !== '超级管理员' && employeeInfo.value.permissionLevel !== '管理员'
    }
  },
  {
    prop: 'status',
    label: '状态',
    type: 'switch',
    activeValue: EMPLOYEE_STATUS.ENABLED,
    inactiveValue: EMPLOYEE_STATUS.DISABLED,
    activeText: '启用',
    inactiveText: '禁用',
    // 只在编辑时显示状态字段
    hidden: (isEditMode) => !isEditMode
  }
]

// 表单验证规则
const formRules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 50, message: '昵称长度不能超过 50 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: false, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur', validator: (rule, value, callback) => {
      // 密码字段只在新增时验证
      if (!isEdit.value && !value) {
        callback(new Error('请输入密码'))
      } else {
        callback()
      }
    }}
  ],
  permissionLevel: [
    { required: true, message: '请选择权限等级', trigger: 'change' }
  ],
  affiliatedSite: [
    { required: true, message: '请输入所属站点', trigger: 'blur' }
  ]
}))

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')

// 响应式数据
const employeeList = ref([])
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
    label: '已启用',
    count: statusCounts.value.enabled,
    lazy: true
  },
  {
    name: 'disabled',
    label: '已禁用',
    count: statusCounts.value.disabled,
    lazy: true
  }
])

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '管理员编号',
    width: 200,
    align: 'center'
  },
  {
    prop: 'username',
    label: '用户名',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'nickname',
    label: '昵称',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'phone',
    label: '手机号',
    minWidth: 120,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'affiliatedSite',
    label: '所属站点',
    minWidth: 120,
    align: 'center'
  },
  {
    prop: 'permissionLevel',
    label: '权限等级',
    minWidth: 120,
    align: 'center'
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
    width: 200,
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
  },
  {
    prop: 'permissionLevel',
    label: '权限等级',
    type: 'select',
    width: '200px',
    placeholder: '请选择权限等级',
    clearable: true,
    options: [
      { label: '普通成员', value: '成员' },
      { label: '管理员', value: '管理员' },
      { label: '超级管理员', value: '超级管理员' },
    ]
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
const fetchEmployeeList = async (searchParams = {}) => {
  loading.value = true
  try {

    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }
    
    // API调用
    const response = await getEmployeeListService(params)
    const data = response.records
    // 启用或禁用员工账号
    
    // 按状态筛选
    let filteredData = filterDataByStatus(data)
    employeeList.value = filteredData
    total.value = Number(response.total)
    
    // 更新状态数量统计
    updateStatusCounts(data)
  } catch (error) {
    employeeList.value = []
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
    return data.filter(item => item.status === EMPLOYEE_STATUS.ENABLED)
  }
  if (statusFilter.value === 'disabled') {
    return data.filter(item => item.status === EMPLOYEE_STATUS.DISABLED)
  }
  return data
}

/**
 * 更新状态数量统计
 * @param {Array} list - 用户列表数据
 */
const updateStatusCounts = (list) => {
  const enabledCount = list.filter(item => item.status === EMPLOYEE_STATUS.ENABLED).length
  const disabledCount = list.filter(item => item.status === EMPLOYEE_STATUS.DISABLED).length
  
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
  return EMPLOYEE_STATUS.getText(status)
}

/**
 * 获取状态标签类型
 * @param {number} status - 状态值
 * @returns {string} 标签类型
 */
const getStatusTagType = (status) => {
  return EMPLOYEE_STATUS.getTagType(status)
}

/**
 * 处理查询操作
 * @param {Object} formData - 搜索表单数据
 */
const handleSearch = (formData) => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchEmployeeList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  statusFilter.value = 'all' // 重置状态筛选
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchEmployeeList({}) // 重置后自动查询
}

/**
 * 处理状态标签页切换
 * @param {string} tabName - 切换到的标签页名称
 */
const handleStatusTabChange = (tabName) => {
  statusFilter.value = tabName // 更新状态筛选
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchEmployeeList()
}

/**
 * 处理表格选择变化
 * @param {Array} selection - 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

/**
 * 新增用户
 */
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  // 重置表单数据
  employeeForm.value = {
    id: '',
    username: '',
    nickname: '',
    phone: '',
    password: '',
    permissionLevel: '',
    affiliatedSite: '',
    status: EMPLOYEE_STATUS.ENABLED
  }
}

/**
 * 批量删除用户
 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的员工')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个员工吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const ids = selectedRows.value.map(row => row.id)
    await deleteEmployeeService(ids);
    ElMessage.success(`成功删除 ${selectedRows.value.length} 个员工`)
    
    // 删除成功后刷新列表
    fetchEmployeeList()
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
  fetchEmployeeList()
}

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchEmployeeList()
}

/**
 * 编辑用户
 * @param {Object} row - 用户数据
 */
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  // 复制数据到表单
  employeeForm.value = {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    phone: row.phone,
    password: '', // 编辑时密码为空
    permissionLevel: row.permissionLevel,
    affiliatedSite: row.affiliatedSite,
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
    
    // 如果是编辑且没有输入新密码，删除密码字段
    if (isEdit.value && !submitData.password) {
      delete submitData.password
    } else {
      submitData.password = md5ToHex(strToBase64(submitData.password))
    }
    if (formData.status === false) {
      submitData.status = EMPLOYEE_STATUS.DISABLED
    }
    
    
    if (isEdit.value) {
      // 编辑用户
      await updateEmployeeService(submitData)
      ElMessage.success('更新用户成功')
    } else {
      // 新增用户
      await createEmployeeService(submitData)
      ElMessage.success('新增用户成功')
    }
    
    // 关闭弹窗
    dialogVisible.value = false
    // 刷新列表
    fetchEmployeeList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 切换用户状态
 * @param {Object} row - 用户数据
 */
const handleStatusChange = async (row) => {
  const newStatus = row.status === EMPLOYEE_STATUS.ENABLED ? EMPLOYEE_STATUS.DISABLED : EMPLOYEE_STATUS.ENABLED
  const actionText = newStatus === EMPLOYEE_STATUS.ENABLED ? '启用' : '禁用'
  
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
    
    await startOrStopService(data)
    ElMessage.success(`${actionText}成功`)
    
    // 刷新列表
    fetchEmployeeList()
  } catch (error) {
    // 用户取消操作或发生错误
    if (error !== 'cancel') {
      ElMessage.error(`${actionText}失败`)
    }
  }
}

/**
 * 判断是否禁用状态切换
 * @param {Object} row - 用户数据
 * @returns {boolean} 是否禁用状态切换
 */
const disabledStatusChange = (row) => {
  // 获取用户权限级别
  const rowPermission = row.permissionLevel || row.role || row.permission || '';
  const currentEmployeePermission = employeeInfo.value?.permissionLevel || '';
  const currentEmployeeId = employeeInfo.value?.id || '';
  
  // 超级管理员的状态不能被任何人修改
  if (rowPermission === '超级管理员') {
    return true;
  }
  
  // 管理员和超级管理员可以修改其他用户的状态，但不能修改自己的状态
  if (currentEmployeePermission === '管理员' || currentEmployeePermission === '超级管理员') {
    return currentEmployeeId === row.id;
  }
  
  // 普通成员没有权限修改任何人的状态
  if (currentEmployeePermission === '成员') {
    return true;
  }
  
  // 其他情况默认禁用
  return true;
}

/**
 * 删除用户
 * @param {Object} row - 用户数据
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.username}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除接口
    await deleteEmployeeService([row.id])
    ElMessage.success('删除成功')
    // 删除成功后刷新列表
    fetchEmployeeList()
  } catch (error) {
    // 用户取消删除或发生错误
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 组件挂载后加载数据
onMounted(() => {
  fetchEmployeeList()
})
</script>

<style scoped lang="scss">
/* 使用ListPage组件的样式，无需额外样式 */
@use './Employee-list-view.scss';
</style>
