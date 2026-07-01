<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const username = computed(() => {
  const user = JSON.parse(sessionStorage.getItem('user') || '{}')
  return user.username || '未登录'
})

const handleCommand = (command) => {
  if (command === 'logout') {
    sessionStorage.removeItem('Authorization')
    sessionStorage.removeItem('user')
    router.push('/login')
  }
}
</script>

<template>
  <el-dropdown @command="handleCommand">
    <span class="user-name">
      {{ username }}
      <el-icon><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="logout">注销</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>