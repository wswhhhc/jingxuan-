<template>
  <div class="runtime-page">
    <el-card>
      <div class="toolbar">
        <div class="toolbar__meta">
          <h2>运行实例</h2>
          <p>查看作品在线体验的当前状态、端口分配和错误信息。</p>
        </div>
        <el-button @click="loadList">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="workId" label="作品ID" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="backendPort" label="后端端口" width="120" />
        <el-table-column prop="frontendPort" label="前端端口" width="120" />
        <el-table-column prop="previewUrl" label="预览地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="lastAccessTime" label="最近访问" width="180" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="handlePrepare(row.workId)">准备</el-button>
              <el-button size="small" type="danger" :disabled="row.status !== 'running'" @click="handleStop(row.workId)">
                停止
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRuntimeList, prepareRuntimeInstance, stopRuntimeInstance, type RuntimeAdminItem } from '@/api/admin/port'

const loading = ref(false)
const list = ref<RuntimeAdminItem[]>([])

function statusTagType(status: RuntimeAdminItem['status']) {
  if (status === 'running') return 'success'
  if (status === 'failed' || status === 'invalid') return 'danger'
  if (status === 'starting' || status === 'prepared') return 'warning'
  return 'info'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getRuntimeList()
    list.value = (res.data as RuntimeAdminItem[]) || []
  } finally {
    loading.value = false
  }
}

async function handlePrepare(workId: number) {
  try {
    await prepareRuntimeInstance(workId)
    ElMessage.success('准备完成')
    await loadList()
  } catch (error: any) {
    ElMessage.error(error?.message || '准备失败')
  }
}

async function handleStop(workId: number) {
  try {
    await ElMessageBox.confirm('确认停止该运行实例？', '提示')
    await stopRuntimeInstance(workId)
    ElMessage.success('实例已停止')
    await loadList()
  } catch {
    // ignore cancel and request failures here
  }
}

onMounted(loadList)
</script>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar__meta h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.toolbar__meta p {
  margin: 0;
  color: var(--text-muted, #6b7280);
}

.row-actions {
  display: flex;
  gap: 8px;
}
</style>
