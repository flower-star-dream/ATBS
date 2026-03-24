<template>
  <!-- 使用增强的ListPage组件，通过配置实现动态列和搜索条件 -->
  <ListPage
    title="座位预订管理"
    :show-tabs="true"
    :tabs="tabs"
    :active-tab="statusFilter"
    :total="total"
    :current-page="currentPage"
    :page-size="pageSize"
    :selected-count="selectedRows.length"
    :loading="loading"
    :show-selection="true"
    :table-data="seatReservationList"
    :table-columns="tableColumns"
    :search-fields="searchFields"
    :initial-search-form="initialSearchForm"
    :pagination="true"
    :show-pagination="true"
    @tab-click="handleStatusTabChange"
    @size-change="handleSizeChange"
    @current-change="handlePageChange"
    @search="handleSearch"
    @reset="handleReset"
    @selection-change="handleSelectionChange"
  >
    <!-- 操作按钮区域 -->
    <template #actions>
      <el-button
        type="warning"
        :disabled="selectedRows.length === 0"
        @click="handleBatchStatusUpdate(1)"
      >
        批量设为已预订
        <span class="selected-count">({{ selectedRows.length }})</span>
      </el-button>
      <el-button
        type="primary"
        :disabled="selectedRows.length === 0"
        @click="handleBatchStatusUpdate(0)"
      >
        批量设为可预订
        <span class="selected-count">({{ selectedRows.length }})</span>
      </el-button>
    </template>

    <!-- 自定义班次信息列 -->
    <template #column-scheduleInfo="{ row }">
      <el-tag type="info" size="small">
        {{
          row.scheduleInfo ||
          `${row.trainName || "未知列车"} - ${row.routeName || "未知线路"}`
        }}
      </el-tag>
    </template>

    <!-- 自定义座位号列 -->
    <template #column-seatNum="{ row }">
      <span class="seat-number">{{ row.seatNum }}</span>
    </template>

    <!-- 自定义预订状态列 -->
    <template #column-bookingStatus="{ row }">
      <el-tag
        :type="BOOKING_STATUS_TYPES[row.bookingStatus] || 'info'"
        size="small"
        effect="dark"
      >
        {{ BOOKING_STATUS_LABELS[row.bookingStatus] || "未知状态" }}
      </el-tag>
    </template>

    <!-- 自定义创建时间列 -->
    <template #column-createTime="{ row }">
      {{ formatDate(row.createTime) }}
    </template>

    <template #column-updateTime="{ row }">
      {{ formatDate(row.updateTime) }}
    </template>

    <!-- 自定义创建人列 -->
    <template #column-createPerson="{ row }">
      <span class="person-name">{{ row.createPerson || "-" }}</span>
    </template>

    <!-- 自定义操作列 -->
    <template #column-action="{ row }">
      <el-button
        v-if="row.bookingStatus === 0"
        type="warning" 
        text
        size="small" 
        @click="handleStatusUpdate(row, 1)"
      >
        设为已预订
      </el-button>
      <el-button
        v-if="row.bookingStatus === 1"
        type="success"
        text
        size="small"
        @click="handleStatusUpdate(row, 0)"
      >
        设为可预订
      </el-button>
    </template>
  </ListPage>

  <!-- 移除了编辑和新增功能的表单弹窗组件 -->
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import {
  getSeatReservationList,
  batchUpdateSeatStatus,
} from "@/api/seat-reservation";
import { getScheduleList } from "@/api/schedule";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_TYPES,
} from "@/types/seat-reservation";
import { formatDate } from "@/utils/formatDate";

// 下拉框选项相关响应式数据 - 仅保留搜索所需的部分
const scheduleOptions = ref([]);
const scheduleLoading = ref(false);

// 简化的班次远程搜索方法 - 仅保留搜索所需功能
/**
 * 班次远程搜索方法
 * @param {string} query - 搜索关键词
 */
const handleScheduleRemoteSearch = async (query) => {
  scheduleLoading.value = true;
  try {
    // 调用API获取班次列表
    const response = await getScheduleList({
      currentPage: 1,
      pageSize: 20,
      keyword: query
    });
    
    // 更新班次列表
    scheduleOptions.value = (response.records || []).map(scheduler => ({
      value: scheduler.id,
      label: (scheduler.trainName || '未知列车') + ' - ' + (scheduler.routeName || '未知线路'),
    }));
  } catch (error) {
    ElMessage.error('获取班次列表失败');
    scheduleOptions.value = [];
  } finally {
    scheduleLoading.value = false;
  }
};

// 响应式数据
const seatReservationList = ref([]);
const loading = ref(false);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedRows = ref([]);
const statusFilter = ref('all');

// 状态数量统计
const statusCounts = ref({
  all: 0,
  available: 0,
  booked: 0
});

// 表格列配置
const tableColumns = [
  {
    prop: "id",
    label: "预订号",
    width: 100,
    align: "center",
  },
  {
    prop: "scheduleInfo",
    label: "班次信息",
    minWidth: 200,
    align: "center",
  },
  {
    prop: "seatNum",
    label: "座位号",
    width: 100,
    align: "center",
  },
  {
    prop: "bookingStatus",
    label: "预订状态",
    width: 120,
    align: "center",
  },
  {
    prop: "createTime",
    label: "创建时间",
    minWidth: 160,
    align: "center",
  },
  {
    prop: "updateTime",
    label: "更新时间",
    minWidth: 160,
    align: "center",
  },
  {
    prop: "createPerson",
    label: "创建人",
    minWidth: 120,
    align: "center",
  },
  {
    prop: "action",
    label: "操作",
    width: 250,
    align: "center",
    fixed: "right",
  },
];

// 搜索字段配置
const searchFields = [
  {
    prop: "scheduleId",
    label: "班次",
    type: "select",
    placeholder: "请选择班次",
    clearable: true,
    filterable: true,
    remote: true,
    remoteMethod: handleScheduleRemoteSearch,
    loading: scheduleLoading.value,
    options: scheduleOptions,
    // 添加滚动事件监听以支持加载更多
    popperClass: 'schedule-select',
    // Element Plus的Select组件需要通过自定义指令或popper-class来添加滚动事件
    // 这里我们在选项列表渲染后通过nextTick添加滚动监听
    teleported: false,
    appendToBody: false
  },
  {
    prop: "seatNum",
    label: "座位号",
    type: "input",
    placeholder: "请输入座位号",
    inputType: "number",
    clearable: true,
  },
];

// 标签页配置 - 计算属性，包含状态数量统计
const tabs = computed(() => [
  {
    name: 'all',
    label: '全部',
    count: statusCounts.value.all,
    lazy: true
  },
  {
    name: 'available',
    label: '可预订',
    count: statusCounts.value.available,
    lazy: true
  },
  {
    name: 'booked',
    label: '已预订',
    count: statusCounts.value.booked,
    lazy: true
  }
]);

// 搜索处理函数
const handleSearch = (formData) => {
  currentPage.value = 1; // 重置为第一页
  selectedRows.value = []; // 清空选择
  
  // 根据当前选中的标签页应用状态筛选
  fetchSeatReservationList(formData);
};

/**
 * 处理状态标签页切换
 * @param {string} tabName - 切换到的标签页名称
 */
const handleStatusTabChange = (tabName) => {
  statusFilter.value = tabName; // 更新状态筛选
  currentPage.value = 1; // 重置为第一页
  selectedRows.value = []; // 清空选择
  fetchSeatReservationList();
};


// 初始搜索表单数据
const initialSearchForm = searchFields.reduce((acc, field) => {
  acc[field.prop] = "";
  return acc;
}, {});


/**
 * 根据状态筛选数据
 * @param {Array} data - 原始数据
 * @returns {Array} 筛选后的数据
 */
const filterDataByStatus = (data) => {
  if (statusFilter.value === 'all') {
    return data;
  }
  // 根据筛选条件返回对应状态的数据
  if (statusFilter.value === 'available') {
    return data.filter(item => item.bookingStatus === 0);
  }
  if (statusFilter.value === 'booked') {
    return data.filter(item => item.bookingStatus === 1);
  }
  return data;
};

/**
 * 更新状态数量统计
 * @param {Array} list - 座位预订列表数据
 */
const updateStatusCounts = (list) => {
  const availableCount = list.filter(item => item.bookingStatus === 0).length;
  const bookedCount = list.filter(item => item.bookingStatus === 1).length;
  
  statusCounts.value = {
    all: list.length,
    available: availableCount,
    booked: bookedCount
  };
};

/**
 * 获取座位预订列表
 * @param {Object} searchParams - 搜索参数
 */
const fetchSeatReservationList = async (searchParams = {}) => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchParams,
    };

    // API调用
    const response = await getSeatReservationList(params);
    const data = response.records || [];

    // 按状态筛选
    let filteredData = filterDataByStatus(data);
    
    // 直接使用筛选后的数据，不再进行客户端分页
    // 这样每次切换分页时都会从服务器获取对应页码的数据
    seatReservationList.value = filteredData;
    total.value = Number(response.total);
    
    // 更新状态数量统计
    updateStatusCounts(data);
  } catch (error) {
    seatReservationList.value = [];
    total.value = 0;
    
    // 重置状态统计
    statusCounts.value = {
      all: 0,
      available: 0,
      booked: 0
    };
    ElMessage.error("获取座位预订列表失败");
  } finally {
    loading.value = false;
  }
};



/**
 * 重置查询条件
 */
const handleReset = () => {
  statusFilter.value = 'all'; // 重置状态筛选
  currentPage.value = 1; // 重置为第一页
  selectedRows.value = []; // 清空选择
  fetchSeatReservationList({}); // 重置后自动查询
};

/**
 * 处理表格选择变化
 * @param {Array} selection - 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedRows.value = selection;
};

/**
 * 分页大小变化
 * @param {number} size - 每页条数
 */
const handleSizeChange = (size) => {
  pageSize.value = size;
  fetchSeatReservationList();
};

/**
 * 分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  currentPage.value = page;
  fetchSeatReservationList();
};

/**
 * 批量更新座位状态
 * @param {number} status - 目标状态
 */
const handleBatchStatusUpdate = async (status) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning("请选择要更新的座位预订");
    return;
  }

  const statusLabel = BOOKING_STATUS_LABELS[status];

  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedRows.value.length} 个座位预订设为${statusLabel}吗？`,
      "状态更新确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    // 只处理0(可预订)和1(已预订)两种状态
    if (status !== 0 && status !== 1) {
      ElMessage.warning("不支持的批量状态更新");
      return;
    }
    
    const ids = selectedRows.value.map((row) => row.id);
    await batchUpdateSeatStatus({ ids, status });
    ElMessage.success(
      `成功更新 ${selectedRows.value.length} 个座位预订状态为${statusLabel}`
    );

    // 更新成功后刷新列表
    fetchSeatReservationList();
  } catch (error) {
    // 用户取消更新或发生错误
    if (error !== "cancel") {
      ElMessage.error("状态更新失败");
    }
  }
};

// 保留核心功能：列表展示、搜索筛选、状态更新

/**
 * 更新座位状态
 * @param {Object} row - 座位预订数据
 * @param {number} status - 新的状态
 */
const handleStatusUpdate = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      `确定要将座位预订「${row.id}」设为${BOOKING_STATUS_LABELS[status]}吗？`,
      "状态更新确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    // 只处理0(可预订)和1(已预订)两种状态
    if (status !== 0 && status !== 1) {
      ElMessage.warning("不支持的状态更新");
      return;
    }

    // 调用状态更新接口
    await batchUpdateSeatStatus({ ids: [row.id], status });
    ElMessage.success("状态更新成功");
    // 更新成功后刷新列表
    fetchSeatReservationList();
  } catch (error) {
    // 用户取消更新或发生错误
    if (error !== "cancel") {
      ElMessage.error("状态更新失败");
    }
  }
};

// 组件挂载后加载数据
onMounted(() => {
  fetchSeatReservationList();
});
</script>

<style scoped lang="scss">
/* 核心样式定义 */
.selected-count {
  margin-left: 4px;
  font-weight: bold;
}

.seat-number {
  font-weight: bold;
  color: #409eff;
}

.person-name {
  color: #606266;
  font-size: 12px;
}
</style>
