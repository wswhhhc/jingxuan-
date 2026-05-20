<template>
  <div class="port-page">
    <el-card>
      <div class="toolbar">
        <el-select v-model="statusFilter" clearable placeholder="端口状态" style="width:130px" @change="loadList">
          <el-option label="空闲" value="free" />
          <el-option label="使用中" value="in_use" />
        </el-select>
        <el-button type="primary" @click="showAllocate = true">分配端口</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="portNumber" label="端口号" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'in_use' ? 'success' : 'info'" size="small">
              {{ row.status === 'in_use' ? '使用中' : '空闲' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="workTitle" label="关联作品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="proxyUrl" label="代理地址" width="200" show-overflow-tooltip />
        <el-table-column prop="allocatedTime" label="分配时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small" type="danger"
              :disabled="row.status !== 'in_use'"
              @click="handleRelease(row)"
            >释放</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="showAllocate" title="分配端口" width="450px" @open="loadAvailablePorts">
      <el-form :model="allocateForm" label-width="80px">
        <el-form-item label="作品ID">
          <el-input-number v-model="allocateForm.workId" :min="1" />
        </el-form-item>
        <el-form-item label="端口号">
          <el-select v-model="allocateForm.portNumber" placeholder="请选择空闲端口" style="width:100%">
            <el-option
              v-for="p in availablePorts"
              :key="p.portNumber"
              :label="`端口 ${p.portNumber}`"
              :value="p.portNumber"
            />
            <el-option v-if="!availablePorts.length" :value="0" label="暂无可用端口" disabled />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAllocate = false">取消</el-button>
        <el-button type="primary" @click="handleAllocate">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPortList, allocatePort, releasePort, getAvailablePorts } from '@/api/admin/port'
import type { PortItem } from '@/api/admin/port'

const loading = ref(false)
const list = ref<PortItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const statusFilter = ref('')
const showAllocate = ref(false)
const allocateForm = reactive({ workId: 1, portNumber: 0 })
const availablePorts = ref<PortItem[]>([])

const loadAvailablePorts = async () => {
  try {
    const res = await getAvailablePorts()
    availablePorts.value = (res.data as any) || []
    if (availablePorts.value.length > 0) {
      allocateForm.portNumber = availablePorts.value[0].portNumber
    }
  } catch {
    availablePorts.value = []
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getPortList({ page: page.value, size: size.value, status: statusFilter.value || undefined })
    list.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleAllocate = async () => {
  try {
    await allocatePort(allocateForm)
    ElMessage.success('分配成功')
    showAllocate.value = false
    loadList()
  } catch { /* mock */ }
}

const handleRelease = async (row: PortItem) => {
  try {
    await ElMessageBox.confirm('确认释放此端口？', '提示')
    await releasePort(row.id)
    ElMessage.success('已释放')
    loadList()
  } catch { /* */ }
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
