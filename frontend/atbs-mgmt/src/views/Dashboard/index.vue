<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1>仪表盘</h1>
      <p>欢迎来到火车订票系统员工管理</p>
    </div>

    <div class="dashboard-stats">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="40" color="#409EFF">
                  <User />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.employeeCount }}</div>
                <div class="stat-label">总员工数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="40" color="#67C23A">
                  <Train />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.trainCount }}</div>
                <div class="stat-label">车次总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="40" color="#E6A23C">
                  <Tickets />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.orderCount }}</div>
                <div class="stat-label">订单总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="40" color="#F56C6C">
                  <Location />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.stationCount }}</div>
                <div class="stat-label">站点总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="dashboard-charts">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>订单统计</span>
              </div>
            </template>
            <div class="chart-container">
              <p>订单统计图表将在这里显示</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>员工增长</span>
              </div>
            </template>
            <div class="chart-container">
              <p>用户增长图表将在这里显示</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'

const stats = reactive({
  employeeCount: 1234,
  trainCount: 567,
  orderCount: 8901,
  stationCount: 234
})

onMounted(() => {
  // 加载统计数据
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 30px;
}

.dashboard-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 10px 0;
  color: #303133;
}

.dashboard-header p {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.dashboard-stats {
  margin-bottom: 30px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  margin-right: 20px;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  font-size: 16px;
  font-weight: 500;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}
</style>