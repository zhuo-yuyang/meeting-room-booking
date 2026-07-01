<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import Nav from './components/Nav.vue'

const route = useRoute()
const isLoggedIn = ref(false)

// 判断当前是否在登录页或注册页
const isAuthPage = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

// 检查登录状态
const checkLoginStatus = () => {
  const token = sessionStorage.getItem('Authorization')
  isLoggedIn.value = !!token
}

// 登录成功回调
const handleLoginSuccess = () => {
  checkLoginStatus()
}

// 监听路由变化
const handleRouteChange = () => {
  // 路由变化时重新检查登录状态
  checkLoginStatus()
}

// 监听 storage 变化
const handleStorageChange = () => {
  checkLoginStatus()
}

onMounted(() => {
  checkLoginStatus()
  window.addEventListener('storage', handleStorageChange)
})

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange)
})

// 监听路由变化
import { watch } from 'vue'
watch(() => route.path, () => {
  checkLoginStatus()
})
</script>

<template>
  <div id="app">
    <!-- 只有登录后才显示导航栏，且不在登录页和注册页显示 -->
    <Nav v-if="isLoggedIn && !isAuthPage" />
    <div class="main-content" :class="{ 'full-width': !isLoggedIn || isAuthPage }">
      <router-view @login-success="handleLoginSuccess" />
    </div>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.main-content {
  padding: 20px;
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 未登录时内容占满全屏 */
.main-content.full-width {
  padding: 0;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>
