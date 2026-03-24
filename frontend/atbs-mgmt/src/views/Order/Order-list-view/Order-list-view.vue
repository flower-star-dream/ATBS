<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage title="订单列表" :show-tabs="true" :tabs="tabs" :active-tab="statusFilter" :total="total"
    :current-page="currentPage" :page-size="pageSize" :selected-count="selectedRows.length" :loading="loading"
    :show-selection="true" :table-data="orderList" :table-columns="tableColumns" :search-fields="searchFields"
    :initial-search-form="initialSearchForm" @tab-click="handleStatusTabChange" @size-change="handleSizeChange"
    @current-change="handlePageChange" @search="handleSearch" @reset="handleReset"
    @selection-change="handleSelectionChange">
    <!-- 操作按钮区域 -->
    <template #actions>
      <!-- 订单列表暂不提供批量操作 -->
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
    <template #column-payTime="{ row }">
      {{ formatDate(row.payTime) }}
    </template>

    <!-- 自定义总价列 -->
    <template #column-totalPrice="{ row }">
      <span style="color: #f56c6c; font-weight: bold;">¥{{ row.totalPrice.toFixed(2) }}</span>
    </template>

    <!-- 自定义已付金额列 -->
    <template #column-amountPaid="{ row }">
      <span style="color: #f56c6c; font-weight: bold;">¥{{ row.amountPaid?.toFixed(2) || '0.00' }}</span>
    </template>

    <!-- 自定义操作列 -->
    <template #column-action="{ row }">
      <el-button type="primary" text size="small" @click="handleViewDetail(row)">查看详情</el-button>
      <template v-if="row.status === ORDER_STATUS.PENDING">
        <el-button type="primary" text size="small" @click="handleStatusChange(row, ORDER_STATUS.CANCELLED)">
          取消订单
        </el-button>
        <el-button type="success" text size="small" @click="handleStatusChange(row, ORDER_STATUS.PAID)">
          标记已支付
        </el-button>
      </template>
      <template v-else-if="row.status === ORDER_STATUS.PAID">
        <el-button type="success" text size="small" @click="handleStatusChange(row, ORDER_STATUS.TICKETED)">
          标记已出票
        </el-button>
        <el-button type="danger" text size="small" @click="handleStatusChange(row, ORDER_STATUS.REFUNDED)">
          退款
        </el-button>
      </template>
      <template v-else-if="row.status === ORDER_STATUS.TICKETED">
        <el-button type="success" text size="small" @click="handleStatusChange(row, ORDER_STATUS.COMPLETED)">
          标记已完成
        </el-button>
        <el-button type="danger" text size="small" @click="handleStatusChange(row, ORDER_STATUS.REFUNDED)">
          退款
        </el-button>
      </template>
      <template v-else-if="row.status === ORDER_STATUS.COMPLETED">
        <el-button type="primary" text size="small" disabled>
          已完成
        </el-button>
      </template>
      <template v-else-if="row.status === ORDER_STATUS.CANCELLED">
        <el-button type="primary" text size="small" disabled>
          已取消
        </el-button>
      </template>
      <template v-else-if="row.status === ORDER_STATUS.REFUNDED">
        <el-button type="primary" text size="small" disabled>
          已退款
        </el-button>
      </template>
    </template>
  </ListPage>

  <!-- 订单详情弹窗 -->
  <el-dialog v-model="detailDialogVisible" :title="'订单详情 - ' + currentOrder.order.id" width="800px"
    :close-on-click-modal="false">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单号">{{ currentOrder.order.id }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ currentOrder.order.username }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">
        <el-tag :type="getStatusTagType(currentOrder.order.status)" size="small">
          {{ getStatusText(currentOrder.order.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="订单总价">
        <span style="color: #f56c6c; font-weight: bold;">¥{{ currentOrder.order.totalPrice?.toFixed(2) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="已付金额">
        <span style="color: #f56c6c; font-weight: bold;">¥{{ currentOrder.order.amountPaid?.toFixed(2) || '0.00'
        }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="支付时间">{{ formatDate(currentOrder.order.payTime) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDate(currentOrder.order.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="订单备注" :span="2">{{ currentOrder.order.remarks || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />

    <h4>车票信息</h4>
    <el-table :data="currentOrder.tickets" border style="width: 100%">
      <el-table-column prop="trainNumber" label="车次号" width="100" />
      <el-table-column prop="departure" label="出发站" width="100" />
      <el-table-column prop="arrival" label="到达站" width="100" />
      <el-table-column prop="departureTime" label="出发时间" width="160" />
      <el-table-column prop="arrivalTime" label="到达时间" width="160" />
      <el-table-column prop="seatNumber" label="座位号" width="80" />
      <el-table-column prop="price" label="票价" width="80">
        <template #default="{ row }">
          ¥{{ row.price.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="车票状态" width="120">
        <template #default="{ row }">
          <el-tag :type="TicketStatusMap[row.status]?.type" size="small">
            {{ TicketStatusMap[row.status]?.text }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="passengerName" label="乘客姓名" width="100" />
    </el-table>

    <el-divider />

    <h4>乘客信息</h4>
    <el-table :data="currentOrder.passengers" border style="width: 100%">
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="cardType" label="证件类型" width="120" />
      <el-table-column prop="idCard" label="证件号码" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getOrderListService, getOrderDetailService, updateOrderStatusService } from '@/api/order'
import { getTicketDetailByOrderIdService } from '@/api/ticket'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/formatDate'
import { TicketStatus, TicketStatusMap } from '@/types/ticket'

// 订单状态枚举常量
const ORDER_STATUS = {
  PENDING: 0,
  PAID: 1,
  TICKETED: 2,
  COMPLETED: 3,
  CANCELLED: 4,
  REFUNDED: 5,
  // 获取状态文本
  getText: (status) => {
    const statusMap = {
      [ORDER_STATUS.PENDING]: '待支付',
      [ORDER_STATUS.PAID]: '已支付',
      [ORDER_STATUS.TICKETED]: '已出票',
      [ORDER_STATUS.COMPLETED]: '已完成',
      [ORDER_STATUS.CANCELLED]: '已取消',
      [ORDER_STATUS.REFUNDED]: '已退款'
    }
    return statusMap[status] || '未知状态'
  },
  // 获取状态标签类型
  getTagType: (status) => {
    const typeMap = {
      [ORDER_STATUS.PENDING]: 'warning',
      [ORDER_STATUS.PAID]: 'success',
      [ORDER_STATUS.TICKETED]: 'success',
      [ORDER_STATUS.COMPLETED]: 'success',
      [ORDER_STATUS.CANCELLED]: 'info',
      [ORDER_STATUS.REFUNDED]: 'info'
    }
    return typeMap[status] || 'info'
  }
}

// 响应式数据
const orderList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const statusFilter = ref('all')
const detailDialogVisible = ref(false)
const currentOrder = ref({
  order: {
    id: '',
  },
  tickets: [],
  passengers: []
})

// 状态数量统计
const statusCounts = ref({
  all: 0,
  pending: 0,
  paid: 0,
  ticketed: 0,
  completed: 0,
  cancelled: 0,
  refunded: 0
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
    name: 'pending',
    label: '待支付',
    count: statusCounts.value.pending,
    lazy: true
  },
  {
    name: 'paid',
    label: '已支付',
    count: statusCounts.value.paid,
    lazy: true
  },
  {
    name: 'ticketed',
    label: '已出票',
    count: statusCounts.value.ticketed,
    lazy: true
  },
  {
    name: 'completed',
    label: '已完成',
    count: statusCounts.value.completed,
    lazy: true
  },
  {
    name: 'cancelled',
    label: '已取消',
    count: statusCounts.value.cancelled,
    lazy: true
  },
  {
    name: 'refunded',
    label: '已退款',
    count: statusCounts.value.refunded,
    lazy: true
  }
])

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: '订单号',
    width: 200,
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
    prop: 'status',
    label: '订单状态',
    width: 100,
    align: 'center'
  },
  { prop: 'totalPrice', label: '订单总价', width: 120, align: 'center' }, 
  { prop: 'amountPaid', label: '已付金额', width: 120, align: 'center' }, 
  { prop: 'payTime', label: '支付时间', minWidth: 160, align: 'center' },
  {
    prop: 'remarks',
    label: '备注',
    minWidth: 150,
    align: 'center',
    showOverflowTooltip: true
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'createPerson',
    label: '创建人',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'updatePerson',
    label: '更新人',
    minWidth: 160,
    align: 'center'
  },
  {
    prop: 'action',
    label: '操作',
    width: 300,
    align: 'center',
    fixed: 'right'
  }
]

// 搜索字段配置
const searchFields = [
  {
    prop: 'id',
    label: '订单号',
    type: 'input',
    placeholder: '请输入订单号',
    clearable: true
  },
  {
    prop: 'userId',
    label: '用户ID',
    type: 'input',
    placeholder: '请输入用户ID',
    clearable: true
  },
  {
    prop: 'username',
    label: '用户名',
    type: 'input',
    placeholder: '请输入用户名',
    clearable: true
  }
]

// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = ''
  return acc
}, {})



/**
 * 获取订单列表
 * @param {Object} searchParams - 搜索参数
 */
const fetchOrderList = async (searchParams = {}) => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams
    }

    // API调用
    const response = await getOrderListService(params)
    const data = response.records

    // 按状态筛选
    let filteredData = filterDataByStatus(data)
    orderList.value = filteredData
    total.value = Number(response.total)

    // 更新状态数量统计
    updateStatusCounts(filteredData)
  } catch (error) {
    orderList.value = []
    total.value = 0

    // 重置状态统计
    statusCounts.value = {
      all: 0,
      pending: 0,
      paid: 0,
      cancelled: 0,
      refunded: 0,
      completed: 0
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
  if (statusFilter.value === 'pending') {
    return data.filter(item => item.status === ORDER_STATUS.PENDING)
  }
  if (statusFilter.value === 'paid') {
    return data.filter(item => item.status === ORDER_STATUS.PAID)
  }
  if (statusFilter.value === 'ticketed') {
    return data.filter(item => item.status === ORDER_STATUS.TICKETED)
  }
  if (statusFilter.value === 'completed') {
    return data.filter(item => item.status === ORDER_STATUS.COMPLETED)
  }
  if (statusFilter.value === 'cancelled') {
    return data.filter(item => item.status === ORDER_STATUS.CANCELLED)
  }
  if (statusFilter.value === 'refunded') {
    return data.filter(item => item.status === ORDER_STATUS.REFUNDED)
  }
  return data
}

/**
 * 更新状态数量统计
 * @param {Array} list - 订单列表数据
 */
const updateStatusCounts = (list) => {
  const pendingCount = list.filter(item => item.status === ORDER_STATUS.PENDING).length
  const paidCount = list.filter(item => item.status === ORDER_STATUS.PAID).length
  const ticketedCount = list.filter(item => item.status === ORDER_STATUS.TICKETED).length
  const completedCount = list.filter(item => item.status === ORDER_STATUS.COMPLETED).length
  const cancelledCount = list.filter(item => item.status === ORDER_STATUS.CANCELLED).length
  const refundedCount = list.filter(item => item.status === ORDER_STATUS.REFUNDED).length

  statusCounts.value = {
    all: list.length,
    pending: pendingCount,
    paid: paidCount,
    ticketed: ticketedCount,
    completed: completedCount,
    cancelled: cancelledCount,
    refunded: refundedCount
  }
}

/**
 * 获取状态文本
 * @param {number} status - 状态值
 * @returns {string} 状态文本
 */
const getStatusText = (status) => {
  return ORDER_STATUS.getText(status)
}

/**
 * 获取状态标签类型
 * @param {number} status - 状态值
 * @returns {string} 标签类型
 */
const getStatusTagType = (status) => {
  return ORDER_STATUS.getTagType(status)
}

/**
 * 处理查询操作
 * @param {Object} formData - 搜索表单数据
 */
const handleSearch = (formData) => {
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchOrderList(formData)
}

/**
 * 重置查询条件
 */
const handleReset = () => {
  statusFilter.value = 'all' // 重置状态筛选
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchOrderList({}) // 重置后自动查询
}

/**
 * 处理状态标签页切换
 * @param {string} tabName - 切换到的标签页名称
 */
const handleStatusTabChange = (tabName) => {
  statusFilter.value = tabName // 更新状态筛选
  currentPage.value = 1 // 重置为第一页
  selectedRows.value = [] // 清空选择
  fetchOrderList()
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
  fetchOrderList()
}

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchOrderList()
}

/**
 * 查看订单详情
 * @param {Object} row - 订单数据
 */
const handleViewDetail = async (row) => {
  if (!row) {
    ElMessage.warning('请选择订单查看详情')
    return
  }

  try {
    loading.value = true
    const detail = await getOrderDetailService(row.id)
    const ticketDetail = await getTicketDetailByOrderIdService(row.id)

    // 处理ticket数据，映射字段名
    const formattedTickets = Array.isArray(ticketDetail) ? ticketDetail.map(ticket => ({
      ...ticket,
      trainNumber: ticket.id, // 车次号使用id
      departure: ticket.startStation,
      arrival: ticket.endStation,
      departureTime: formatDate(ticket.startTime),
      arrivalTime: formatDate(ticket.endTime),
      price: ticket.money,
      status: ticket.status, // 车票状态
      passengerName: ticket.realName
      // 删除座位类型字段，因为列车没有这个字段
    })) : []

    currentOrder.value.order = detail
    currentOrder.value.tickets = formattedTickets
    currentOrder.value.passengers = Array.isArray(ticketDetail) ? ticketDetail.map(ticket => ({
      realName: ticket.realName,
      cardType: ticket.cardType,
      idCard: ticket.idCard
    })) : []

    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

/**
 * 修改订单状态
 * @param {Object} row - 订单数据
 * @param {number} newStatus - 新的状态值
 */
const handleStatusChange = async (row, newStatus) => {
  // 检查状态是否允许变更
  const allowChange = checkStatusChangeAllowed(row.status, newStatus)
  if (!allowChange) {
    ElMessage.warning('不允许的状态变更')
    return
  }

  try {
    // 根据新状态生成确认消息
    const confirmMsg = getConfirmMessage(row.id, newStatus)

    await ElMessageBox.confirm(
      confirmMsg,
      '操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const data = {
      id: row.id,
      status: newStatus,
      remarks: getRemarks(newStatus)
    }

    await updateOrderStatusService(data)
    ElMessage.success(getSuccessMessage(newStatus))

    // 刷新列表
    fetchOrderList()
  } catch (error) {
    // 用户取消操作或发生错误
    if (error !== 'cancel') {
      ElMessage.error('订单状态修改失败')
    }
  }
}

/**
 * 检查状态变更是否允许
 * @param {number} currentStatus - 当前状态
 * @param {number} newStatus - 新状态
 * @returns {boolean} 是否允许变更
 */
const checkStatusChangeAllowed = (currentStatus, newStatus) => {
  const allowedTransitions = {
    [ORDER_STATUS.PENDING]: [ORDER_STATUS.CANCELLED, ORDER_STATUS.PAID], // 待支付 -> 已取消/已支付
    [ORDER_STATUS.PAID]: [ORDER_STATUS.REFUNDED, ORDER_STATUS.TICKETED], // 已支付 -> 已退款/已出票
    [ORDER_STATUS.TICKETED]: [ORDER_STATUS.REFUNDED, ORDER_STATUS.COMPLETED] // 已出票 -> 已退款/已完成
  }

  return allowedTransitions[currentStatus]?.includes(newStatus) || false
}

/**
 * 获取确认消息
 * @param {string} orderId - 订单号
 * @param {number} newStatus - 新状态
 * @returns {string} 确认消息
 */
const getConfirmMessage = (orderId, newStatus) => {
  const messageMap = {
    [ORDER_STATUS.PAID]: `确定要将订单「${orderId}」标记为已支付吗？`,
    [ORDER_STATUS.TICKETED]: `确定要将订单「${orderId}」标记为已出票吗？`,
    [ORDER_STATUS.COMPLETED]: `确定要将订单「${orderId}」标记为已完成吗？`,
    [ORDER_STATUS.CANCELLED]: `确定要取消订单「${orderId}」吗？`,
    [ORDER_STATUS.REFUNDED]: `确定要将订单「${orderId}」退款吗？`
  }
  return messageMap[newStatus] || `确定要修改订单「${orderId}」的状态吗？`
}

/**
 * 获取操作备注
 * @param {number} newStatus - 新状态
 * @returns {string} 操作备注
 */
const getRemarks = (newStatus) => {
  const remarksMap = {
    [ORDER_STATUS.PAID]: '后台管理员标记已支付',
    [ORDER_STATUS.TICKETED]: '后台管理员标记已出票',
    [ORDER_STATUS.COMPLETED]: '后台管理员标记已完成',
    [ORDER_STATUS.CANCELLED]: '后台管理员取消订单',
    [ORDER_STATUS.REFUNDED]: '后台管理员发起退款'
  }
  return remarksMap[newStatus] || '后台管理员修改订单状态'
}

/**
 * 获取成功消息
 * @param {number} newStatus - 新状态
 * @returns {string} 成功消息
 */
const getSuccessMessage = (newStatus) => {
  const messageMap = {
    [ORDER_STATUS.PAID]: '订单标记已支付成功',
    [ORDER_STATUS.TICKETED]: '订单标记已出票成功',
    [ORDER_STATUS.COMPLETED]: '订单标记已完成成功',
    [ORDER_STATUS.CANCELLED]: '订单取消成功',
    [ORDER_STATUS.REFUNDED]: '订单退款成功'
  }
  return messageMap[newStatus] || '订单状态修改成功'
}

// 组件挂载后加载数据
onMounted(() => {
  fetchOrderList()
})
</script>

<style scoped lang="scss">
/* 使用ListPage组件的样式，无需额外样式 */
@use './Order-list-view.scss';

:deep(.el-dialog__body) {
  padding-top: 20px;
}

h4 {
  margin: 20px 0 10px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}
</style>