<template>
  <div class="workspace-page log-page">
    <section class="workspace-section workspace-filter-panel reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">操作日志</h2>
          <p class="workspace-toolbar__desc">按操作类型查看后台审计线索，快速定位关键行为。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-select v-model="actionFilter" placeholder="操作类型" clearable @change="loadList">
          <el-option label="全部" value="" />
          <el-option label="登录" value="登录" />
          <el-option label="登出" value="登出" />
          <el-option label="新增" value="新增" />
          <el-option label="修改" value="修改" />
          <el-option label="删除" value="删除" />
          <el-option label="审核" value="审核" />
          <el-option label="发布" value="发布" />
          <el-option label="下线" value="下线" />
          <el-option label="评分" value="评分" />
          <el-option label="上传" value="上传" />
        </el-select>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="编号" width="68">
          <template #default="{ $index }">
            {{ getRowIndex($index) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="action" label="操作类型" width="100" />
        <el-table-column prop="target" label="操作对象" width="120" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column label="请求方法" width="90">
          <template #default="{ row }">
            <el-tag :type="methodTagType(row.requestMethod)" size="small">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestPath" label="请求路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">
            <span>{{ row.duration }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" />
      </el-table>

      <div class="workspace-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadList"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLogList } from '@/api/admin/log'
import type { LogItem } from '@/api/admin/log'

const loading = ref(false)
const list = ref<LogItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const actionFilter = ref('')

const methodTagType = (method: string) => {
  const map: Record<string, string> = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getLogList({
      page: page.value,
      size: size.value,
      action: actionFilter.value || undefined
    })
    list.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const getRowIndex = (index: number) => (page.value - 1) * size.value + index + 1

onMounted(loadList)
</script>

<style scoped>
.log-page { max-width: 1220px; margin: 0 auto; }
</style>
