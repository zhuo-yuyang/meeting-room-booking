<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRoomList } from '../api'

const router = useRouter()
const rooms = ref([])
const keyword = ref('')

const filteredRooms = computed(() => {
  if (!keyword.value) return rooms.value
  return rooms.value.filter(room =>
      room.name.includes(keyword.value) ||
      room.location.includes(keyword.value)
  )
})

const fetchRooms = async () => {
  try {
    const res = await getRoomList()
    if (res.code === 200) {
      rooms.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取会议室列表失败')
  }
}

const goToBooking = (roomId) => {
  router.push({ path: '/booking', query: { roomId } })
}

onMounted(() => {
  fetchRooms()
})
</script>

<template>
  <div class="room-list">
    <h2>会议室列表</h2>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索会议室" style="width: 200px" clearable />
      <el-button type="primary" @click="fetchRooms">搜索</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="room in filteredRooms" :key="room.id" style="margin-bottom: 20px">
        <el-card class="room-card" @click="goToBooking(room.id)">
          <h3>{{ room.name }}</h3>
          <p>容量：{{ room.capacity }}人</p>
          <p>位置：{{ room.location }}</p>
          <p>设备：{{ room.equipment || '无' }}</p>
          <el-tag :type="room.status === 1 ? 'success' : 'danger'">
            {{ room.status === 1 ? '可用' : '禁用' }}
          </el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.room-list {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-bar {
  margin: 20px 0;
  display: flex;
  gap: 10px;
}

.room-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.room-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
</style>