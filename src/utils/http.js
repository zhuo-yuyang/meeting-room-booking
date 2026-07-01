import axios from 'axios'

const http = axios.create({
    baseURL: '/api',
    timeout: 10000
})

// 请求拦截器：添加Token
http.interceptors.request.use(
    config => {
        const token = sessionStorage.getItem('Authorization')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：统一处理错误
http.interceptors.response.use(
    response => {
        return response.data
    },
    error => {
        if (error.response?.status === 401) {
            sessionStorage.removeItem('Authorization')
            sessionStorage.removeItem('user')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default http