<template>
  <div class="workspace-page work-submit">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Submission Studio</span>
          <h2 class="workspace-intro__title">{{ pageTitle }}</h2>
          <p class="workspace-intro__summary">
            以编辑版式整理作品名称、摘要、材料与团队信息，让提交内容既适合审核，也适合后续展示。
          </p>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :disabled="isView"
        label-width="110px"
        label-position="right"
        size="large"
        class="workspace-form"
      >
        <section class="workspace-form-section">
          <div class="workspace-form-section__header">
            <h3 class="workspace-form-section__title">基本信息</h3>
            <p class="workspace-form-section__desc">先定义作品的标题、摘要与核心技术标签，为评审建立第一层阅读线索。</p>
          </div>

          <el-form-item label="作品名称" prop="title">
            <el-input v-model="form.title" placeholder="请输入作品名称" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="作品简介" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="4"
              placeholder="请简要描述作品的功能、用途和亮点"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <div class="workspace-form-grid">
            <el-form-item label="技术栈" prop="techStack">
              <el-input v-model="form.techStack" placeholder="如 Vue3, Spring Boot, MySQL（逗号分隔）" />
            </el-form-item>
            <el-form-item label="指导教师" prop="advisor">
              <el-input v-model="form.advisor" placeholder="请输入指导教师姓名" />
            </el-form-item>
          </div>

          <el-form-item label="封面图片" prop="coverUrl">
            <el-upload
              :disabled="isView"
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              accept="image/png,image/jpeg,image/gif"
            >
              <template #trigger>
                <div v-if="!form.coverUrl" class="cover-upload">
                  <el-icon :size="32"><Plus /></el-icon>
                  <span>上传封面</span>
                </div>
                <el-image v-else :src="form.coverUrl" class="cover-preview" fit="cover" />
              </template>
            </el-upload>
          </el-form-item>
        </section>

        <section class="workspace-form-section">
          <div class="workspace-form-section__header">
            <h3 class="workspace-form-section__title">附件资料</h3>
            <p class="workspace-form-section__desc">整理压缩包、图像、视频与运行说明，让审核和展示都有充足的阅读材料。</p>
          </div>

          <el-form-item label="上传文件" prop="attachments">
            <div class="attachment-area">
              <el-upload
                v-model:file-list="fileList"
                :disabled="isView"
                :before-upload="beforeFileUpload"
                :on-remove="handleFileRemove"
                multiple
                list-type="text"
              >
                <el-button v-if="!isView" type="primary" plain>
                  <el-icon><Upload /></el-icon>选择文件
                </el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 zip/rar/7z/jpg/png/gif/mp4/pdf，压缩包≤200MB，图片≤10MB，视频≤500MB
                  </div>
                </template>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item label="演示视频链接">
            <el-input v-model="form.videoUrl" placeholder="请输入演示视频在线播放地址（可选）" />
          </el-form-item>

          <el-form-item label="运行说明" prop="runDescription">
            <el-input
              v-model="form.runDescription"
              type="textarea"
              :rows="4"
              placeholder="请描述作品的运行环境要求、安装步骤和启动方式（可选）"
            />
          </el-form-item>
        </section>

        <section class="workspace-form-section">
          <div class="workspace-form-section__header">
            <h3 class="workspace-form-section__title">团队成员</h3>
            <p class="workspace-form-section__desc">明确主创与成员信息，保持提交名单与展示信息的一致性。</p>
          </div>

          <el-form-item label="成员列表">
            <div class="member-area">
              <div v-for="(member, idx) in form.members" :key="idx" class="member-row">
                <el-select v-model="member.isLeader" class="member-role" @change="handleLeaderChange(idx)">
                  <el-option :value="true" label="队长" />
                  <el-option :value="false" label="成员" />
                </el-select>
                <el-input v-model="member.studentName" placeholder="姓名" class="member-input" />
                <el-input v-model="member.studentNo" placeholder="学号" class="member-input" />
                <el-input v-model="member.className" placeholder="班级" class="member-input" />
                <el-button v-if="!isView" type="danger" :icon="Delete" circle @click="removeMember(idx)" />
              </div>
              <el-button v-if="!isView" type="primary" plain @click="addMember">
                <el-icon><Plus /></el-icon>添加成员
              </el-button>
            </div>
          </el-form-item>
        </section>

        <div class="form-actions">
          <el-button v-if="!isView" type="primary" size="large" :loading="submitting" @click="handleSaveAndSubmit">
            {{ submitting ? '内容安全检测中...' : '提交审核' }}
          </el-button>
          <el-button v-if="!isView" size="large" :loading="saving" @click="handleSaveDraft">
            保存草稿
          </el-button>
          <el-button size="large" @click="router.back()">{{ isView ? '返回' : '取消' }}</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Upload } from '@element-plus/icons-vue'
import {
  createWork, updateWork, submitWork, getWorkDetail,
  uploadFile, type WorkForm, type WorkAttachment,
} from '../../api/student/work'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const saving = ref(false)
const fileList = ref<any[]>([])
const _pendingUploadNames = new Set<string>()
const hasWorkId = computed(() => Boolean(getRouteWorkId()))
const isEdit = computed(() => route.name === 'WorkEdit' || (hasWorkId.value && route.name !== 'WorkView'))
const isView = computed(() => route.name === 'WorkView')
const pageTitle = computed(() => {
  if (isView.value) return '作品详情'
  return isEdit.value ? '编辑作品' : '提交作品'
})

const form = reactive<WorkForm>({
  title: '',
  summary: '',
  techStack: '',
  advisor: '',
  coverUrl: '',
  videoUrl: '',
  runDescription: '',
  attachments: [],
  members: [{
    studentName: '',
    studentNo: '',
    className: '',
    isLeader: true,
  }],
})

const rules = {
  title: [
    { required: true, message: '请输入作品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '作品名称长度在 2 到 100 个字符', trigger: 'blur' },
  ],
  summary: [
    { required: true, message: '请输入作品简介', trigger: 'blur' },
  ],
  techStack: [
    { required: true, message: '请输入技术栈', trigger: 'blur' },
  ],
}

function getRouteWorkId() {
  const id = route.params.id
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : ''
}

async function loadWork(id: string | number) {
  try {
    const res = await getWorkDetail(id)
    const data = res.data
    Object.assign(form, {
      title: data.title,
      summary: data.summary,
      techStack: data.techStack,
      advisor: data.advisor,
      coverUrl: data.coverUrl,
      videoUrl: data.videoUrl,
      runDescription: data.runDescription,
      members: data.members?.length ? data.members : form.members,
    })
    // 后端返回的附件仅用于初始化（如已有附件则不覆盖已上传的）
    if (data.attachments?.length) {
      form.attachments = data.attachments
    }
    fileList.value = (data.attachments || []).map((a: WorkAttachment) => ({
      uid: String(a.id || a.fileName),
      name: a.fileName,
      url: a.fileUrl,
      status: 'success',
    }))
  } catch {
    ElMessage.error('加载作品信息失败')
  }
}

onMounted(() => {
  const workId = getRouteWorkId()
  if (workId) {
    loadWork(workId)
  }
})

function addMember() {
  form.members.push({
    studentName: '',
    studentNo: '',
    className: '',
    isLeader: false,
  })
}

function removeMember(idx: number) {
  if (form.members.length <= 1) {
    ElMessage.warning('至少需要一名成员')
    return
  }
  form.members.splice(idx, 1)
}

function handleLeaderChange(changedIdx: number) {
  if (form.members[changedIdx].isLeader) {
    form.members.forEach((m, idx) => {
      if (idx !== changedIdx) m.isLeader = false
    })
  }
}

async function handleCoverUpload(file: File): Promise<boolean> {
  try {
    const res = await uploadFile(file, getRouteWorkId() || undefined)
    form.coverUrl = res.data.url
  } catch {
    ElMessage.error('封面上传失败')
  }
  return false
}

async function beforeFileUpload(file: File): Promise<boolean> {
  const whiteList = ['zip', 'rar', '7z', 'jpg', 'png', 'gif', 'mp4', 'pdf']
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !whiteList.includes(ext)) {
    ElMessage.error('不支持的文件格式')
    return false
  }
  const maxSize = ext === 'mp4' ? 500 * 1024 * 1024
    : ['jpg', 'png', 'gif'].includes(ext) ? 10 * 1024 * 1024
    : 200 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小超出限制')
    return false
  }
  try {
    const res = await uploadFile(file, getRouteWorkId() || undefined)
    const uploadedFile = {
      uid: String(res.data.id),
      name: file.name,
      url: res.data.url,
      status: 'success',
    }
    form.attachments.push({
      id: res.data.id,
      fileName: file.name,
      fileType: ext === 'mp4' ? 'video' : ['jpg', 'png', 'gif'].includes(ext) ? 'image' : 'zip',
      fileSize: file.size,
      fileUrl: res.data.url,
    })
    fileList.value = [...fileList.value, uploadedFile]
    ElMessage.success('文件已上传到服务器，保存草稿或提交审核后会绑定到作品')
  } catch {
    ElMessage.error('上传失败')
  }
  _pendingUploadNames.add(file.name)
  return false
}

function handleFileRemove(_file: any, list: any[]) {
  fileList.value = list
  // el-upload 在 beforeFileUpload 返回 false 后会触发一次自动删除，跳过
  if (_pendingUploadNames.has(_file.name)) {
    _pendingUploadNames.delete(_file.name)
    return
  }
  const idx = form.attachments.findIndex(a => a.fileName === _file.name)
  if (idx !== -1) form.attachments.splice(idx, 1)
}

function buildSubmitData(): WorkForm {
  return {
    ...form,
    attachments: form.attachments,
    members: form.members.filter((m) => m.studentName && m.studentNo),
  }
}

async function handleSaveDraft() {
  saving.value = true
  try {
    const data = buildSubmitData()
    if (hasWorkId.value) {
      await updateWork(getRouteWorkId(), data)
    } else {
      await createWork(data)
    }
    ElMessage.success('已保存草稿')
    router.push('/student/works')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

async function handleSaveAndSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = buildSubmitData()
    let id = getRouteWorkId()
    if (hasWorkId.value) {
      await updateWork(id, data)
    } else {
      const res = await createWork(data)
      id = res.data
    }
    await submitWork(id)
    ElMessage.success('提交成功，请等待管理员审核')
    router.push('/student/works')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.work-submit {
  max-width: 1040px;
  margin: 0 auto;
}

.cover-upload {
  width: 220px;
  height: 136px;
  border: 1px dashed var(--border-color);
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  background: color-mix(in srgb, var(--card-bg) 78%, transparent);
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-base);
}
.cover-upload:hover {
  border-color: var(--brand);
  color: var(--brand);
  transform: translateY(-1px);
}

.cover-preview {
  width: 220px;
  height: 136px;
  border-radius: 18px;
  cursor: pointer;
}

.attachment-area { width: 100%; }
.member-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.member-row {
  display: grid;
  grid-template-columns: 90px repeat(3, minmax(0, 1fr)) auto;
  gap: 10px;
  align-items: center;
}

.member-role { width: 90px; }
.member-input { flex: 1; min-width: 0; }
.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 4px;
}

@media (max-width: 768px) {
  .member-row {
    grid-template-columns: 1fr;
  }

  .member-role {
    width: 100%;
  }
}
</style>
