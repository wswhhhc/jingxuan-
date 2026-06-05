export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export const STATUS_MAP: Record<number, 'draft' | 'submitted' | 'rejected' | 'approved'> = {
  0: 'draft',
  1: 'submitted',
  2: 'rejected',
  3: 'approved',
}

export const STATUS_REV_MAP: Record<string, number | undefined> = {
  draft: 0,
  submitted: 1,
  rejected: 2,
  approved: 3,
}

export const PUBLISH_STATUS_MAP: Record<number, 'unpublished' | 'published' | 'offline' | undefined> = {
  0: 'unpublished',
  1: 'published',
  2: 'offline',
}

export function toFrontendStatus(status: number) {
  return STATUS_MAP[status] || 'draft'
}

export function toBackendStatus(status?: string): number | undefined {
  return status ? STATUS_REV_MAP[status] : undefined
}

export function toFrontendPublishStatus(status: number) {
  return PUBLISH_STATUS_MAP[status]
}

export function adaptAttachment(attachment: any) {
  return {
    id: attachment.id,
    workId: attachment.workId,
    fileName: attachment.fileName || '',
    fileType: attachment.fileType || '',
    fileSize: attachment.fileSize,
    fileUrl: attachment.fileUrl || '',
  }
}

export function adaptMember(member: any) {
  return {
    id: member.id,
    workId: member.workId,
    studentId: member.studentId,
    studentName: member.studentName || '',
    studentNo: member.studentNo || '',
    className: member.className || '',
    isLeader: member.isLeader === 1 || member.isLeader === true,
  }
}

export function adaptWorkVO(item: any) {
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
    previewUrl: item.previewUrl || '',
    likeCount: item.likeCount ?? 0,
    viewCount: item.viewCount ?? 0,
    liked: item.liked ?? false,
    tags: item.tags || [],
  }
}

export function adaptPageResult<T>(res: any, adapter: (item: any) => T, defaultPageSize = 10): PageResult<T> {
  return {
    records: (res.records || []).map(adapter),
    total: res.total || 0,
    page: res.pageNum ?? res.page ?? 1,
    pageSize: res.pageSize || defaultPageSize,
  }
}
