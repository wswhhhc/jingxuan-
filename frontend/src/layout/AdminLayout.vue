<template>
  <div class="workspace-layout admin-layout">
    <div class="workspace-layout__frame">
      <aside class="workspace-layout__sidebar">
        <div class="workspace-layout__brand">
          <span class="workspace-layout__brand-note">Control Archive</span>
          <span class="workspace-layout__brand-title">管理后台</span>
        </div>

        <el-menu :default-active="route.path" router class="workspace-layout__menu">
          <el-menu-item index="/admin/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>控制台</span>
          </el-menu-item>
          <el-menu-item index="/admin/audit">
            <el-icon><Finished /></el-icon>
            <span>审核管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/notice">
            <el-icon><Notification /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/comment">
            <el-icon><ChatDotRound /></el-icon>
            <span>评论管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/rules">
            <el-icon><Setting /></el-icon>
            <span>审核规则</span>
          </el-menu-item>
          <el-menu-item index="/admin/port">
            <el-icon><Connection /></el-icon>
            <span>端口管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/prize">
            <el-icon><TrophyBase /></el-icon>
            <span>奖品配置</span>
          </el-menu-item>
          <el-menu-item index="/admin/score-batch">
            <el-icon><Timer /></el-icon>
            <span>评分批次</span>
          </el-menu-item>
          <el-menu-item index="/admin/roles">
            <el-icon><Key /></el-icon>
            <span>角色权限</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/log">
            <el-icon><Document /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
          <el-menu-item index="/admin/dict">
            <el-icon><List /></el-icon>
            <span>数据字典</span>
          </el-menu-item>
        </el-menu>

      </aside>

      <div class="workspace-layout__main">
        <header class="workspace-layout__topbar">
          <div class="workspace-layout__headline">
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageDescription }}</p>
          </div>

          <div class="workspace-layout__tools">
            <el-badge :value="unreadCount" :hidden="!hasUnread" class="workspace-layout__notify">
              <el-button circle @click="goNotify">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <AppThemeToggle />
            <el-dropdown trigger="click">
              <span class="workspace-layout__user">
                <el-avatar :size="34" :src="userInfo?.avatar || undefined">
                  {{ avatarFallback }}
                </el-avatar>
                <span>{{ userInfo?.realName || '管理员' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人信息</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <main class="workspace-layout__content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer, Finished, Notification, Setting,
  Connection, TrophyBase, Timer, Key, Bell, ArrowDown, User, Document, List, ChatDotRound
} from '@element-plus/icons-vue'
import request from '../api/request'
import AppThemeToggle from '@/components/AppThemeToggle.vue'

function getCachedUserInfo() {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

const route = useRoute()
const router = useRouter()
const userInfo = ref(getCachedUserInfo())
const unreadCount = ref(0)
let unreadTimer: ReturnType<typeof setInterval> | null = null
const hasUnread = computed(() => unreadCount.value > 0)
const avatarFallback = computed(() => userInfo.value?.realName?.charAt?.(0) || '管')
const syncUserInfo = () => {
  userInfo.value = getCachedUserInfo()
}

const descriptions: Record<string, string> = {
  '/admin/dashboard': '用更强的层次和图表节奏总览平台状态。',
  '/admin/audit': '围绕审核流程组织筛选、详情和发布动作。',
  '/admin/notice': '用统一的编辑界面维护公告内容。',
  '/admin/comment': '治理评论秩序，同时保留公开交流的可读性。',
  '/admin/rules': '以结构化方式维护审核规则。',
  '/admin/port': '管理展示端口与预览入口。',
  '/admin/prize': '整理奖项、奖品与榜单呈现逻辑。',
  '/admin/score-batch': '配置评审批次，控制时间与范围。',
  '/admin/roles': '管理角色权限，保持后台秩序清晰。',
  '/admin/users': '集中维护用户与账号状态。',
  '/admin/log': '查看系统操作脉络，保留审计线索。',
  '/admin/dict': '维护字典数据，让配置与内容彼此一致。',
}

const fetchUnread = async () => {
  try {
    const res = await request.get('/admin/notify/unread-count')
    unreadCount.value = Number(res.data?.count ?? 0)
  } catch {
    unreadCount.value = 0
  }
}

onMounted(() => {
  syncUserInfo()
  fetchUnread()
  unreadTimer = setInterval(fetchUnread, 30000)
  window.addEventListener('admin-notify-changed', fetchUnread)
  window.addEventListener('focus', syncUserInfo)
  window.addEventListener('storage', syncUserInfo)
})

onUnmounted(() => {
  if (unreadTimer) clearInterval(unreadTimer)
  window.removeEventListener('admin-notify-changed', fetchUnread)
  window.removeEventListener('focus', syncUserInfo)
  window.removeEventListener('storage', syncUserInfo)
})

const pageTitle = computed(() => {
  const map: Record<string, string> = {
    '/admin/dashboard': '控制台',
    '/admin/audit': '审核管理',
    '/admin/notice': '公告管理',
    '/admin/comment': '评论管理',
    '/admin/rules': '审核规则',
    '/admin/port': '端口管理',
    '/admin/prize': '奖品配置',
    '/admin/score-batch': '评分批次',
    '/admin/roles': '角色权限',
    '/admin/users': '用户管理',
    '/admin/notify': '消息通知',
    '/admin/log': '操作日志',
    '/admin/dict': '数据字典',
  }
  return map[route.path] || '控制台'
})

const pageDescription = computed(() => descriptions[route.path] || '后台保持理性与秩序，但不再保留默认 SaaS 模板气质。')

const goNotify = () => router.push('/admin/notify')

const logout = () => {
  sessionStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  --layout-accent: #7f2436;
}
</style>
