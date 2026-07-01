<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCleaningList, finishCleaning } from '../api'

const cleaningList = ref([])
const statusFilter = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const stats = ref({
  pending: 0,
  completed: 0,
  total: 0
})

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// ⭐ 单独获取统计数据（获取全部数据，用于统计）
const fetchStats = async () => {
  try {
    // 获取全部数据（不分页，只取总数）
    const res = await getCleaningList(1, 1000, null)
    if (res.code === 200) {
      const records = res.data.records || res.data || []
      stats.value.pending = records.filter(c => c.status === 0).length
      stats.value.completed = records.filter(c => c.status === 1).length
      stats.value.total = records.length
      console.log('统计数据:', stats.value)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchCleaningList = async () => {
  try {
    let status = null
    if (statusFilter.value === '0') {
      status = 0
    } else if (statusFilter.value === '1') {
      status = 1
    }
    // ⭐ 传递分页参数
    const res = await getCleaningList(pageNum.value, pageSize.value, status)

    if (res.code === 200) {
      // ⭐ 处理分页数据
      if (res.data && res.data.records) {
        cleaningList.value = res.data.records
        total.value = res.data.total
      } else if (Array.isArray(res.data)) {
        cleaningList.value = res.data
        total.value = res.data.length
        if(total.value === 0) {
          total.value = 1
        }
      } else {
        cleaningList.value = []
        total.value = 0
      }
    }
  } catch (error) {
    ElMessage.error('获取保洁列表失败')
  }
}

const handleFinish = async (id) => {
  try {
    const res = await finishCleaning(id)
    if (res.code === 200) {
      ElMessage.success('标记完成成功')
      // 延迟刷新，等待数据库更新
      setTimeout(() => {
        fetchCleaningList()
      }, 500)
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchCleaningList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchCleaningList()
}

const handleFilterChange = () => {
  pageNum.value = 1
  fetchCleaningList()
}

onMounted(() => {
  fetchStats()
  fetchCleaningList()
})
</script>

<template>
  <div class="cleaning-list">
    <h2>保洁管理</h2>

    <!-- 统计数据：基于全部数据，不随筛选变化 -->
    <div class="stats">
      <el-statistic title="待打扫" :value="stats.pending" />
      <el-statistic title="已完成" :value="stats.completed" />
      <el-statistic title="总计" :value="stats.total" />
    </div>

    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="handleFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="0">待打扫</el-radio-button>
        <el-radio-button value="1">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="cleaningList" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roomName" label="会议室" />
      <el-table-column label="预约时间">
        <template #default="{ row }">
          {{ formatDateTime(row.scheduledTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'warning' : 'success'">
            {{ row.status === 0 ? '待打扫' : '已完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button
              v-if="row.status === 0 && row.bookingStatus === 'ended'"
              type="primary"
              size="small"
              @click="handleFinish(row.id)"
          >
            标记完成
          </el-button>
          <!-- 如果预约未结束，显示提示 -->
          <el-tooltip v-else-if="row.status === 0 && row.bookingStatus !== 'ended'" content="会议尚未结束，暂不能打扫">
            <el-button type="info" size="small" disabled>标记完成</el-button>
          </el-tooltip>
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
.cleaning-list {
  max-width: 1200px;
  margin: 0 auto;
}

.stats {
  display: flex;
  gap: 40px;
  margin: 20px 0;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.toolbar {
  margin: 20px 0;
}
</style>