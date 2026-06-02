<template>
  <div class="preview-page" v-loading="loading">
    <div class="preview-shell">
      <div class="preview-meta">
        <h1>Online Preview</h1>
        <p v-if="statusMessage">{{ statusMessage }}</p>
      </div>

      <div v-if="runtimeUrl" class="preview-frame-wrap">
        <iframe :src="runtimeUrl" class="preview-frame" title="Work Preview" />
      </div>
      <el-empty v-else description="Preview is not ready yet" :image-size="72" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRuntimeStatus, heartbeatRuntime, stopRuntime } from '@/api/runtime'

const route = useRoute()
const loading = ref(false)
const statusMessage = ref('Preparing preview...')
const runtimeUrl = ref('')
let heartbeatTimer: number | null = null
let statusTimer: number | null = null

const workId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : ''
})

async function loadStatus() {
  if (!workId.value) return
  if (!runtimeUrl.value) {
    loading.value = true
  }
  try {
    const res = await getRuntimeStatus(workId.value)
    const data = res.data
    statusMessage.value = data.message || data.errorMessage || `Runtime status: ${data.status}`
    if (data.frontendPort) {
      runtimeUrl.value = `http://127.0.0.1:${data.frontendPort}`
      stopStatusPolling()
    }
  } catch (error: any) {
    statusMessage.value = error?.message || 'Failed to load runtime status'
    ElMessage.error(statusMessage.value)
  } finally {
    loading.value = false
  }
}

function startHeartbeat() {
  if (heartbeatTimer || !workId.value) return
  heartbeatTimer = window.setInterval(async () => {
    try {
      await heartbeatRuntime(workId.value)
    } catch {
      // ignore runtime heartbeat failures
    }
  }, 30000)
}

function startStatusPolling() {
  if (statusTimer || !workId.value || runtimeUrl.value) return
  statusTimer = window.setInterval(async () => {
    await loadStatus()
  }, 3000)
}

function stopStatusPolling() {
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
}

async function stopPreview() {
  if (!workId.value) return
  try {
    await stopRuntime(workId.value)
  } catch {
    // ignore best-effort stop failures
  }
}

function handleBeforeUnload() {
  stopPreview()
}

onMounted(async () => {
  await loadStatus()
  startStatusPolling()
  startHeartbeat()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  stopStatusPolling()
  window.removeEventListener('beforeunload', handleBeforeUnload)
  stopPreview()
})
</script>

<style scoped>
.preview-page {
  min-height: 100vh;
  padding: 24px;
  background: var(--page-bg, #f5f7fb);
}

.preview-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-meta h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.preview-meta p {
  margin: 0;
  color: var(--text-muted, #6b7280);
}

.preview-frame-wrap {
  min-height: calc(100vh - 140px);
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.preview-frame {
  width: 100%;
  min-height: calc(100vh - 140px);
  border: none;
}
</style>
