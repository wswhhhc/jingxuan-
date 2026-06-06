<template>
  <div class="workspace-page notify-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">消息通知</h2>
          <p class="workspace-toolbar__desc">{{ description }}</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-checkbox v-model="unreadOnly" label="仅看未读" @change="loadList" />
          <el-button size="small" @click="handleMarkAll">全部已读</el-button>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div v-loading="loading">
        <div v-if="list.length > 0" class="workspace-collection">
          <article
            v-for="item in list"
            :key="item.id"
            class="notify-item"
            :class="{ unread: !item.isRead }"
            @click="handleRead(item)"
          >
            <div class="notify-header">
              <div class="notify-title-wrap">
                <span class="notify-title">{{ item.title }}</span>
                <span v-if="!item.isRead" class="unread-dot" />
              </div>
              <span class="notify-time">{{ item.createTime }}</span>
            </div>
            <div class="notify-content">{{ item.content }}</div>
          </article>
        </div>

        <div v-else-if="!loading" class="workspace-empty">
          <el-empty description="暂无通知" />
        </div>
      </div>

      <div class="workspace-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          small
          @change="loadList"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

interface NotifyApi {
  getNotifyList(params: { page?: number; size?: number; unreadOnly?: boolean }): Promise<any>
  markAsRead(id: number): Promise<any>
  markAllRead(): Promise<any>
}

const props = defineProps<{
  api: NotifyApi
  eventName: string
  description?: string
}>()

const loading = ref(false)
const list = ref<NotifyItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const unreadOnly = ref(false)

const emitNotifyChanged = () => {
  window.dispatchEvent(new CustomEvent(props.eventName))
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await props.api.getNotifyList({
      page: page.value,
      size: size.value,
      unreadOnly: unreadOnly.value || undefined,
    })
    list.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleRead = async (item: NotifyItem) => {
  if (!item.isRead) {
    try {
      await props.api.markAsRead(item.id)
      item.isRead = 1
      emitNotifyChanged()
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }
}

const handleMarkAll = async () => {
  try {
    await props.api.markAllRead()
    list.value.forEach((item) => { item.isRead = 1 })
    emitNotifyChanged()
    ElMessage.success('全部已读')
  } catch (e) {
    console.error('全部已读操作失败:', e)
    ElMessage.error('操作失败，请重试')
  }
}

onMounted(loadList)
</script>

<style scoped>
.notify-page {
  max-width: 1100px;
  margin: 0 auto;
}

.notify-item {
  padding: 18px 20px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
  cursor: pointer;
  transition:
    transform var(--transition-base),
    border-color var(--transition-fast),
    background-color var(--transition-fast);
}

.notify-item:hover {
  transform: translateY(-1px);
  border-color: var(--border-color);
}

.notify-item.unread {
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--brand-soft) 70%, transparent), transparent 30%),
    color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.notify-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notify-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.notify-title {
  font-weight: 600;
  font-size: 15px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--brand);
  flex-shrink: 0;
}

.notify-time {
  color: var(--text-muted);
  font-size: 12px;
}

.notify-content {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
}
</style>
