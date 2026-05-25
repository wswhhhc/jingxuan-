<template>
  <div class="score-page">
    <section class="editorial-note reveal-up">
      当前页面保持匿名评审，不展示学生姓名和成员信息。建议先浏览简介、附件、视频与运行说明，再进行维度评分。
    </section>

    <section class="filter-panel reveal-up reveal-delay-1">
      <div class="section-heading">
        <div>
          <h2 class="section-heading__title">筛选评审对象</h2>
          <p class="section-heading__meta">按批次、作品名称、技术栈和评分状态快速切换待评材料。</p>
        </div>
      </div>
      <el-form :model="query" inline class="score-filters">
        <el-form-item label="批次">
          <el-select v-model="query.batchId" clearable placeholder="全部批次" @change="loadList">
            <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="作品名称" clearable />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="query.techStack" placeholder="技术栈" clearable />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.onlyUnscored">仅看未评分</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="score-layout reveal-up reveal-delay-2">
      <section class="table-panel">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">作品评分列表</h2>
            <p class="section-heading__meta">共 {{ total }} 项，点击左侧作品进入右侧评分区。</p>
          </div>
        </div>

        <el-table
          :data="workList"
          v-loading="loading"
          row-key="id"
          :current-row-key="selectedWork?.id"
          highlight-current-row
          @row-click="selectWork"
        >
          <el-table-column label="编号" width="76">
            <template #default="{ $index }">
              {{ getRowIndex($index) }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="作品名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="techStack" label="技术栈" min-width="150" show-overflow-tooltip />
          <el-table-column label="评分状态" width="108">
            <template #default="{ row }">
              <el-tag v-if="row.scored" type="success" size="small">已评</el-tag>
              <el-tag v-else type="warning" size="small">待评</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            layout="total, prev, pager, next"
            small
            @change="loadList"
          />
        </div>
      </section>

      <div class="score-side">
        <template v-if="selectedWork">
          <section class="surface-panel material-card">
            <div class="section-heading">
              <div>
                <h2 class="section-heading__title">匿名评审 #{{ selectedWork.id }}</h2>
                <p class="section-heading__meta">评分前可直接查看作品材料，不展示学生姓名、成员与提交人信息。</p>
              </div>
            </div>

            <div class="material-actions">
              <el-button v-if="selectedWork.previewUrl" @click="openLink(selectedWork.previewUrl)">在线预览</el-button>
              <el-button v-if="selectedWork.videoUrl" type="success" @click="openLink(selectedWork.videoUrl)">演示视频</el-button>
            </div>

            <div class="work-info">
              <h3>{{ selectedWork.title }}</h3>
              <div class="material-block">
                <div class="block-label">作品简介</div>
                <p class="summary-text">{{ selectedWork.summary || '暂无简介' }}</p>
              </div>

              <div class="material-block">
                <div class="block-label">技术栈</div>
                <div class="tag-list">
                  <el-tag v-for="tag in techStackList" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                  <span v-if="techStackList.length === 0" class="text-muted">暂无技术栈信息</span>
                </div>
              </div>

              <div class="material-block">
                <div class="block-label">附件列表</div>
                <div v-if="selectedWork.attachments?.length" class="attachment-list">
                  <div v-for="file in selectedWork.attachments" :key="file.id || file.fileName" class="attachment-item">
                    <span class="attachment-name">{{ file.fileName }}</span>
                    <el-button link type="primary" @click="openLink(file.fileUrl)">查看附件</el-button>
                  </div>
                </div>
                <span v-else class="text-muted">暂无附件</span>
              </div>

              <div class="material-block">
                <div class="block-label">运行说明</div>
                <pre class="run-desc">{{ selectedWork.runDesc || '暂无运行说明' }}</pre>
              </div>
            </div>
          </section>

          <section class="surface-panel score-card">
            <div class="section-heading">
              <div>
                <h2 class="section-heading__title">评分表单</h2>
                <p class="section-heading__meta">按维度打分后自动汇总总分。</p>
              </div>
            </div>
            <el-form :model="scoreForm" label-width="100px">
              <el-form-item label="创新性(25)">
                <el-slider v-model="scoreForm.innovation" :min="0" :max="25" show-input />
              </el-form-item>
              <el-form-item label="技术难度(25)">
                <el-slider v-model="scoreForm.difficulty" :min="0" :max="25" show-input />
              </el-form-item>
              <el-form-item label="完成度(30)">
                <el-slider v-model="scoreForm.completion" :min="0" :max="30" show-input />
              </el-form-item>
              <el-form-item label="实用性(20)">
                <el-slider v-model="scoreForm.practicality" :min="0" :max="20" show-input />
              </el-form-item>
              <el-form-item label="总分">
                <span class="total-score">{{ totalScore }}</span>
              </el-form-item>
              <el-form-item label="评语">
                <el-input v-model="scoreForm.comment" type="textarea" :rows="4" placeholder="填写评语（可选）" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="submitting" class="submit-button" @click="handleSubmit">
                  {{ selectedWorkScored ? '更新评分' : '提交评分' }}
                </el-button>
              </el-form-item>
            </el-form>
          </section>
        </template>

        <section v-else class="surface-panel empty-panel">
          <div class="section-heading">
            <div>
              <h2 class="section-heading__title">评分提示</h2>
              <p class="section-heading__meta">请从左侧列表选择一个作品开始评分。</p>
            </div>
          </div>
          <p class="text-muted tip-list">
            1. 当前页面保持匿名评审，不展示学生姓名和成员信息。<br>
            2. 可先查看简介、附件、视频、预览与运行说明后再打分。<br>
            3. 评分截止前可返回“我的评分记录”继续调整。
          </p>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScoredWorkList } from '@/api/teacher/work'
import { submitScore, getMyScore, getBatchList } from '@/api/teacher/score'
import { getWorkDetail } from '@/api/teacher/work'
import type { WorkDetailVO, WorkListVO } from '@/api/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const workList = ref<WorkListVO[]>([])
const batches = ref<{ id: number; batchName: string }[]>([])
const total = ref(0)
const selectedWork = ref<WorkDetailVO | null>(null)
const selectedWorkScored = ref(false)

const query = reactive({
  page: 1,
  size: 20,
  batchId: undefined as number | undefined,
  keyword: '',
  techStack: '',
  onlyUnscored: false
})

const scoreForm = reactive({
  innovation: 15,
  difficulty: 15,
  completion: 20,
  practicality: 12,
  comment: ''
})

const totalScore = computed(() =>
  scoreForm.innovation + scoreForm.difficulty + scoreForm.completion + scoreForm.practicality
)

const techStackList = computed(() =>
  (selectedWork.value?.techStack || '').split(',').map(item => item.trim()).filter(Boolean)
)

const getRowIndex = (index: number) => (query.page - 1) * query.size + index + 1

const resetScoreForm = () => {
  scoreForm.innovation = 15
  scoreForm.difficulty = 15
  scoreForm.completion = 20
  scoreForm.practicality = 12
  scoreForm.comment = ''
}

const buildFallbackWork = (row: Partial<WorkListVO> & { id: number; batchId?: number }): WorkDetailVO => ({
  id: row.id,
  title: row.title || `作品 #${row.id}`,
  summary: '',
  techStack: row.techStack || '',
  advisor: '',
  coverUrl: '',
  videoUrl: '',
  runDesc: '',
  status: 3,
  statusLabel: '',
  submitterId: 0,
  submitterName: '',
  submitTime: '',
  batchId: row.batchId || 0,
  publishStatus: 0,
  publishStatusLabel: '',
  featured: 0,
  previewUrl: '',
  avgScore: '',
  members: [],
  attachments: []
})

const loadBatches = async () => {
  try {
    const res = await getBatchList()
    batches.value = res.data || []
  } catch {
    batches.value = []
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getScoredWorkList({ ...query })
    workList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const loadMyScoreData = async (workId: number) => {
  try {
    const scoreRes = await getMyScore(workId)
    if (scoreRes.data) {
      scoreForm.innovation = Number(scoreRes.data.innovation ?? 15)
      scoreForm.difficulty = Number(scoreRes.data.difficulty ?? 15)
      scoreForm.completion = Number(scoreRes.data.completion ?? 20)
      scoreForm.practicality = Number(scoreRes.data.practicality ?? 12)
      scoreForm.comment = scoreRes.data.comment || ''
      selectedWorkScored.value = true
      return
    }
  } catch {
    // ignore
  }
  selectedWorkScored.value = false
  resetScoreForm()
}

const loadSelectedWork = async (row: Partial<WorkListVO> & { id: number; batchId?: number }) => {
  try {
    const detailRes = await getWorkDetail(row.id)
    selectedWork.value = detailRes.data
  } catch {
    selectedWork.value = buildFallbackWork(row)
  }
  await loadMyScoreData(row.id)
}

const clearScoreRouteQuery = () => {
  if (!route.query.workId && !route.query.batchId && !route.query.from) return
  router.replace({ path: route.path }).catch(() => undefined)
}

const selectWork = async (row: WorkListVO) => {
  await loadSelectedWork(row)
  clearScoreRouteQuery()
}

const syncRouteSelection = async () => {
  const rawWorkId = Array.isArray(route.query.workId) ? route.query.workId[0] : route.query.workId
  const rawBatchId = Array.isArray(route.query.batchId) ? route.query.batchId[0] : route.query.batchId
  const rawFrom = Array.isArray(route.query.from) ? route.query.from[0] : route.query.from
  const workId = Number(rawWorkId)
  const batchId = Number(rawBatchId)
  if (!workId) return

  if (rawFrom === 'history' && query.onlyUnscored) {
    query.onlyUnscored = false
  }
  if (batchId) {
    query.batchId = batchId
  }

  const matched = workList.value.find(item => item.id === workId)
  if (matched) {
    await loadSelectedWork(matched)
  } else {
    await loadSelectedWork({ id: workId, batchId: batchId || undefined })
  }
  clearScoreRouteQuery()
}

const handleSearch = async () => {
  query.page = 1
  await loadList()
}

const handleReset = async () => {
  query.page = 1
  query.keyword = ''
  query.techStack = ''
  query.batchId = undefined
  query.onlyUnscored = false
  await loadList()
  clearScoreRouteQuery()
}

const handleSubmit = async () => {
  if (!selectedWork.value) return
  submitting.value = true
  try {
    await submitScore({
      workId: selectedWork.value.id,
      innovation: scoreForm.innovation,
      difficulty: scoreForm.difficulty,
      completion: scoreForm.completion,
      practicality: scoreForm.practicality,
      comment: scoreForm.comment
    })
    ElMessage.success(selectedWorkScored.value ? '评分已更新' : '评分提交成功')
    selectedWorkScored.value = true
    await loadList()
  } finally {
    submitting.value = false
  }
}

const openLink = (url?: string) => {
  if (!url) return
  window.open(url, '_blank')
}

watch(
  () => route.query.workId,
  async (value) => {
    if (!value) return
    const rawBatchId = Array.isArray(route.query.batchId) ? route.query.batchId[0] : route.query.batchId
    const batchId = Number(rawBatchId)
    if (batchId && query.batchId !== batchId) {
      query.batchId = batchId
      await loadList()
    }
    await syncRouteSelection()
  }
)

onMounted(async () => {
  const rawBatchId = Array.isArray(route.query.batchId) ? route.query.batchId[0] : route.query.batchId
  const rawFrom = Array.isArray(route.query.from) ? route.query.from[0] : route.query.from
  const batchId = Number(rawBatchId)
  if (batchId) {
    query.batchId = batchId
  }
  if (rawFrom === 'history') {
    query.onlyUnscored = false
  }
  await loadBatches()
  await loadList()
  await syncRouteSelection()
})
</script>

<style scoped>
.score-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.score-filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.score-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 22px;
  align-items: start;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.score-side {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.material-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.work-info h3 {
  margin: 0 0 16px;
  font-family: var(--font-display);
  font-size: 28px;
}

.material-block + .material-block {
  margin-top: 18px;
}

.block-label {
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.summary-text,
.text-muted {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.85;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.attachment-name {
  word-break: break-all;
}

.run-desc {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--page-bg) 60%, transparent);
  color: var(--text-secondary);
  white-space: pre-wrap;
  line-height: 1.8;
  font-family: inherit;
}

.total-score {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--brand);
}

.submit-button {
  width: 100%;
}

.tip-list {
  margin-top: 8px;
}

@media (max-width: 1080px) {
  .score-layout {
    grid-template-columns: 1fr;
  }
}
</style>
