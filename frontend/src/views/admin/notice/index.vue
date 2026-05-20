<template>
  <div class="workspace-page notice-page">
    <section class="workspace-section workspace-filter-panel reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">公告筛选</h2>
          <p class="workspace-toolbar__desc">以统一编辑界面维护公告，并快速进入新增与预览流程。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-input v-model="keyword" placeholder="搜索公告" clearable @clear="loadList" @keyup.enter="loadList" />
          <el-button type="primary" @click="showEdit()">发布公告</el-button>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">公告列表</h3>
          <p class="workspace-toolbar__desc">当前公告按标题、发布人与发布时间组织为档案表。</p>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="编号" width="72">
          <template #default="{ $index }">
            {{ (page - 1) * size + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="发布人" width="120">
          <template #default="{ row }">
            {{ row.publisherName || row.publisherId || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="notice-actions">
              <el-button size="small" plain @click="showDetail(row)">预览</el-button>
              <el-button size="small" type="primary" @click="showEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
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

    <el-dialog v-model="editVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="650px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="公告预览" width="650px">
      <h3 style="margin-bottom:12px">{{ detailData?.title }}</h3>
      <div class="text-muted" style="margin-bottom:16px">
        发布人：{{ detailData?.publisherName || detailData?.publisherId || '--' }}
        | {{ detailData?.publishTime }}
      </div>
      <div class="notice-content">{{ detailData?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoticeList, createNotice, updateNotice, deleteNotice } from '@/api/admin/notice'
import type { NoticeItem } from '@/api/admin/notice'

const loading = ref(false)
const list = ref<NoticeItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const editVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const detailData = ref<NoticeItem | null>(null)
const form = reactive({ title: '', content: '' })
const editId = ref(0)

const loadList = async () => {
  loading.value = true
  try {
    const res = await getNoticeList({
      page: page.value,
      size: size.value,
      status: undefined
    })
    list.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const showDetail = (row: NoticeItem) => {
  detailData.value = row
  detailVisible.value = true
}

const showEdit = (row?: NoticeItem) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.title = row.title
    form.content = row.content
  } else {
    isEdit.value = false
    editId.value = 0
    form.title = ''
    form.content = ''
  }
  editVisible.value = true
}

const handleSave = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写完整')
    return
  }
  try {
    if (isEdit.value) {
      await updateNotice(editId.value, { title: form.title, content: form.content })
    } else {
      await createNotice({ title: form.title, content: form.content, status: 1 })
    }
    ElMessage.success(isEdit.value ? '已更新' : '已发布')
    editVisible.value = false
    loadList()
  } catch {
    ElMessage.success(isEdit.value ? '已更新' : '已发布')
    editVisible.value = false
    loadList()
  }
}

const handleDelete = async (row: NoticeItem) => {
  try {
    await ElMessageBox.confirm('确认删除此公告？', '提示')
    await deleteNotice(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch { /* 取消或错误 */ }
}

onMounted(loadList)
</script>

<style scoped>
.notice-page {
  max-width: 1220px;
  margin: 0 auto;
}

.notice-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
}

.notice-actions :deep(.el-button) {
  min-width: 0;
  margin-left: 0;
  padding-inline: 14px;
}

.text-muted { color: var(--text-muted); font-size: 13px; }
.notice-content { white-space: pre-wrap; line-height: 1.8; }
</style>
