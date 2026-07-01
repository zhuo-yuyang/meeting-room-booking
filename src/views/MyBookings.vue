<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyBookings, cancelBooking as cancelBookingApi ,endBooking} from '../api'

const activeTab = ref('pending')
const bookings = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const bookingList = computed(() => {
  return Array.isArray(bookings.value) ? bookings.value : []
})

// ⭐ 状态映射函数
const getStatusText = (status) => {
  const statusMap = {
    'pending': '待开始',
    'ongoing': '进行中',
    'ended': '已结束',
    'cancelled': '已取消'
  }
  return statusMap[status] || status
}

// 按状态分类
const pendingBookings = computed(() =>
    bookings.value.filter(b => b.status === 'pending' && new Date(b.startTime) > new Date())
)
const ongoingBookings = computed(() =>
    bookings.value.filter(b => b.status === 'pending' && new Date(b.startTime) <= new Date() && new Date(b.endTime) > new Date())
)
const endedBookings = computed(() =>
    bookings.value.filter(b => b.status === 'ended' || new Date(b.startTime) <= new Date())
)
const cancelledBookings = computed(() =>
    bookings.value.filter(b => b.status === 'cancelled')
)

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const fetchBookings = async () => {
  try {
    const res = await getMyBookings(pageNum.value, pageSize.value)
    console.log('预约记录响应:', res)

    if (res.code === 200) {
      // ⭐ 处理分页数据
      if (res.data && typeof res.data === 'object') {
        // 如果是分页对象
        if (res.data.records && Array.isArray(res.data.records)) {
          bookings.value = res.data.records
          total.value = res.data.total || 0
        }
        // 如果是数组
        else if (Array.isArray(res.data)) {
          bookings.value = res.data
          total.value = res.data.length
        }
        // 其他情况
        else {
          bookings.value = []
          total.value = 0
        }
      } else {
        bookings.value = []
        total.value = 0
      }

      console.log('处理后的预约列表:', bookings.value)
    } else {
      bookings.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取预约记录失败:', error)
    ElMessage.error('获取预约记录失败')
    bookings.value = []
    total.value = 0
  }
}

const cancelBooking = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消这个预约吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await cancelBookingApi(id)
    if (res.code === 200) {
      ElMessage.success('取消成功')
      await fetchBookings()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// ⭐ 添加结束会议方法
const handleEndBooking = async (id) => {
  try {
    await ElMessageBox.confirm('确定要提前结束这个会议吗？', '提示', {
      confirmButtonText: '确定结束',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await endBooking(id)
    if (res.code === 200) {
      ElMessage.success('会议已提前结束')
      await fetchBookings()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchBookings()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchBookings()
}

onMounted(() => {
  fetchBookings()
})
</script>

<template>
  <div class="my-bookings">
    <h2>我的预约</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="未开始" name="pending">
        <el-table :data="pendingBookings" style="width: 100%">
          <el-table-column prop="roomName" label="会议室" />
          <el-table-column prop="title" label="事由" />
          <el-table-column label="开始时间">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="结束时间">
            <template #default="{ row }">
              {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button type="danger" size="small" @click="cancelBooking(row.id)">
                取消预约
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="进行中" name="ongoing">
        <el-table :data="ongoingBookings" border>
          <el-table-column prop="roomName" label="会议室" />
          <el-table-column prop="title" label="事由" />
          <el-table-column label="开始时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <el-tag type="success">进行中</el-tag>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button
                  type="primary"
                  size="small"
                  @click="handleEndBooking(row.id)"
              >
                结束会议
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="已结束" name="ended">
        <el-table :data="endedBookings" style="width: 100%">
          <el-table-column prop="roomName" label="会议室" />
          <el-table-column prop="title" label="事由" />
          <el-table-column label="开始时间">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="结束时间">
            <template #default="{ row }">
              {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status">
            <template #default="{ row }">
              <el-tag type="info">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="已取消" name="cancelled">
        <el-table :data="cancelledBookings" style="width: 100%">
          <el-table-column prop="roomName" label="会议室" />
          <el-table-column prop="title" label="事由" />
          <el-table-column label="开始时间">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="结束时间">
            <template #default="{ row }">
              {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

    </el-tabs>



    <el-empty v-if="bookings.length === 0" description="暂无预约记录" />
    <!-- 分页组件 -->
    <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[5, 10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end"
    />
  </div>
</template>

<style scoped>
.my-bookings {
  max-width: 1200px;
  margin: 0 auto;
}
</style>