<template>
  <div class="score-batch-page">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>评分批次列表</span>
          <el-button type="primary" @click="openDialog()">新建批次</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="batchName" label="批次名称" min-width="160" />
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ row.startTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">{{ row.endTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column prop="classScopes" label="适用范围" width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="batch-actions">
              <el-button size="small" @click="openDialog(row)">编辑</el-button>
              <el-button size="small" plain @click="viewDetail(row)">详情</el-button>
              <el-button size="small" type="primary" @click="viewScores(row)">评分明细</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑批次' : '新建批次'" width="550px" destroy-on-close>
      <el-form :model="form" label-width="100px" ref="formRef" :rules="rules">
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="form.batchName" placeholder="请输入批次名称" />
        </el-form-item>
        <el-form-item label="时间范围" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="适用范围" prop="classScopes">
          <el-input v-model="form.classScopes" placeholder="班级范围，多个以逗号分隔" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="批次详情" width="500px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-grid">
          <div class="detail-item"><label>批次名称</label><span>{{ detail.batchName }}</span></div>
          <div class="detail-item"><label>开始时间</label><span>{{ detail.startTime?.replace('T', ' ') }}</span></div>
          <div class="detail-item"><label>结束时间</label><span>{{ detail.endTime?.replace('T', ' ') }}</span></div>
          <div class="detail-item"><label>适用范围</label><span>{{ detail.classScopes || '-' }}</span></div>
          <div class="detail-item"><label>状态</label><span>
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small">
              {{ detail.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </span></div>
          <div class="detail-item"><label>创建时间</label><span>{{ detail.createTime?.replace('T', ' ') }}</span></div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="scoreVisible" title="评分明细" width="900px" destroy-on-close top="5vh">
      <template v-if="scoreBatchId">
        <div style="margin-bottom:12px">
          <el-button type="success" size="small" @click="publishRanking(scoreBatchId)" :disabled="rankingPublished">
            {{ rankingPublished ? '已公示' : '公示排行榜' }}
          </el-button>
          <el-button size="small" @click="unpublishRanking(scoreBatchId)" :disabled="!rankingPublished">
            取消公示
          </el-button>
        </div>
        <el-table :data="scoreWorks" v-loading="scoreLoading" stripe style="width:100%">
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.scores" stripe size="small" v-if="row.scores?.length">
                <el-table-column prop="teacherName" label="评分教师" width="100" />
                <el-table-column prop="innovation" label="创新性(25)" width="100" />
                <el-table-column prop="difficulty" label="难度(25)" width="100" />
                <el-table-column prop="completion" label="完成度(30)" width="100" />
                <el-table-column prop="practicality" label="实用性(20)" width="100" />
                <el-table-column prop="total" label="总分" width="80" />
                <el-table-column prop="comment" label="评语" min-width="160" show-overflow-tooltip />
              </el-table>
              <el-empty v-else description="暂无评分" />
            </template>
          </el-table-column>
          <el-table-column prop="workTitle" label="作品名称" min-width="160" />
          <el-table-column prop="submitterName" label="提交人" width="100" />
          <el-table-column label="评分人数" width="80">
            <template #default="{ row }">{{ row.scores?.length || 0 }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBatchList, createBatch, updateBatch, getBatchScoreDetail, deleteBatch } from '@/api/admin/scoreBatch'
import type { ScoreBatchItem, BatchScoreDetail } from '@/api/admin/scoreBatch'
import request from '@/api/request'

const loading = ref(false)
const list = ref<ScoreBatchItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const detail = ref<ScoreBatchItem | null>(null)
const formRef = ref<any>(null)

const form = reactive({
  batchName: '',
  classScopes: '',
  status: 1 as number,
  id: undefined as number | undefined,
  timeRange: null as [string, string] | null
})

const rules = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }]
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getBatchList(pageNum.value, pageSize.value)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const openDialog = (row?: ScoreBatchItem) => {
  if (row) {
    isEdit.value = true
    form.id = row.id
    form.batchName = row.batchName
    form.classScopes = row.classScopes
    form.status = row.status
    form.timeRange = [row.startTime, row.endTime]
  } else {
    isEdit.value = false
    form.id = undefined
    form.batchName = ''
    form.classScopes = ''
    form.status = 1
    form.timeRange = null
  }
  dialogVisible.value = true
}

const viewDetail = (row: ScoreBatchItem) => {
  detail.value = row
  detailVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const { timeRange, ...rest } = form
    const data = {
      ...rest,
      startTime: timeRange?.[0],
      endTime: timeRange?.[1]
    }
    if (isEdit.value && form.id) {
      await updateBatch(data)
      ElMessage.success('更新成功')
    } else {
      await createBatch(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: ScoreBatchItem) => {
  try {
    await ElMessageBox.confirm(`确认删除批次「${row.batchName}」？`, '提示')
    await deleteBatch(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch { /* cancelled or failed */ }
}

const scoreVisible = ref(false)
const scoreLoading = ref(false)
const scoreBatchId = ref<number>(0)
const rankingPublished = ref(false)
const scoreWorks = ref<BatchScoreDetail[]>([])

const viewScores = async (row: ScoreBatchItem) => {
  scoreBatchId.value = row.id
  rankingPublished.value = row.rankPublished === 1
  scoreVisible.value = true
  scoreLoading.value = true
  try {
    const res = await getBatchScoreDetail(row.id)
    scoreWorks.value = res.data || []
  } catch {
    scoreWorks.value = []
  } finally {
    scoreLoading.value = false
  }
}

const publishRanking = async (batchId: number) => {
  try {
    await request.post(`/score-batch/${batchId}/publish-ranking`)
    ElMessage.success('排行榜已公示')
    rankingPublished.value = true
    loadList()
  } catch { /* handled */ }
}

const unpublishRanking = async (batchId: number) => {
  try {
    await request.post(`/score-batch/${batchId}/unpublish-ranking`)
    ElMessage.success('已取消公示')
    rankingPublished.value = false
    loadList()
  } catch { /* handled */ }
}

onMounted(loadList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-item label { display: block; font-size: 12px; color: #999; margin-bottom: 4px; }
.detail-item span { font-size: 14px; color: #333; }

.batch-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
}

.batch-actions :deep(.el-button) {
  min-width: 0;
  margin-left: 0;
  padding-inline: 14px;
}
</style>
