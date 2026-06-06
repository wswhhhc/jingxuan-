<template>
  <div class="notify-page">
    <el-card>
      <div class="toolbar">
        <span style="font-size:14px;color:#666">消息通知</span>
        <div>
          <el-checkbox v-model="unreadOnly" label="仅看未读" @change="loadList" />
          <el-button size="small" @click="handleMarkAll">全部已读</el-button>
        </div>
      </div>

      <div v-loading="loading">
        <div v-for="item in list" :key="item.id" class="notify-item" :class="{ unread: !item.isRead }" @click="handleRead(item)">
          <div class="notify-header">
            <span class="notify-title">{{ item.title }}</span>
            <span v-if="!item.isRead" class="unread-dot" />
            <span class="notify-time">{{ item.createTime }}</span>
          </div>
          <div class="notify-content">{{ item.content }}</div>
        </div>
        <el-empty v-if="!loading && list.length === 0" description="暂无通知" />
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          small
          @change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifyList, markAsRead, markAllRead } from '@/api/admin/notify'
import type { NotifyItem } from '@/api/admin/notify'

const loading = ref(false)
const list = ref<NotifyItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const unreadOnly = ref(false)

const emitNotifyChanged = () => {
  window.dispatchEvent(new CustomEvent('admin-notify-changed'))
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getNotifyList({
      page: page.value,
      size: size.value,
      unreadOnly: unreadOnly.value || undefined
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
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }
}

const handleMarkAll = async () => {
  try {
    await markAllRead()
    list.value.forEach(i => { i.isRead = 1 })
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
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.notify-item {
  padding: 14px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer;
  transition: background 0.2s;
}
.notify-item:hover { background: #fafafa; }
.notify-item.unread { background: #f0f7ff; }
.notify-header { display: flex; align-items: center; gap: 8px; }
.notify-title { font-weight: 500; font-size: 14px; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: #409eff; flex-shrink: 0; }
.notify-time { margin-left: auto; color: #999; font-size: 12px; }
.notify-content { color: #666; font-size: 13px; margin-top: 6px; line-height: 1.6; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
