<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoomList, addRoom, updateRoom, deleteRoom } from '../api'

const rooms = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  name: '',
  capacity: 10,
  location: '',
  equipment: '',
  status: 1
})

const dialogTitle = computed(() => isEdit.value ? '编辑会议室' : '新增会议室')

const rules = {
  name: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }],
  location: [{ required: true, message: '请输入位置', trigger: 'blur' }]
}

const filteredRooms = computed(() => {
  if (!keyword.value) return rooms.value
  return rooms.value.filter(r => r.name.includes(keyword.value))
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

const showAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, name: '', capacity: 10, location: '', equipment: '', status: 1 }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  try {
    let res
    if (isEdit.value) {
      res = await updateRoom(form.value)
    } else {
      res = await addRoom(form.value)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      fetchRooms()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个会议室吗？', '提示', { type: 'warning' })
    const res = await deleteRoom(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchRooms()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchRooms()
})
</script>

<template>
  <div class="admin-rooms">
    <h2>会议室管理</h2>

    <div class="toolbar">
      <el-button type="primary" @click="showAddDialog">新增会议室</el-button>
      <el-input v-model="keyword" placeholder="搜索" style="width: 200px" clearable @clear="fetchRooms" />
      <el-button @click="fetchRooms">搜索</el-button>
    </div>

    <el-table :data="filteredRooms" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="会议室名称" />
      <el-table-column prop="capacity" label="容量" width="100" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="equipment" label="设备" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="会议室名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="设备" prop="equipment">
          <el-input v-model="form.equipment" placeholder="投影仪、白板..." />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-rooms {
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  margin: 20px 0;
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>