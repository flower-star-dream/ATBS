<template>
  <div class="layout-container">
    <el-container class="layout">
      <el-aside :width="sidebarCollapsed ? '64px' : '200px'" class="sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <img :src="logoUrl" alt="logo" />
            <span v-if="!sidebarCollapsed">火车订票系统</span>
          </div>
        </div>

        <el-menu
          :default-active="$route.path"
          class="sidebar-menu"
          :collapse="sidebarCollapsed"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          unique-opened
        >
          <!-- 菜单列表 - 支持三级菜单结构 -->
          <template v-for="item in menuList" :key="item.path">
            <!-- 一级菜单 - 带children的菜单项 -->
            <el-sub-menu
              v-if="item.children?.length && item.children.length > 0"
              :index="item.path"
            >
              <template #title>
                <el-icon>
                  <component :is="item.icon" />
                </el-icon>
                <span>{{ item.title }}</span>
              </template>
              
              <!-- 二级菜单 - 递归渲染 -->
              <template v-for="child in item.children" :key="child.path">
                <!-- 二级菜单 - 带children的菜单项(即三级菜单) -->
                <el-sub-menu
                  v-if="child.children?.length && child.children.length > 0"
                  :index="child.path"
                >
                  <template #title>
                    <el-icon v-if="child.icon">
                      <component :is="child.icon" />
                    </el-icon>
                    <span>{{ child.title }}</span>
                  </template>
                  
                  <!-- 三级菜单 - 最终菜单项 -->
                  <el-menu-item
                    v-for="grandchild in child.children"
                    :key="grandchild.path"
                    :index="grandchild.path"
                  >
                    <el-icon v-if="grandchild.icon">
                      <component :is="grandchild.icon" />
                    </el-icon>
                    <span>{{ grandchild.title }}</span>
                  </el-menu-item>
                </el-sub-menu>
                
                <!-- 二级菜单 - 无子菜单的菜单项 -->
                <el-menu-item
                  v-else
                  :index="child.path"
                >
                  <el-icon v-if="child.icon">
                    <component :is="child.icon" />
                  </el-icon>
                  <span>{{ child.title }}</span>
                </el-menu-item>
              </template>
            </el-sub-menu>
            
            <!-- 一级菜单 - 无子菜单的菜单项 -->
            <el-menu-item
              v-else
              :index="item.path"
            >
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-icon
              class="collapse-icon"
              @click="toggleSidebar"
            >
              <Expand v-if="sidebarCollapsed" />
              <Fold v-else />
            </el-icon>
          </div>

          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="employee-info">
                <el-avatar :size="32" :src="avatarUrl" />
                <span class="username">{{ employeeInfo?.nickname || employeeInfo?.username }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, defineAsyncComponent, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmployeeStore } from '@/stores/employee'
import { useAppStore } from '@/stores/app'
import { ossUrl } from '@/stores'
import { getEmployeeInfoService } from '@/api/employee'
import type { EmployeeInfo } from '@/types/employee'

const router = useRouter()
const employeeStore = useEmployeeStore()
const appStore = useAppStore()
const employeeInfo = computed(() => (employeeStore.employeeInfo || {}) as EmployeeInfo)
const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const menuList = computed(() => appStore.menuList)
import { loadAsset } from '@/utils/loadAsset';

const logoUrl = ref('');
const avatarUrl = ref('');

// 定义一个标志变量，用于标记是否是第一次加载
const isFirstLoad = ref(true);

const toggleSidebar = () => {
  appStore.toggleSidebar()
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'logout':
      handleLogout()
      break
    case 'profile':
      router.push('/employee/profile')
      break
    case 'resetPassword':
      router.push('/employee/resetPassword')
      break
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    employeeStore.logoutAction()
    router.push('/login')
    ElMessage.success('已退出登录')
  }).catch(() => {
    // 取消退出
  })
}

onMounted(async () => {
    // 确保employeeInfo和id都存在时才获取用户详情
    try {
      if (isFirstLoad.value) {
        let res = await getEmployeeInfoService()
        employeeStore.setEmployeeInfo(res)
        isFirstLoad.value = false
      }
    } catch (error) {
        console.error('获取员工信息失败:', error)
        ElMessage.error('获取员工信息失败，请重新登录')
    }
    logoUrl.value = await loadAsset(`${ossUrl}/assets/atbs/logo.png`, '../assets/atbs/logo.png');
    avatarUrl.value = await loadAsset(employeeInfo.value?.avatar, '../assets/atbs/Avatar.png');
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout {
  height: 100%;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #1f2d3d;
}

.logo {
  display: flex;
  align-items: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.logo img {
  width: 32px;
  height: 32px;
  margin-right: 8px;
}

.sidebar-menu {
  border-right: none;
  height: calc(100vh - 60px);
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  transition: color 0.3s;
}

.collapse-icon:hover {
  color: #409EFF;
}

.header-right {
  display: flex;
  align-items: center;
}

.employee-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.employee-info:hover {
  background-color: #f5f5f5;
}

.username {
  margin-left: 8px;
  margin-right: 4px;
}

.main {
  background-color: #f5f5f5;
  padding: 20px;
}
</style>