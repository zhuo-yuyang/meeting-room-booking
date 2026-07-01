import http from '../utils/http.js'

// ========== 认证相关 ==========
export const register = (data) => http.post('/api/auth/register', data)
export const login = (data) => http.post('/api/auth/login', data)

// ========== 会议室相关 ==========
export const getRoomList = () => http.get('/api/room/list')
export const getRoomById = (id) => http.get(`/api/room/${id}`)
export const addRoom = (data) => http.post('/api/room/add', data)
export const updateRoom = (data) => http.put('/api/room/update', data)
export const deleteRoom = (id) => http.delete(`/api/room/${id}`)

// 预约相关
export const saveBooking = (data) => http.post('/api/booking/save', data)
export const getMyBookings = (pageNum, pageSize) =>
    http.get('/api/booking/my', { params: { pageNum, pageSize } })
export const cancelBooking = (id) => http.delete(`/api/booking/cancel/${id}`)
export const getAllBookings = (pageNum, pageSize, roomName, username) =>
    http.get('/api/booking/all', { params: { pageNum, pageSize, roomName, username } })

// 保洁相关
export const getCleaningList = (pageNum, pageSize, status) =>
    http.get('/api/cleaning/list', { params: { pageNum, pageSize, status } })
export const finishCleaning = (id) => http.put(`/api/cleaning/finish/${id}`)

// 获取会议室的预约记录
export const getRoomBookings = (roomId) => http.get(`/api/booking/room/${roomId}`)

// 用户相关
export const getUserInfo = () => http.get('/api/user/info')
export const updatePassword = (data) => http.put('/api/user/password', data)

// 用户管理
export const getUserList = (pageNum, pageSize, keyword) =>
    http.get('/api/admin/user/list', { params: { pageNum, pageSize, keyword } })

export const getUserById = (id) =>
    http.get(`/api/admin/user/${id}`)

export const addUser = (data) =>
    http.post('/api/admin/user/add', data)

export const updateUser = (data) =>
    http.put('/api/admin/user/update', data)

export const resetPassword = (id) =>
    http.put(`/api/admin/user/reset-password/${id}`)

export const deleteUser = (id) =>
    http.delete(`/api/admin/user/${id}`)

// 提前结束会议
export const endBooking = (id) => http.put(`/api/booking/end/${id}`)

// 管理员取消预约
export const adminCancelBooking = (id) => http.put(`/api/booking/admin/cancel/${id}`)