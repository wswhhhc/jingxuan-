<template>
  <div class="workspace-page prize-page">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="奖项配置" name="config">
        <section class="workspace-section reveal-up">
          <div class="workspace-toolbar workspace-toolbar--tight">
            <div class="workspace-toolbar__body">
              <h3 class="workspace-toolbar__title">奖项配置</h3>
              <p class="workspace-toolbar__desc">按批次整理奖项、奖品与名额配置。</p>
            </div>
            <div class="workspace-toolbar__actions">
              <el-select v-model="batchFilter" clearable placeholder="筛选批次" @change="loadList">
              <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
              </el-select>
              <el-button type="primary" @click="showEdit()">新增奖项</el-button>
            </div>
          </div>

          <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="batchName" label="批次" width="140" />
            <el-table-column prop="rewardLevel" label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="levelType(row.rewardLevel)" size="small">{{ row.rewardLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rewardName" label="奖项名称" width="120" />
            <el-table-column prop="prizeName" label="奖品" min-width="200" show-overflow-tooltip />
            <el-table-column prop="quota" label="名额" width="70" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="showEdit(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
      </el-tab-pane>

      <el-tab-pane label="发放追踪" name="issue">
        <section class="workspace-section reveal-up">
          <div class="workspace-toolbar workspace-toolbar--tight">
            <div class="workspace-toolbar__body">
              <h3 class="workspace-toolbar__title">发放追踪</h3>
              <p class="workspace-toolbar__desc">跟踪奖项发放与取消状态，保留领奖记录。</p>
            </div>
            <div class="workspace-toolbar__actions">
              <el-select v-model="issueRewardFilter" clearable placeholder="筛选奖品" @change="loadIssueList">
              <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
              </el-select>
              <el-button type="primary" @click="showIssueDialog()">发放奖品</el-button>
            </div>
          </div>

          <el-table :data="issueList" v-loading="issueLoading" stripe>
            <el-table-column prop="id" label="编号" width="70" />
            <el-table-column prop="rewardName" label="奖项" width="120" />
            <el-table-column prop="workTitle" label="作品" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.issueStatus === 1 ? 'success' : 'info'" size="small">
                  {{ row.issueStatus === 1 ? '已发放' : '已取消' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="issueTime" label="发放时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.issueStatus === 1" size="small" type="warning" @click="handleCancelIssue(row)">取消发放</el-button>
                <span v-else style="color:#c0c4cc">--</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="workspace-pagination">
            <el-pagination
              v-model:current-page="issuePage"
              v-model:page-size="issueSize"
              :total="issueTotal"
              layout="total, prev, pager, next"
              @change="loadIssueList"
            />
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="editVisible" :title="isEdit ? '编辑奖项' : '新增奖项'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="批次">
          <el-select v-model="form.batchId" placeholder="选择批次" style="width:100%">
            <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.rewardLevel" placeholder="选择等级" style="width:100%">
            <el-option label="一等奖" value="一等奖" />
            <el-option label="二等奖" value="二等奖" />
            <el-option label="三等奖" value="三等奖" />
            <el-option label="优秀奖" value="优秀奖" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖项名称">
          <el-input v-model="form.rewardName" placeholder="如：最佳创新奖" />
        </el-form-item>
        <el-form-item label="奖品">
          <el-input v-model="form.prizeName" placeholder="奖品名称" />
        </el-form-item>
        <el-form-item label="名额">
          <el-input-number v-model="form.quota" :min="1" :max="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueVisible" title="发放奖品" width="400px">
      <el-form :model="issueForm" label-width="80px">
        <el-form-item label="奖品">
          <el-select v-model="issueForm.rewardId" placeholder="选择奖品" style="width:100%">
            <el-option v-for="p in list" :key="p.id" :label="`${p.batchName} - ${p.rewardName}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作品ID">
          <el-input-number v-model="issueForm.workId" :min="1" placeholder="输入作品ID" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="issueSaving" @click="handleIssueSubmit">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPrizeList, createPrize, updatePrize, deletePrize, getPrizeBatches, getIssueList, issuePrize, cancelIssue } from '@/api/admin/prize'
import type { PrizeItem, IssueItem } from '@/api/admin/prize'

const activeTab = ref('config')
const loading = ref(false)
const list = ref<PrizeItem[]>([])
const batches = ref<{ id: number; batchName: string }[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const batchFilter = ref('')
const editVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const form = reactive({
  batchId: 0,
  rewardLevel: '',
  rewardName: '',
  prizeName: '',
  quota: 1
})

// issue tracking
const issueLoading = ref(false)
const issueList = ref<IssueItem[]>([])
const issueTotal = ref(0)
const issuePage = ref(1)
const issueSize = ref(20)
const issueRewardFilter = ref('')
const issueVisible = ref(false)
const issueSaving = ref(false)
const issueForm = reactive({
  rewardId: 0,
  workId: 0
})

const levelType = (l: string) => {
  const map: Record<string, string> = { '一等奖': 'danger', '二等奖': 'warning', '三等奖': '', '优秀奖': 'info' }
  return map[l] || ''
}

const loadBatches = async () => {
  try {
    const res = await getPrizeBatches()
    batches.value = res.data || []
  } catch { /* mock */ }
}

const loadList = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (batchFilter.value) params.batchId = batchFilter.value
    const res = await getPrizeList(params)
    list.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const showEdit = (row?: PrizeItem) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.batchId = row.batchId
    form.rewardLevel = row.rewardLevel
    form.rewardName = row.rewardName
    form.prizeName = row.prizeName
    form.quota = row.quota
  } else {
    isEdit.value = false
    editId.value = 0
    form.batchId = 0
    form.rewardLevel = ''
    form.rewardName = ''
    form.prizeName = ''
    form.quota = 1
  }
  editVisible.value = true
}

const handleSave = async () => {
  if (!form.batchId || !form.rewardLevel) {
    ElMessage.warning('请填写完整')
    return
  }
  try {
    if (isEdit.value) {
      await updatePrize(editId.value, form)
    } else {
      await createPrize(form)
    }
    ElMessage.success(isEdit.value ? '已更新' : '已创建')
    editVisible.value = false
    loadList()
  } catch { /* mock */ }
}

const handleDelete = async (row: PrizeItem) => {
  try {
    await ElMessageBox.confirm('确认删除此奖项配置？', '提示')
    await deletePrize(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch { /* */ }
}

const loadIssueList = async () => {
  issueLoading.value = true
  try {
    const params: any = { page: issuePage.value, size: issueSize.value }
    if (issueRewardFilter.value) params.rewardId = issueRewardFilter.value
    const res = await getIssueList(params)
    issueList.value = res.data?.records || []
    issueTotal.value = res.data?.total || 0
  } finally {
    issueLoading.value = false
  }
}

const showIssueDialog = () => {
  issueForm.rewardId = 0
  issueForm.workId = 0
  issueVisible.value = true
}

const handleIssueSubmit = async () => {
  if (!issueForm.rewardId || !issueForm.workId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  issueSaving.value = true
  try {
    await issuePrize({ rewardId: issueForm.rewardId, workId: issueForm.workId, operatorId: 1 })
    ElMessage.success('已发放')
    issueVisible.value = false
    loadIssueList()
  } catch { /* */ } finally {
    issueSaving.value = false
  }
}

const handleCancelIssue = async (row: IssueItem) => {
  try {
    await ElMessageBox.confirm('确认取消该发放记录？', '提示')
    await cancelIssue(row.id)
    ElMessage.success('已取消')
    loadIssueList()
  } catch { /* */ }
}

const onTabChange = (tab: string) => {
  if (tab === 'issue') loadIssueList()
}

onMounted(() => { loadBatches(); loadList() })
</script>

<style scoped>
.prize-page {
  max-width: 1240px;
  margin: 0 auto;
}
</style>
