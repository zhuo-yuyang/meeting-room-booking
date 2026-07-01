<script setup>
import { ref, onMounted } from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import { getAllBookings, adminCancelBooking } from '../api'

const bookings = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = ref({
  roomName: '',
  username: ''
})

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getStatusType = (status) => {
  const map = { pending: 'primary', ongoing: 'success', ended: 'info', cancelled: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待开始', ongoing: '进行中', ended: '已结束', cancelled: '已取消' }
  return map[status] || status
}

const fetchData = async () => {
  try {
    const res = await getAllBookings(
        pageNum.value,
        pageSize.value,
        searchForm.value.roomName,
        searchForm.value.username
    )
    if (res.code === 200) {
      bookings.value = res.data.records || []
      total.value = res.data.total || 0
      console.log(res.data)
    }
  } catch (error) {
    ElMessage.error('获取预约记录失败')
  }
}

// ⭐ 管理员取消预约
const handleAdminCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '提示', {
      confirmButtonText: '确定取消',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await adminCancelBooking(id)
    if (res.code === 200) {
      ElMessage.success('取消预约成功')
      await fetchData()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const resetSearch = () => {
  searchForm.value.roomName = ''
  searchForm.value.username = ''
  pageNum.value = 1
  fetchData()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="admin-bookings">
    <h2>预约管理</h2>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
          v-model="searchForm.roomName"
          placeholder="会议室名称"
          style="width: 200px"
          clearable
          @clear="fetchData"
      />
      <el-input
          v-model="searchForm.username"
          placeholder="预约人"
          style="width: 200px; margin-left: 10px"
          clearable
          @clear="fetchData"
      />
      <el-button type="primary" @click="fetchData" style="margin-left: 10px">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="bookings" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roomName" label="会议室" />
      <el-table-column prop="username" label="预约人" />
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
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <!-- ⭐ 只有待开始的预约才能被管理员取消 -->
          <el-button
              v-if="row.status === 'pending'"
              type="danger"
              size="small"
              @click="handleAdminCancel(row.id)"
          >
            取消预约
          </el-button>
          <el-button
              v-else
              type="info"
              size="small"
              disabled
          >
            不可取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end"
    />
  </div>
</template>

<style scoped>
.admin-bookings {
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar {
  margin: 20px 0;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
</style>