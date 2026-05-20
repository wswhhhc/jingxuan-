import request from '../request'

/* ============ 类型定义（前端使用） ============ */

export interface WorkAttachment {
  id?: string | number
  workId?: string | number
  fileName: string
  fileType: string
  fileSize?: number
  fileUrl: string
}

export interface WorkMember {
  id?: string | number
  workId?: string | number
  studentId?: string | number
  studentName: string
  studentNo: string
  className: string
  isLeader: boolean
}

export interface WorkForm {
  id?: string | number
  title: string
  summary: string
  techStack: string
  advisor: string
  coverUrl: string
  videoUrl: string
  runDescription: string
  members: WorkMember[]
  attachments: WorkAttachment[]
}

export interface WorkItem {
  id: string | number
  title: string
  summary: string
  techStack: string
  advisor: string
  coverUrl: string
  videoUrl: string
  runDescription: string
  status: 'draft' | 'submitted' | 'rejected' | 'approved'
  statusLabel?: string
  rejectReason?: string
  submitterId: string | number
  submitterName: string
  submitTime?: string
  createTime: string
  updateTime: string
  attachments: WorkAttachment[]
  members: WorkMember[]
  publishStatus?: 'unpublished' | 'published' | 'offline'
  featured?: number
  score?: number
  rank?: number
  previewUrl?: string
}

export interface WorkListParams {
  page?: number
  pageSize?: number
  status?: string
  keyword?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

/* ============ 后端 ↔ 前端 枚举映射 ============ */

const STATUS_MAP: Record<number, WorkItem['status']> = {
  0: 'draft',
  1: 'submitted',
  2: 'rejected',
  3: 'approved',
}
const STATUS_REV_MAP: Record<string, number | undefined> = {
  draft: 0,
  submitted: 1,
  rejected: 2,
  approved: 3,
}
const PUBLISH_STATUS_MAP: Record<number, WorkItem['publishStatus'] | undefined> = {
  0: 'unpublished',
  1: 'published',
  2: 'offline',
}

function toFrontendStatus(s: number): WorkItem['status'] {
  return STATUS_MAP[s] || 'draft'
}
function toBackendStatus(s?: string): number | undefined {
  return s ? STATUS_REV_MAP[s] : undefined
}
function toFrontendPublishStatus(s: number): WorkItem['publishStatus'] | undefined {
  return PUBLISH_STATUS_MAP[s]
}

/* ============ 后端响应 → 前端 WorkItem 适配 ============ */

/**
 * 适配 WorkListVO（学生作品列表项）
 */
function adaptListVO(item: any): WorkItem {
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    techStack: item.techStack || '',
    advisor: item.advisor || '',
    coverUrl: item.coverUrl || '',
    videoUrl: item.videoUrl || '',
    runDescription: item.runDesc || '',
    status: toFrontendStatus(item.status),
    statusLabel: item.statusLabel,
    rejectReason: item.rejectReason,
    submitterId: item.submitterId,
    submitterName: item.submitterName || '',
    submitTime: item.submitTime || '',
    createTime: item.createTime || '',
    updateTime: item.updateTime || '',
    attachments: (item.attachments || []).map(adaptAttachment),
    members: (item.members || []).map(adaptMember),
    publishStatus: toFrontendPublishStatus(item.publishStatus),
    featured: item.featured,
    score: item.avgScore != null ? Number(item.avgScore) : undefined,
    rank: item.rank,
  }
}

/**
 * 适配 WorkDetailVO（作品详情）
 */
function adaptDetailVO(item: any): WorkItem {
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    techStack: item.techStack || '',
    advisor: item.advisor || '',
    coverUrl: item.coverUrl || '',
    videoUrl: item.videoUrl || '',
    runDescription: item.runDesc || '',
    status: toFrontendStatus(item.status),
    statusLabel: item.statusLabel,
    rejectReason: item.rejectReason,
    submitterId: item.submitterId,
    submitterName: item.submitterName || '',
    submitTime: item.submitTime || '',
    createTime: item.createTime || '',
    updateTime: item.updateTime || '',
    attachments: (item.attachments || []).map(adaptAttachment),
    members: (item.members || []).map(adaptMember),
    publishStatus: toFrontendPublishStatus(item.publishStatus),
    featured: item.featured,
    score: item.avgScore != null ? Number(item.avgScore) : undefined,
    rank: item.rank,
  }
}

function adaptAttachment(a: any): WorkAttachment {
  return {
    id: a.id,
    workId: a.workId,
    fileName: a.fileName || '',
    fileType: a.fileType || '',
    fileSize: a.fileSize,
    fileUrl: a.fileUrl || '',
  }
}

function adaptMember(m: any): WorkMember {
  return {
    id: m.id,
    workId: m.workId,
    studentId: m.studentId,
    studentName: m.studentName || '',
    studentNo: m.studentNo || '',
    className: m.className || '',
    isLeader: m.isLeader === 1 || m.isLeader === true,
  }
}

/* ============ 前端表单 → 后端请求体适配 ============ */

function toCreateRequest(form: WorkForm) {
  const attachmentIds = form.attachments.filter(a => a.id).map(a => a.id)
  console.log('[DEBUG] toCreateRequest attachments:', form.attachments)
  console.log('[DEBUG] toCreateRequest attachmentIds:', attachmentIds)
  return {
    title: form.title,
    summary: form.summary,
    techStack: form.techStack,
    advisor: form.advisor,
    coverUrl: form.coverUrl,
    videoUrl: form.videoUrl,
    runDesc: form.runDescription,
    members: form.members.map((m) => ({
      studentName: m.studentName,
      studentNo: m.studentNo,
      className: m.className,
      isLeader: m.isLeader ? 1 : 0,
    })),
    attachmentIds,
  }
}

function toUpdateRequest(form: WorkForm) {
  const attachmentIds = form.attachments.filter(a => a.id).map(a => a.id)
  console.log('[DEBUG] toUpdateRequest attachments:', form.attachments)
  console.log('[DEBUG] toUpdateRequest attachmentIds:', attachmentIds)
  return {
    title: form.title,
    summary: form.summary,
    techStack: form.techStack,
    advisor: form.advisor,
    coverUrl: form.coverUrl,
    videoUrl: form.videoUrl,
    runDesc: form.runDescription,
    members: form.members.map((m) => ({
      id: m.id,
      studentId: m.studentId,
      studentName: m.studentName,
      studentNo: m.studentNo,
      className: m.className,
      isLeader: m.isLeader ? 1 : 0,
    })),
    attachmentIds,
  }
}

/* ============ 分页响应适配 ============ */

function adaptPageResult(res: any): PageResult<WorkItem> {
  return {
    records: (res.records || []).map(adaptListVO),
    total: res.total || 0,
    page: res.pageNum ?? res.page ?? 1,
    pageSize: res.pageSize || 10,
  }
}

/* ============ API 函数 ============ */

export function createWork(data: WorkForm) {
  return request({
    url: '/student/works',
    method: 'post',
    data: toCreateRequest(data),
  })
}

export function updateWork(id: string | number, data: WorkForm) {
  return request({
    url: `/student/works/${id}`,
    method: 'put',
    data: toUpdateRequest(data),
  })
}

export function deleteWork(id: string | number) {
  return request({
    url: `/student/works/${id}`,
    method: 'delete',
  })
}

export function submitWork(id: string | number) {
  return request({
    url: `/student/works/${id}/submit`,
    method: 'post',
  })
}

export async function getMyWorks(params: WorkListParams) {
  const queryParams: Record<string, any> = {
    page: params.page || 1,
    size: params.pageSize || 10,
  }
  const statusNum = toBackendStatus(params.status)
  if (statusNum !== undefined) queryParams.status = statusNum

  const res = await request({
    url: '/student/works',
    method: 'get',
    params: queryParams,
  })
  // 适配后端响应：包住 data 中的 PageResult
  res.data = adaptPageResult(res.data)
  return res
}

export async function getWorkDetail(id: string | number) {
  const res = await request({
    url: `/student/works/${id}`,
    method: 'get',
  })
  res.data = adaptDetailVO(res.data)
  return res
}

// ====== 文件上传 ======

export function uploadFile(file: File, workId?: string | number) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    params: {
      workId: workId || undefined,
    },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
