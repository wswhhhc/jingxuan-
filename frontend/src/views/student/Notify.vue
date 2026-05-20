<template>
  <div class="workspace-page notify-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">消息通知</h2>
          <p class="workspace-toolbar__desc">把审核结果、发布动态、评论回复与排行榜提醒整理成一份可持续阅读的消息册。</p>
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifyList, markAllRead, markAsRead } from '@/api/student/notify'
import type { NotifyItem } from '@/api/student/notify'

const loading = ref(false)
const list = ref<NotifyItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const unreadOnly = ref(false)

const emitNotifyChanged = () => {
  window.dispatchEvent(new CustomEvent('student-notify-changed'))
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getNotifyList({
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
      await markAsRead(item.id)
      item.isRead = 1
      emitNotifyChanged()
    } catch {
      /* ignore optimistic update failure */
    }
  }
}

const handleMarkAll = async () => {
  try {
    await markAllRead()
    list.value.forEach((item) => {
      item.isRead = 1
    })
    emitNotifyChanged()
    ElMessage.success('全部已读')
  } catch {
    /* ignore */
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
