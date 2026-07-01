<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveBooking, getRoomList, getRoomBookings } from '../api'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const rooms = ref([])
const roomBookings = ref([])

const form = reactive({
  roomId: null,
  title: '',
  startTime: '',
  endTime: ''
})

const rules = {
  roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const userStr = sessionStorage.getItem('user')
const user = JSON.parse(userStr)

// 禁用过去的时间
const disabledStartDate = (time) => {
  return time.getTime() < Date.now() - 3600000
}

// 结束时间不能早于开始时间
const disabledEndDate = (time) => {
  if (!form.startTime) return false
  return time.getTime() <= new Date(form.startTime).getTime()
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  // 检查是否是有效日期
  if (isNaN(date.getTime())) return dateStr
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 获取会议室列表
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

// 获取会议室的预约记录
const fetchRoomBookings = async (roomId) => {
  try {
    const res = await getRoomBookings(roomId)
    if (res.code === 200) {
      roomBookings.value = res.data
      console.log(res.data)
    }
  } catch (error) {
    console.error('获取预约记录失败', error)
  }
}

// 监听会议室变化，加载预约记录
watch(() => form.roomId, (newVal) => {
  if (newVal) {
    fetchRoomBookings(newVal)
  }
})

// 提交预约
const submitBooking = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  // 验证结束时间晚于开始时间
  if (new Date(form.endTime) <= new Date(form.startTime)) {
    ElMessage.error('结束时间必须晚于开始时间')
    return
  }

  loading.value = true
  try {
    const res = await saveBooking(form)
    if (res.code === 200) {
      ElMessage.success(res.message)
      router.push('/my-bookings')
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('预约失败')
  } finally {
    loading.value = false
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  form.roomId = null
  form.title = ''
  form.startTime = ''
  form.endTime = ''
}



// 如果URL带roomId参数，自动选中
onMounted(() => {
  fetchRooms()
  if (route.query.roomId) {
    form.roomId = parseInt(route.query.roomId)
  }
})
</script>

<template>
  <div class="booking-form">
    <el-card>
      <template #header>
        <h2>预约会议室</h2>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="选择会议室" prop="roomId">
          <el-select v-model="form.roomId" placeholder="请选择会议室" style="width: 100%">
            <el-option
                v-for="room in rooms"
                :key="room.id"
                :label="`${room.name} (${room.location})`"
                :value="room.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="预约事由" prop="title">
          <el-input v-model="form.title" placeholder="请输入会议事由" />
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="form.startTime"
              type="datetime"
              placeholder="选择开始时间"
              :disabled-date="disabledStartDate"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="form.endTime"
              type="datetime"
              placeholder="选择结束时间"
              :disabled-date="disabledEndDate"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitBooking" :loading="loading">
            提交预约
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预约记录展示 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <h3>当前会议室的预约情况</h3>
      </template>
      <el-timeline>
        <el-timeline-item
            v-for="booking in roomBookings"
            :key="booking.id"
            :timestamp="formatDateTime(booking.startTime) + ' - ' + formatDateTime(booking.endTime)"
            :type="booking.status === 'pending' ? 'primary' : 'info'"
        >
          {{ booking.title || '无标题' }} - 预约人: {{ booking.username || '未知' }}
        </el-timeline-item>
        <el-empty v-if="roomBookings.length === 0" description="暂无预约记录" />
      </el-timeline>
    </el-card>
  </div>
</template>

<style scoped>
.booking-form {
  max-width: 800px;
  margin: 0 auto;
}
</style>