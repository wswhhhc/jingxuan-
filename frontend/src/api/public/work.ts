import request from '../request'

export interface PublicWorkListParams {
  page?: number
  pageSize?: number
  keyword?: string
  techStack?: string
  classId?: number
  tagIds?: number[]
  submitTimeBegin?: string
  submitTimeEnd?: string
  sortBy?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface TagItem {
  id: number
  name: string
  color: string
  type: string
}

/* ============ 状态映射（与 student/work.ts 保持一致） ============ */

const STATUS_MAP: Record<number, string> = { 0: 'draft', 1: 'submitted', 2: 'rejected', 3: 'approved' }
const PUBLISH_STATUS_MAP: Record<number, string | undefined> = { 0: 'unpublished', 1: 'published', 2: 'offline' }

function adaptWorkEntity(item: any): any {
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    techStack: item.techStack || '',
    advisor: item.advisor || '',
    coverUrl: item.coverUrl || '',
    videoUrl: item.videoUrl || '',
    runDescription: item.runDesc || '',
    status: STATUS_MAP[item.status] || 'draft',
    submitterId: item.submitterId,
    submitterName: item.submitterName || '',
    submitTime: item.submitTime || '',
    createTime: item.createTime || '',
    updateTime: item.updateTime || '',
    attachments: (item.attachments || []).map((a: any) => ({
      id: a.id, workId: a.workId, fileName: a.fileName || '',
      fileType: a.fileType || '', fileSize: a.fileSize, fileUrl: a.fileUrl || '',
    })),
    members: (item.members || []).map((m: any) => ({
      id: m.id, workId: m.workId, studentId: m.studentId,
      studentName: m.studentName || '', studentNo: m.studentNo || '',
      className: m.className || '',
      isLeader: m.isLeader === 1 || m.isLeader === true,
    })),
    publishStatus: PUBLISH_STATUS_MAP[item.publishStatus],
    featured: item.featured,
    score: item.avgScore != null ? Number(item.avgScore) : undefined,
    rank: item.rank,
    likeCount: item.likeCount ?? 0,
    viewCount: item.viewCount ?? 0,
    liked: item.liked ?? false,
    tags: item.tags || [],
  }
}

function adaptDetailVO(item: any): any {
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    techStack: item.techStack || '',
    advisor: item.advisor || '',
    coverUrl: item.coverUrl || '',
    videoUrl: item.videoUrl || '',
    runDescription: item.runDesc || '',
    status: STATUS_MAP[item.status] || 'draft',
    statusLabel: item.statusLabel,
    submitterId: item.submitterId,
    submitterName: item.submitterName || '',
    submitTime: item.submitTime || '',
    createTime: item.createTime || '',
    updateTime: item.updateTime || '',
    attachments: (item.attachments || []).map((a: any) => ({
      id: a.id, workId: a.workId, fileName: a.fileName || '',
      fileType: a.fileType || '', fileSize: a.fileSize, fileUrl: a.fileUrl || '',
    })),
    members: (item.members || []).map((m: any) => ({
      id: m.id, workId: m.workId, studentId: m.studentId,
      studentName: m.studentName || '', studentNo: m.studentNo || '',
      className: m.className || '',
      isLeader: m.isLeader === 1 || m.isLeader === true,
    })),
    publishStatus: PUBLISH_STATUS_MAP[item.publishStatus],
    featured: item.featured,
    score: item.avgScore != null ? Number(item.avgScore) : undefined,
    rank: item.rank,
    previewUrl: item.previewUrl,
    likeCount: item.likeCount ?? 0,
    viewCount: item.viewCount ?? 0,
    liked: item.liked ?? false,
    tags: item.tags || [],
  }
}

function adaptPageResult(res: any): PageResult<any> {
  return {
    records: (res.records || []).map(adaptWorkEntity),
    total: res.total || 0,
    page: res.pageNum ?? res.page ?? 1,
    pageSize: res.pageSize || 12,
  }
}

/* ============ API ============ */

export async function getPublicWorkList(params: PublicWorkListParams) {
  const res = await request({
    url: '/public/works',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.pageSize || 12,
      keyword: params.keyword || undefined,
      techStack: params.techStack || undefined,
      classId: params.classId || undefined,
      tagIds: params.tagIds?.length ? params.tagIds.join(',') : undefined,
      submitTimeBegin: params.submitTimeBegin || undefined,
      submitTimeEnd: params.submitTimeEnd || undefined,
    },
  })
  res.data = adaptPageResult(res.data)
  return res
}

export interface PublicClassItem {
  id: number
  dictValue: string
  dictLabel: string
}

export async function getPublicClassList() {
  return request.get<PublicClassItem[]>('/public/classes')
}

export async function getPublicWorkDetail(id: string | number) {
  const res = await request({
    url: `/public/works/${id}`,
    method: 'get',
  })
  res.data = adaptDetailVO(res.data)
  return res
}

export async function getPublicTagList() {
  return request.get<TagItem[]>('/public/tags')
}

export async function toggleLike(id: string | number) {
  return request.post<{ liked: boolean; likeCount: number }>(`/works/${id}/like`)
}

export async function getLikeStatus(id: string | number) {
  return request.get<{ liked: boolean; likeCount: number }>(`/public/works/${id}/like-status`)
}
