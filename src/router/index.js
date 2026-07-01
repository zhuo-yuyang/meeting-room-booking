import { createRouter, createWebHistory } from 'vue-router'

import login from '../views/Login.vue'
import Register from '../views/Register.vue'
import RoomList from '../views/RoomList.vue'
import MyBookings from '../views/MyBookings.vue'
import BookingForm from '../views/BookingForm.vue'
import AdminRooms from '../views/AdminRooms.vue'
import AdminBookings from '../views/AdminBookings.vue'
import CleaningList from '../views/CleaningList.vue'
import ChangePassword from '../views/ChangePassword.vue'
import AdminUser from '../views/AdminUser.vue'
const routes = [
    {
        path: '/',
        redirect: '/rooms'
    },
    {
        path: '/login',
        name: 'Login',
        component: login
    },
    {
        path: '/register',
        name: 'Register',
        component: Register
    },
    {
        path: '/rooms',
        name: 'RoomList',
        component: RoomList,
        meta: { requiresAuth: true }
    },
    {
        path: '/my-bookings',
        name: 'MyBookings',
        component: MyBookings,
        meta: { requiresAuth: true }
    },
    {
        path: '/booking',
        name: 'BookingForm',
        component: BookingForm,
        meta: { requiresAuth: true }
    },
    {
        path: '/admin/rooms',
        name: 'AdminRooms',
        component: AdminRooms ,
        meta: { requiresAuth: true, role: 'admin' }
    },
    {
        path: '/admin/bookings',
        name: 'AdminBookings',
        component: AdminBookings,
        meta: { requiresAuth: true, role: 'admin' }
    },
    {
        path: '/admin/cleaning',
        name: 'CleaningList',
        component: CleaningList,
        meta: { requiresAuth: true, role: 'admin' }
    },
    { path: '/change-password',
        name: 'ChangePassword',
        component: ChangePassword,
        meta: { requiresAuth: true }
    },
    {
        path: '/admin/users',
        name: 'AdminUser',
        component: AdminUser,
        meta: { requiresAuth: true, role: 'admin' }
    }

]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 白名单（不需要登录的页面）
const whiteList = ['/login', '/register']

// 全局路由守卫
router.beforeEach((to,
                   from,
                   next) => {
    const token = sessionStorage.getItem('Authorization')
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')

    if (whiteList.includes(to.path)) {
        next()
        return
    }

    // 登录页和注册页直接放行
    if (to.path === '/login' || to.path === '/register') {
        // 如果已登录，跳转到会议室列表
        if (token && to.path === '/login') {
            next('/rooms')
        } else {
            next()
        }
        return
    }

    // 需要登录的页面
    if (to.meta.requiresAuth) {
        if (!token) {
            // 未登录，跳转到登录页
            // alert('请先登录')
            next('/login')
            return
        }
    }


    // 检查角色权限
    if (to.meta.role) {
        // ⭐ 关键修改：比较角色时统一转小写
        const userRole = user?.role?.toLowerCase()
        const requiredRole = to.meta.role.toLowerCase()

        console.log('角色检查:', '用户角色=', userRole, '需要角色=', requiredRole)

        if (userRole !== requiredRole) {
            alert('权限不足')
            next('/rooms')  // 跳转到首页，不是 /login
            return
        }
    }
    next()
})

export default router