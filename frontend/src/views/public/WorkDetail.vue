<template>
  <div class="app-page work-detail" v-loading="loading">
    <div v-if="work" class="detail-stack">
      <div class="detail-back">
        <el-button link @click="router.push('/works')">← 返回作品展廊</el-button>
      </div>

      <section class="surface-panel detail-hero reveal-up">
        <div class="detail-hero__meta">
          <span class="page-kicker">Work Profile</span>
          <h1 class="detail-hero__title">{{ work.title }}</h1>
          <div class="detail-hero__info">
            <span>作者：{{ work.submitterName }}</span>
            <span>指导教师：{{ work.advisor || '未标注' }}</span>
            <span>提交时间：{{ work.submitTime }}</span>
          </div>
          <p class="detail-hero__summary">{{ work.summary || '暂无作品简介。' }}</p>
          <div class="detail-hero__actions">
            <el-tag v-if="work.featured" type="warning" effect="dark">精选作品</el-tag>
            <el-tag v-if="work.score !== undefined" type="success" effect="plain">评分：{{ work.score }} 分</el-tag>
            <el-tag v-if="work.rank" type="danger" effect="plain">排名：第 {{ work.rank }} 名</el-tag>
            <el-button v-if="work.previewUrl" type="primary" @click="openPreview">在线体验</el-button>
          </div>
        </div>

        <div class="detail-hero__cover">
          <el-image :src="work.coverUrl || '/placeholder-cover.png'" fit="contain" class="detail-cover">
            <template #error>
              <div class="cover-placeholder"><el-icon :size="48"><Picture /></el-icon></div>
            </template>
          </el-image>
        </div>
      </section>

      <div class="detail-grid reveal-up reveal-delay-1">
        <section class="surface-panel">
          <div class="section-heading">
            <h2 class="section-heading__title">作品信息</h2>
          </div>
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">技术栈</span>
              <div class="info-value tag-wrap">
                <el-tag v-for="tag in techTags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                <span v-if="techTags.length === 0" class="text-muted">暂无技术栈信息</span>
              </div>
            </div>
            <div class="info-item">
              <span class="info-label">指导教师</span>
              <span class="info-value">{{ work.advisor || '无' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">提交时间</span>
              <span class="info-value">{{ work.submitTime }}</span>
            </div>
          </div>

          <div v-if="work.runDescription" class="detail-block">
            <h3>运行说明</h3>
            <pre class="run-desc">{{ work.runDescription }}</pre>
          </div>
        </section>

        <section class="surface-panel">
          <div class="section-heading">
            <h2 class="section-heading__title">团队成员</h2>
            <span class="section-heading__meta">{{ work.members?.length || 0 }} 位成员</span>
          </div>
          <div v-if="work.members?.length" class="member-list">
            <div v-for="m in work.members" :key="m.id || m.studentNo" class="member-item">
              <el-avatar :size="36">{{ m.studentName.charAt(0) }}</el-avatar>
              <div class="member-info">
                <div class="member-name">
                  <span>{{ m.studentName }}</span>
                  <el-tag v-if="m.isLeader" type="warning" size="small" effect="dark">队长</el-tag>
                </div>
                <span class="member-meta">{{ m.studentNo }} · {{ m.className }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无成员信息" :image-size="60" />
        </section>
      </div>

      <div class="detail-grid detail-grid--secondary reveal-up reveal-delay-2">
        <section class="surface-panel">
          <div class="section-heading">
            <h2 class="section-heading__title">附件与媒体</h2>
          </div>

          <div v-if="work.attachments?.length" class="attachment-list">
            <div v-for="att in work.attachments" :key="att.id || att.fileName" class="attachment-item">
              <div class="attachment-item__left">
                <el-icon><Files /></el-icon>
                <span>{{ att.fileName }}</span>
              </div>
              <el-button link type="primary" @click="downloadFile(att.fileUrl, att.fileName)">下载</el-button>
            </div>
          </div>
          <p v-else class="text-muted">暂无附件资料</p>

          <div v-if="work.videoUrl" class="detail-block">
            <h3>演示视频</h3>
            <video :src="work.videoUrl" controls class="video-player" />
          </div>
        </section>

        <section class="surface-panel">
          <div class="section-heading">
            <h2 class="section-heading__title">评分与排行</h2>
          </div>
          <div v-if="work.score !== undefined" class="score-display">
            <div class="score-main">
              <span class="score-num">{{ work.score }}</span>
              <span class="score-unit">分</span>
            </div>
            <div v-if="work.rank" class="rank-info">当前排名：第 <strong>{{ work.rank }}</strong> 名</div>
          </div>
          <p v-else class="text-muted">该作品暂未公开评分。</p>
        </section>
      </div>

      <section class="surface-panel comment-card reveal-up reveal-delay-3">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">作品评论</h2>
            <p class="section-heading__meta">共 {{ commentTotal }} 条交流内容</p>
          </div>
        </div>

        <div v-if="isLoggedIn" class="comment-input">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :loading="commentSending" class="comment-submit" @click="submitComment">
            发表评论
          </el-button>
        </div>

        <div v-else class="comment-login-tip">
          <el-alert
            title="登录后可评论，学生和教师账号均可参与交流"
            type="info"
            :closable="false"
            show-icon
          />
          <div class="comment-login-actions">
            <el-button type="primary" plain @click="goToLogin">立即登录</el-button>
          </div>
        </div>

        <div v-if="comments.length" class="comment-list">
          <CommentThread
            v-for="c in comments"
            :key="c.id"
            :comment="c"
            :depth="0"
            :is-logged-in="isLoggedIn"
            :reply-target-id="replyTargetId"
            :reply-target-name="replyTargetName"
            :reply-sending="replySending"
            :current-user-id="currentUser?.id ?? null"
            :is-admin="isAdmin"
            :is-work-leader="isWorkLeader"
            @open-reply="openReply"
            @cancel-reply="cancelReply"
            @submit-reply="submitReply"
            @delete-comment="handleDeleteComment"
            @go-to-login="goToLogin"
          />
        </div>
        <el-empty v-else description="暂无评论，来抢沙发吧~" :image-size="80" />

        <div v-if="commentTotal > comments.length" class="load-more">
          <el-button link type="primary" @click="loadMoreComments">加载更多</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Files } from '@element-plus/icons-vue'
import { getPublicWorkDetail } from '../../api/public/work'
import { getCommentList, addComment, deleteComment } from '../../api/public/comment'
import type { CommentItem } from '../../api/public/comment'
import type { UserInfo } from '../../api/student/auth'
import type { WorkItem } from '../../api/student/work'
import CommentThread from './CommentThread.vue'

function openPreview() {
  if (work.value?.previewUrl) {
    window.open(work.value.previewUrl, '_blank')
  }
}

const route = useRoute()
const router = useRouter()
const work = ref<WorkItem | null>(null)
const loading = ref(false)
const comments = ref<CommentItem[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentText = ref('')
const commentSending = ref(false)
const replyTargetId = ref<string | number | null>(null)
const replyTargetName = ref('')
const replySending = ref(false)
const currentUser = ref<UserInfo | null>(getCachedUserInfo())

const techTags = computed(() => work.value?.techStack?.split(',').map((t) => t.trim()).filter(Boolean) || [])
const isLoggedIn = ref(hasLoginToken())
const isAdmin = computed(() => currentUser.value?.roleCode === 'ROLE_ADMIN')
const isWorkLeader = computed(() => {
  const userId = currentUser.value?.id
  if (!userId || !work.value?.members?.length) return false
  return work.value.members.some(member => member.isLeader && member.studentId === userId)
})

function getCachedUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) as UserInfo : null
  } catch {
    return null
  }
}

function hasLoginToken() {
  return !!(sessionStorage.getItem('token') || localStorage.getItem('token'))
}

function syncLoginState() {
  isLoggedIn.value = hasLoginToken()
  currentUser.value = getCachedUserInfo()
  if (!isLoggedIn.value) {
    replyTargetId.value = null
    replyTargetName.value = ''
  }
}

function goToLogin() {
  router.push('/login')
}

function getRouteWorkId() {
  const id = route.params.id
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : ''
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getPublicWorkDetail(getRouteWorkId())
    work.value = res.data
  } catch {
    ElMessage.error('加载作品详情失败')
  } finally {
    loading.value = false
  }
}

async function loadComments(reset = false) {
  if (reset) commentPage.value = 1
  try {
    const res = await getCommentList(getRouteWorkId(), commentPage.value, 10)
    const data = res.data
    const list = data?.records || []
    if (reset) {
      comments.value = list
    } else {
      comments.value = [...comments.value, ...list]
    }
    commentTotal.value = data?.total || 0
  } catch {
    if (reset) comments.value = []
    commentTotal.value = 0
  }
}

async function submitComment() {
  if (!isLoggedIn.value) {
    ElMessage.info('登录后可评论')
    goToLogin()
    return
  }
  const text = commentText.value.trim()
  if (!text) return
  commentSending.value = true
  try {
    await addComment(getRouteWorkId(), text)
    ElMessage.success('评论成功')
    commentText.value = ''
    loadComments(true)
  } finally {
    commentSending.value = false
  }
}

function loadMoreComments() {
  commentPage.value++
  loadComments(false)
}

function openReply(id: string | number, userName: string) {
  if (!isLoggedIn.value) {
    ElMessage.info('登录后可回复')
    goToLogin()
    return
  }
  replyTargetId.value = id
  replyTargetName.value = userName || ''
}

function cancelReply() {
  replyTargetId.value = null
  replyTargetName.value = ''
}

async function submitReply(parentId: string | number, content: string) {
  if (!isLoggedIn.value) {
    ElMessage.info('登录后可回复')
    goToLogin()
    return
  }
  const text = content.trim()
  if (!text) return
  replySending.value = true
  try {
    await addComment(getRouteWorkId(), text, parentId)
    ElMessage.success('回复成功')
    cancelReply()
    loadComments(true)
  } finally {
    replySending.value = false
  }
}

async function handleDeleteComment(commentId: string | number) {
  try {
    await ElMessageBox.confirm('删除这条评论后，下面的所有回复也会一并删除，是否继续？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteComment(commentId)
    ElMessage.success('评论已删除')
    if (replyTargetId.value === commentId) {
      cancelReply()
    }
    loadComments(true)
  } catch (error: any) {
    if (error === 'cancel' || error?.message === 'cancel') return
  }
}

function downloadFile(url: string, name: string) {
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
}

onMounted(() => {
  syncLoginState()
  window.addEventListener('focus', syncLoginState)
  window.addEventListener('storage', syncLoginState)
  loadDetail()
  loadComments(true)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', syncLoginState)
  window.removeEventListener('storage', syncLoginState)
})
</script>

<style scoped>
.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.detail-back {
  margin-top: 6px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 24px;
  align-items: stretch;
}

.detail-hero__meta {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-hero__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(32px, 5vw, 54px);
  line-height: 1.08;
}

.detail-hero__info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  color: var(--text-muted);
  font-size: 13px;
}

.detail-hero__summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

.detail-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-hero__cover {
  min-height: 360px;
  padding: 18px;
  border-radius: 28px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--page-bg-alt) 78%, transparent), transparent);
}

.detail-cover {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-muted);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.detail-grid--secondary {
  align-items: start;
}

.info-list,
.member-list,
.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.info-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.info-label {
  width: 86px;
  flex-shrink: 0;
  color: var(--text-muted);
}

.info-value {
  color: var(--text-primary);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-block {
  margin-top: 22px;
}

.detail-block h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--text-secondary);
}

.run-desc {
  margin: 0;
  padding: 18px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--page-bg) 62%, transparent);
  color: var(--text-secondary);
  line-height: 1.85;
  white-space: pre-wrap;
  font-family: inherit;
}

.member-item {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.member-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.member-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.member-meta,
.text-muted {
  color: var(--text-muted);
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: 18px;
  background: color-mix(in srgb, var(--card-bg) 84%, transparent);
}

.attachment-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.attachment-item__left span {
  word-break: break-all;
}

.video-player {
  width: 100%;
  border-radius: 20px;
}

.score-display {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  min-height: 100%;
}

.score-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.score-num {
  font-family: var(--font-display);
  font-size: 72px;
  line-height: 0.9;
  color: var(--brand);
}

.score-unit {
  color: var(--text-muted);
}

.rank-info {
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 16px;
}

.rank-info strong {
  color: var(--text-primary);
  font-size: 28px;
}

.comment-input,
.comment-login-tip {
  margin-bottom: 24px;
}

.comment-submit {
  margin-top: 12px;
}

.comment-login-actions {
  margin-top: 12px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.load-more {
  margin-top: 14px;
  text-align: center;
}

@media (max-width: 1024px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-hero__cover {
    min-height: auto;
    padding: 12px;
  }

  .detail-cover {
    min-height: 240px;
  }

  .info-item {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
