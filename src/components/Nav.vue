<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {Lock, Setting, SwitchButton} from "@element-plus/icons-vue";

const route = useRoute()
const router = useRouter()

const username = ref('')
const userRole = ref('')

// 获取用户信息
const getUserInfo = () => {
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      username.value = user.username || ''
      userRole.value = user.role || ''
    } catch (e) {
      console.error('解析用户信息失败', e)
    }
  }
}

const activeIndex = computed(() => route.path)

// 退出登录
const logout = () => {
  sessionStorage.removeItem('Authorization')
  sessionStorage.removeItem('user')
  ElMessage.success('已退出登录')
  // 刷新页面触发 App.vue 重新判断登录状态
  window.location.href = '/login'
}

const goToChangePassword = () => {
  router.push('/change-password')
}

onMounted(() => {
  getUserInfo()
})
</script>

<template>
  <el-menu :default-active="activeIndex" mode="horizontal" router>
    <!-- 普通用户菜单 -->
    <template v-if="userRole === 'USER'">
      <el-menu-item index="/rooms">会议室列表</el-menu-item>
      <el-menu-item index="/my-bookings">我的预约</el-menu-item>
    </template>

    <!-- 管理员菜单 -->
    <template v-if="userRole === 'ADMIN'">
      <el-menu-item index="/admin/rooms">会议室管理</el-menu-item>
      <el-menu-item index="/admin/bookings">预约管理</el-menu-item>
      <el-menu-item index="/admin/cleaning">保洁管理</el-menu-item>
      <el-menu-item index="/admin/users">用户管理</el-menu-item>
    </template>

    <div class="user-info">
      <span class="welcome">欢迎，{{ username }}</span>
      <el-dropdown trigger="click" style="margin-left: 15px">
        <el-button :icon="Setting" circle size="small" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goToChangePassword">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided @click="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-menu>
</template>

<style scoped>
.user-info {
  float: right;
  margin-right: 20px;
  line-height: 60px;
  display: flex;
  align-items: center;
}

.welcome {
  color: #333;
  font-size: 14px;
}
</style>