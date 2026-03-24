import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/Home/Home.vue'),
        meta: { title: '首页', icon: 'Home' }
      },
      // {
      //   path: '/dashboard',
      //   name: 'Dashboard',
      //   component: () => import('@/views/Dashboard/index.vue'),
      //   meta: { title: '仪表盘', icon: 'Odometer' }
      // },
      {
        path: '/train',
        name: 'Train',
        meta: { title: '列车管理', icon: 'Train' },
        children: [
          {
            path: '/train/list',
            name: 'TrainList',
            component: () => import('@/views/Train/Train-list-view/Train-list-view.vue'),
            meta: { title: '列车列表' }
          }
        ]
      },
      {
        path: '/schedule',
        name: 'Schedule',
        meta: { title: '班次管理', icon: 'Calendar' },
        children: [
          {
            path: '/schedule/list',
            name: 'ScheduleList',
            component: () => import('@/views/Schedule/Schedule-list-view/Schedule-list-view.vue'),
            meta: { title: '班次列表' }
          }
        ]
      },
      {
        path: '/order',
        name: 'Order',
        meta: { title: '订单管理', icon: 'Tickets' },
        children: [
          {
            path: '/order/list',
            name: 'OrderList',
            component: () => import('@/views/Order/Order-list-view/Order-list-view.vue'),
            meta: { title: '订单列表' }
          }
        ]
      },
      {
        path: '/ticket',
        name: 'Ticket',
        meta: { title: '票务管理', icon: 'Tickets' },
        children: [
          {
            path: '/ticket/list',
            name: 'TicketList',
            component: () => import('@/views/Ticket/Ticket-list-view/Ticket-list-view.vue'),
            meta: { title: '车票列表' }
          }
        ]
      },
      {
        path: '/employee',
        name: 'Employee',
        meta: { title: '员工管理', icon: 'Employee' },
        children: [
          {
            path: '/employee/list',
            name: 'EmployeeList',
            component: () => import('@/views/Employee/Employee-list-view/Employee-list-view.vue'),
            meta: { title: '员工列表' }
          },
          {
            path: '/employee/profile',
            name: 'Profile',
            component: () => import('@/views/Employee/Employee-info-view/Employee-info-view.vue'),
            meta: { title: '个人信息'}
          },
          {
            path: '/employee/resetPassword',
            name: 'ResetPassword',
            component: () => import('@/views/Employee/Employee-resetPwd-view/Employee-resetPwd-view.vue'),
            meta: { title: '重置密码'}
          }
        ]
      },
      {
        path: '/user',
        name: 'User',
        meta: { title: '用户管理', icon: 'User' },
        children: [
          {
            path: '/user/list',
            name: 'UserList',
            component: () => import('@/views/User/User-list-view/User-list-view.vue'),
            meta: { title: '用户列表' }
          }
        ]
      },
      {
        path: '/passenger',
        name: 'Passenger',
        meta: { title: '乘车人管理', icon: 'User' },
        children: [
          {
            path: '/passenger/list',
            name: 'PassengerList',
            component: () => import('@/views/Passenger/Passenger-list-view/Passenger-list-view.vue'),
            meta: { title: '乘车人列表' }
          }
        ]
      },
      {
        path: '/seat-reservation',
        name: 'SeatReservation',
        meta: { title: '座位预订管理', icon: 'OfficeBuilding' },
        children: [
          {
            path: '/seat-reservation/list',
            name: 'SeatReservationList',
            component: () => import('@/views/SeatReservation/SeatReservation-list-view/SeatReservation-list-view.vue'),
            meta: { title: '座位预订列表' }
          }
        ]
      },
      {
        path: '/route',
        name: 'Route',
        meta: { title: '线路管理', icon: 'Connection' },
        children: [
          {
            path: '/route/list',
            name: 'RouteList',
            component: () => import('@/views/Route/Route-list-view/Route-list-view.vue'),
            meta: { title: '线路列表' }
          }
        ]
      },
      {
        path: '/station',
        name: 'Station',
        meta: { title: '站点管理', icon: 'Location' },
        children: [
          {
            path: '/station/list',
            name: 'StationList',
            component: () => import('@/views/Station/Station-list-view/Station-list-view.vue'),
            meta: { title: '站点列表' }
          }
        ]
      }
      
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || ''} - 火车订票系统后台`

  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router