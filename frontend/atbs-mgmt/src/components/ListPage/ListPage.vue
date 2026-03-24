<template>
  <!-- 列表页面通用组件根节点 -->
  <div class="list-page">
    <!-- 页面标题（可选） -->
    <h2 v-if="title" class="page-title">
      <span class="title-icon"></span>{{ title }}
    </h2>

    <!-- 查询条件区域（动态生成或通过插槽实现） -->
    <div v-if="searchFields && searchFields.length > 0 || $slots.search" class="search-container">
      <!-- 动态生成的搜索条件 -->
      <el-form v-if="searchFields && searchFields.length > 0" :inline="true" :model="searchForm" class="demo-form-inline">
        <template v-for="field in searchFields" :key="field.prop">
          <el-form-item :label="field.label">
            <!-- 文本输入框 -->
            <el-input
              v-if="field.type === 'input'"
              v-model="searchForm[field.prop]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :clearable="field.clearable !== false"
              :disabled="field.disabled"
              :style="{ width: field.width || '200px' }"
            />
            <!-- 选择器 -->
            <el-select
              v-else-if="field.type === 'select'"
              v-model="searchForm[field.prop]"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :clearable="field.clearable || false"
              :disabled="field.disabled"
              :style="{ width: field.width || '100%' }"
              :filterable="field.filterable || false"
              :remote="field.remote || false"
              :remote-method="field.remoteMethod || null"
              :loading="field.loading || false"
              :remote-show-suffix="field.remote || false"
              @visible-change="(visible) => handleRemoteSelectVisibleChange(field, visible)"
            >
              <el-option
                v-for="option in (isRef(field.options) ? field.options.value : field.options)"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <!-- 日期选择器 -->
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="searchForm[field.prop]"
              :type="field.dateType || 'date'"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="field.disabled"
              :style="{ width: field.width || '200px' }"
            />
            <!-- 自定义渲染 -->
            <template v-else-if="$slots[`search-${field.prop}`]">
              <slot :name="`search-${field.prop}`" :field="field"></slot>
            </template>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <!-- 通过插槽自定义的搜索条件 -->
      <slot name="search" v-else></slot>
    </div>

    <!-- 标签页和操作按钮区域 -->
      <div class="tabs-actions-container">
        <!-- 状态切换标签页（可选） -->
        <el-tabs
          v-if="showTabs && validTabs && validTabs.length > 0"
          v-model="activeTab"
          @tab-click="handleTabClick"
          class="status-tabs"
        >
          <el-tab-pane
            v-for="tab in validTabs"
            :key="tab.name"
            :name="tab.name"
            :lazy="tab.lazy"
          >
            <template #label>
              <span>{{ tab.label }}</span>
              <span v-if="tab.count !== undefined" class="count-badge">{{ tab.count }}</span>
            </template>
          </el-tab-pane>
        </el-tabs>

      <!-- 按钮操作区域（通过插槽实现） -->
      <div v-if="$slots.actions" class="action-buttons">
        <slot name="actions"></slot>
      </div>
    </div>

    <!-- 表格区域（动态生成或通过插槽实现） -->
    <div class="table-container">
      <!-- 动态生成的表格 -->
      <el-table
        v-if="tableColumns && tableColumns.length > 0 && tableData && tableData.length > 0"
        :data="tableData"
        style="width: 100%"
        :loading="loading"
        border
        stripe
        :header-cell-style="{ 'font-weight': 'bold', 'background-color': '#f5f7fa' }"
        @selection-change="handleSelectionChange"
      >
        <!-- 多选列 -->
        <el-table-column
          v-if="showSelection"
          type="selection"
          width="55"
          align="center"
        />
        <!-- 动态生成的表格列 -->
        <template v-for="column in tableColumns" :key="column.prop">
          <!-- 自定义渲染的列 -->
          <el-table-column
            v-if="$slots[`column-${column.prop}`]"
            :prop="column.prop"
            :label="column.label"
            :width="column.width"
            :min-width="column.minWidth"
            :align="column.align"
            :fixed="column.fixed"
          >
            <template #default="{ row }">
              <slot :name="`column-${column.prop}`" :row="row" :column="column"></slot>
            </template>
          </el-table-column>
          <!-- 普通列 -->
          <el-table-column
            v-else
            :prop="column.prop"
            :label="column.label"
            :width="column.width"
            :min-width="column.minWidth"
            :align="column.align"
            :fixed="column.fixed"
            :show-overflow-tooltip="column.showOverflowTooltip"
          >
            <!-- 简单的格式化 -->
            <template v-if="column.formatter && !$slots[`column-${column.prop}`]" #default="{ row }">
              {{ formatCell(row, column) }}
            </template>
          </el-table-column>
        </template>
      </el-table>
      <!-- 数据为空时显示空状态 -->
      <el-empty
        v-else-if="tableColumns && tableColumns.length > 0 && (!tableData || tableData.length === 0)"
        description="暂无数据"
      />
      <!-- 通过插槽自定义的表格 -->
      <slot v-else></slot>
    </div>

    <!-- 分页组件（可选） -->
    <div v-if="showPagination" class="pagination-container">
      <div v-if="showSelectedCount" class="pagination-info">
        已选择 <span class="selected-count-text">{{ selectedCount }}</span> 项
      </div>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
  import { ref, reactive, watch, computed, isRef } from 'vue'

  // Props定义
  const props = defineProps({
  // 页面标题
  title: {
    type: String,
    default: ''
  },
  // 是否显示分页
  showPagination: {
    type: Boolean,
    default: true
  },
  // 是否显示选中数量
  showSelectedCount: {
    type: Boolean,
    default: true
  },
  // 是否显示标签页
  showTabs: {
    type: Boolean,
    default: false
  },
  // 标签页配置
  tabs: {
    type: Array,
    default: () => []
  },
  // 总数据量
  total: {
    type: Number,
    default: 0
  },
  // 当前页码
  currentPage: {
    type: Number,
    default: 1
  },
  // 每页条数
  pageSize: {
    type: Number,
    default: 10
  },
  // 可选的每页条数
  pageSizes: {
    type: Array,
    default: () => [5, 10, 20, 50, 100]
  },
  // 选中数量
  selectedCount: {
    type: Number,
    default: 0
  },
  // 默认激活的标签页
  activeTab: {
    type: String,
    default: ''
  },
  // 是否显示加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 是否显示多选列
  showSelection: {
    type: Boolean,
    default: false
  },
  // 表格数据
  tableData: {
    type: Array,
    default: () => []
  },
  // 表格列配置
  tableColumns: {
    type: Array,
    default: () => []
  },
  // 搜索字段配置
  // 对于select类型的字段，支持以下参数：
  // - filterable: 是否可搜索过滤选项，默认false
  // - remote: 是否启用远程搜索，默认false
  // - remoteMethod: 远程搜索方法，接收输入值作为参数
  // - loading: 加载状态，用于远程搜索时显示加载动画
  searchFields: {
    type: Array,
    default: () => []
  },
  // 初始搜索表单数据
  initialSearchForm: {
    type: Object,
    default: () => ({})
  }
})

// Emits定义
const emit = defineEmits([
  'size-change',
  'current-change',
  'tab-click',
  'search',
  'reset',
  'selection-change'
])

// 响应式数据
  const activeTab = ref(props.activeTab || (props.tabs && props.tabs.length > 0 ? props.tabs[0].name : ''))
  // 搜索表单数据
  const searchForm = reactive({ ...props.initialSearchForm })

  // 计算属性：获取有效的标签页
  const validTabs = computed(() => {
    return props.tabs.filter(tab => tab && typeof tab === 'object')
  })

// 监听初始搜索表单数据变化
watch(
  () => props.initialSearchForm,
  (newVal) => {
    Object.assign(searchForm, newVal)
  },
  { deep: true }
)

// 监听activeTab prop变化，确保组件状态与父组件保持同步
watch(
  () => props.activeTab,
  (newVal) => {
    if (newVal) {
      activeTab.value = newVal
    }
  },
  { immediate: true }
)

/**
 * 格式化表格单元格数据
 * @param {Object} row - 当前行数据
 * @param {Object} column - 列配置对象
 * @returns {*} 格式化后的数据
 */
const formatCell = (row, column) => {
  if (typeof column.formatter === 'function') {
    return column.formatter(row, column)
  }
  
  // 内置的简单格式化器
  if (column.formatter === 'date') {
    if (!row[column.prop]) return ''
    const date = new Date(row[column.prop])
    return date.toLocaleDateString()
  }
  
  if (column.formatter === 'datetime') {
    if (!row[column.prop]) return ''
    const date = new Date(row[column.prop])
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
  }
  
  return row[column.prop]
}

/**
 * 处理查询操作
 */
const handleSearch = () => {
  emit('search', { ...searchForm })
}

/**
 * 处理重置操作
 */
const handleReset = () => {
  // 重置搜索表单
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  // 恢复初始值
  Object.assign(searchForm, props.initialSearchForm)
  emit('reset')
}

/**
 * 处理分页大小变化
 * @param {number} size - 每页条数
 */
const handleSizeChange = (size) => {
  emit('size-change', size)
}

/**
 * 处理分页切换
 * @param {number} page - 当前页码
 */
const handlePageChange = (page) => {
  emit('current-change', page)
}

/**
   * 处理标签页点击
   * @param {Object} tab - 标签页对象
   */
  const handleTabClick = (tab) => {
    if (tab && tab.paneName) {
      activeTab.value = tab.paneName
      emit('tab-click', tab.paneName)
    }
  }

/**
 * 处理表格选择变化
 * @param {Array} selection - 选中的行数据
 */
const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

/**
 * 处理远程搜索下拉框的显示状态变化
 * 避免在组件内部直接修改props数据导致的循环引用问题
 * @param {Object} field - 字段配置对象
 * @param {boolean} visible - 是否显示下拉框
 */
const handleRemoteSelectVisibleChange = (field, visible) => {
  // 不自动触发远程搜索，避免循环引用问题
  // 让父组件自己控制远程搜索的触发时机
  // 这样可以避免当远程搜索方法更新选项时可能导致的循环引用
}
</script>

<style scoped lang="scss">
@use './ListPage.scss';
</style>