import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const menuList = ref<MenuItem[]>([
    {
      path: '/home',
      title: '首页',
      icon: 'House'
    },
    // {
    //   path: '/dashboard',
    //   title: '仪表盘',
    //   icon: 'Odometer'
    // },
    {
      path: '/transport',
      title: '运力中心',
      icon: 'TransportIcon',
      children: [
        {
          path: '/schedule',
          title: '班次管理',
          icon: 'ScheduleIcon',
          children: [
            {
              path: '/schedule/list',
              title: '班次列表',
              icon: 'ScheduleIcon'
            }
          ]
        },
        {
          path: '/train',
          title: '列车管理',
          icon: 'TrainIcon',
          children: [
            {
              path: '/train/list',
              title: '列车列表',
              icon: 'TrainIcon'
            }
          ]
        },
        {
          path: '/seat-reservation',
          title: '座位预订管理',
          icon: 'SeatReservationIcon',
          children: [
            {
              path: '/seat-reservation/list',
              title: '座位预订列表',
              icon: 'SeatReservationIcon'
            }
          ]
        },
        {
          path: '/route',
          title: '线路管理',
          icon: 'RouteIcon',
          children: [
            {
              path: '/route/list',
              title: '线路列表',
              icon: 'RouteIcon'
            }
          ]
        },
        {
          path: '/station',
          title: '站点管理',
          icon: 'Location',
          children: [
            {
              path: '/station/list',
              title: '站点列表',
              icon: 'Location'
            }
          ]
        },
      ]
    },
    {
      path: '/orders',
      title: '订单中心',
      icon: 'OrderIcon',
      children: [
        {
          path: '/ticket',
          title: '票务管理',
          icon: 'Ticket',
          children: [
            {
              path: '/ticket/list',
              title: '车票列表',
              icon: 'Ticket'
            }
          ]
        },
        {
          path: '/order',
          title: '订单管理',
          icon: 'OrderIcon',
          children: [
            {
              path: '/order/list',
              title: '订单列表',
              icon: 'OrderIcon'
            }
          ]
        },
      ]
    },
    {
      path: '/person',
      title: '人员管理',
      icon: 'Avatar',
      children: [
        {
          path: '/employee',
          title: '员工管理',
          icon: 'EmployeeIcon',
          children: [
            {
              path: '/employee/list',
              title: '员工列表',
              icon: 'EmployeeIcon'
            },
            {
              path: '/employee/profile',
              title: '个人信息',
              icon: 'ProfileIcon'
            },
            {
              path: '/employee/resetPassword',
              title: '重置密码',
              icon: 'ResetPwdIcon'
            }
          ]
        },
        {
          path: '/user',
          title: '用户管理',
          icon: 'User',
          children: [
            {
              path: '/user/list',
              title: '用户列表',
              icon: 'User'
            }
          ]
        },
        {
          path: '/passenger',
          title: '乘车人管理',
          icon: 'PassengerIcon',
          children: [
            {
              path: '/passenger/list',
              title: '乘车人列表',
              icon: 'PassengerIcon'
            }
          ]
        },
      ]
    },
    // {
    //   path: '/system',
    //   title: '系统配置',
    //   icon: 'Setting'
    // }
  ])

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    sidebarCollapsed,
    menuList,
    toggleSidebar
  }
})

export default useAppStore