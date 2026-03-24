<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`${routeName} - 线路站点管理`"
    width="80%"
    top="5vh"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="route-station-dialog">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAddStation" :icon="Plus">
          添加站点
        </el-button>
        <el-button type="success" @click="handleSaveSort" :icon="Check" :disabled="!hasSortChanged">
          保存排序
        </el-button>
        <el-button @click="handleRefresh" :icon="Refresh">
          刷新
        </el-button>
      </div>

      <!-- 站点列表 -->
      <div class="station-list-container">
        <el-table
          :data="routeStations"
          style="width: 100%"
          row-key="id"
          v-loading="loading"
          border
        >
          <el-table-column
            type="index"
            label="序号"
            width="80"
            align="center"
          />
          <el-table-column
            prop="stationName"
            label="站点名称"
            min-width="150"
          />
          <el-table-column
            prop="stationSorting"
            label="排序"
            width="100"
            align="center"
          />
          <el-table-column
            label="操作"
            width="200"
            align="center"
            fixed="right"
          >
            <template #default="{ row, $index }">
              <el-button
                type="primary"
                text
                size="small"
                @click="handleMoveUp($index)"
                :disabled="$index === 0"
              >
                上移
              </el-button>
              <el-button
                type="primary"
                text
                size="small"
                @click="handleMoveDown($index)"
                :disabled="$index === routeStations.length - 1"
              >
                下移
              </el-button>
              <el-button
                type="danger"
                text
                size="small"
                @click="handleRemoveStation(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 拖拽排序提示 -->
      <div class="sort-tip" v-if="hasSortChanged">
        <el-alert
          title="提示"
          type="warning"
          description="站点顺序已更改，请点击'保存排序'按钮保存更改"
          show-icon
          :closable="false"
        />
      </div>
    </div>

    <!-- 添加站点弹窗 -->
    <el-dialog
      v-model="addDialogVisible"
      title="添加站点"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="80px">
        <el-form-item label="选择站点" prop="stationId">
          <el-select
            v-model="addForm.stationId"
            placeholder="请选择要添加的站点"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="station in availableStations"
              :key="station.id"
              :label="station.stationName"
              :value="station.id"
            >
              <span>{{ station.stationName }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序位置" prop="position">
          <el-input-number
            v-model="addForm.position"
            :min="1"
            :max="routeStations.length + 1"
            controls-position="right"
            style="width: 100%"
          />
          <div class="form-tip">在当前位置插入站点，1表示最前面</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddSubmit" :loading="addLoading">
            添加
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, Refresh } from '@element-plus/icons-vue'
import { getRouteStationList, addRouteStation, deleteRouteStation, updateRouteStationSort } from '@/api/route-station'
import { getStationList } from '@/api/station'

// Props定义
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  routeId: {
    type: Number,
    required: true
  },
  routeName: {
    type: String,
    default: ''
  }
})

// Emits定义
const emit = defineEmits(['update:visible', 'close'])

// 响应式数据
const loading = ref(false)
const routeStations = ref([])
const originalStations = ref([]) // 保存原始顺序用于比较
const availableStations = ref([])
const addDialogVisible = ref(false)
const addLoading = ref(false)

const addForm = ref({
  stationId: '',
  position: 1
})

const addFormRef = ref(null)

// 添加表单验证规则
const addRules = {
  stationId: [
    { required: true, message: '请选择要添加的站点', trigger: 'change' }
  ],
  position: [
    { required: true, message: '请选择插入位置', trigger: 'blur' }
  ]
}

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 检查是否有排序变化
const hasSortChanged = computed(() => {
  if (routeStations.value.length !== originalStations.value.length) {
    return true
  }
  return routeStations.value.some((station, index) => 
    station.id !== originalStations.value[index]?.id
  )
})

/**
   * 获取线路站点列表
   */
const fetchRouteStations = async () => {
  if (!props.routeId) return
  
  loading.value = true
  try {
    const response = await getRouteStationList({
      routeId: props.routeId,
      currentPage: 1,
      pageSize: 100
    })
    // 按stationSorting字段升序排序
    const sortedRecords = response.records.sort((a, b) => a.stationSorting - b.stationSorting)
    routeStations.value = sortedRecords
    originalStations.value = JSON.parse(JSON.stringify(sortedRecords)) // 深拷贝保存原始顺序
  } catch (error) {
    ElMessage.error('获取线路站点列表失败')
    routeStations.value = []
    originalStations.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 获取所有可用站点
 */
const fetchAvailableStations = async () => {
  try {
    // 使用getStationList API替代getAllStations，并添加分页参数
    const response = await getStationList({
      currentPage: 1,
      pageSize: 100 // 设置较大的分页以获取足够多的站点选项
    })
    // 过滤掉已添加的站点
    const existingStationIds = routeStations.value.map(station => station.stationId)
    availableStations.value = response.records.filter(station => 
      !existingStationIds.includes(station.id)
    )
  } catch (error) {
    ElMessage.error('获取站点列表失败')
    availableStations.value = []
  }
}

/**
 * 处理添加站点
 */
const handleAddStation = () => {
  addForm.value = {
    stationId: '',
    position: routeStations.value.length + 1
  }
  fetchAvailableStations()
  addDialogVisible.value = true
}

/**
 * 处理添加站点提交
 */
const handleAddSubmit = async () => {
  if (!addFormRef.value) return
  
  try {
    await addFormRef.value.validate()
    addLoading.value = true
    
    const stationData = {
      routeId: props.routeId,
      stationId: addForm.value.stationId,
      stationSorting: addForm.value.position
    }
    
    await addRouteStation(stationData)
    ElMessage.success('添加站点成功')
    
    addDialogVisible.value = false
    fetchRouteStations()
  } catch (error) {
    if (error !== false) { // 表单验证错误时不显示错误消息
      ElMessage.error('添加站点失败')
    }
  } finally {
    addLoading.value = false
  }
}

/**
 * 处理移除站点
 */
const handleRemoveStation = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要从线路中移除站点「${row.stationName}」吗？`,
      '移除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteRouteStation([row.id])
    ElMessage.success('移除站点成功')
    fetchRouteStations()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除站点失败')
    }
  }
}

/**
 * 上移站点
 */
const handleMoveUp = (index) => {
  if (index === 0) return
  
  const temp = routeStations.value[index]
  routeStations.value[index] = routeStations.value[index - 1]
  routeStations.value[index - 1] = temp
  
  // 更新排序值
  updateSortingValues()
}

/**
 * 下移站点
 */
const handleMoveDown = (index) => {
  if (index === routeStations.value.length - 1) return
  
  const temp = routeStations.value[index]
  routeStations.value[index] = routeStations.value[index + 1]
  routeStations.value[index + 1] = temp
  
  // 更新排序值
  updateSortingValues()
}

/**
 * 更新排序值
 */
const updateSortingValues = () => {
  routeStations.value.forEach((station, index) => {
    station.stationSorting = index + 1
  })
}

/**
 * 保存排序
 */
const handleSaveSort = async () => {
  try {
    loading.value = true
    
    // 确保routeStations数组存在且不为空
    if (!routeStations.value || routeStations.value.length === 0) {
      ElMessage.warning('没有站点数据可排序')
      return
    }

    const routeStationsIds = routeStations.value.map(station => station.id)
    
    const sortData = {
      routeId: props.routeId,
      routeStationsIds: routeStationsIds
    }
    
    await updateRouteStationSort(sortData)
    ElMessage.success('保存排序成功')
    
    // 更新原始顺序
    originalStations.value = JSON.parse(JSON.stringify(routeStations.value))
  } catch (error) {
    ElMessage.error('保存排序失败')
    console.error('保存排序失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 刷新数据
 */
const handleRefresh = () => {
  fetchRouteStations()
}

/**
 * 处理关闭弹窗
 */
const handleClose = () => {
  emit('close')
  // 重置数据
  routeStations.value = []
  originalStations.value = []
  availableStations.value = []
}

// 监听routeId变化
watch(() => props.routeId, (newRouteId) => {
  if (newRouteId && props.visible) {
    fetchRouteStations()
  }
})

// 监听visible变化
watch(() => props.visible, (visible) => {
  if (visible && props.routeId) {
    fetchRouteStations()
  }
})

// 组件挂载
onMounted(() => {
  if (props.visible && props.routeId) {
    fetchRouteStations()
  }
})
</script>

<style scoped lang="scss">
.route-station-dialog {
  .toolbar {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
  }

  .station-list-container {
    margin-bottom: 20px;
  }

  .sort-tip {
    margin-top: 20px;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
  }
}
</style>