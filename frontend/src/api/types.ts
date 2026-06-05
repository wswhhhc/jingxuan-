/* 与后端 DTO 对齐的共享类型 */

/** 作品列表 VO — WorkListVO */
export interface WorkListVO {
  id: number
  title: string
  techStack: string
  coverUrl: string
  previewUrl: string
  status: number          // 0=草稿 1=已提交 2=已驳回 3=已通过
  statusLabel: string
  submitterName: string
  submitTime: string
  publishStatus: number   // 0=未发布 1=已发布 2=已下线
  featured: number
  memberCount: number
  scored?: boolean
}

/** 作品详情 VO — WorkDetailVO */
export interface WorkDetailVO {
  id: number
  title: string
  summary: string
  techStack: string
  advisor: string
  coverUrl: string
  videoUrl: string
  runDesc: string
  status: number
  statusLabel: string
  submitterId: number
  submitterName: string
  submitTime: string
  batchId: number
  publishStatus: number
  publishStatusLabel: string
  featured: number
  previewUrl: string
  avgScore: string
  members: {
    id: number
    studentId: number
    studentName: string
    studentNo: string
    className: string
    isLeader: number
  }[]
  attachments: {
    id: number
    fileName: string
    fileType: string
    fileUrl: string
  }[]
}

/** 共享常量 */
export const IMAGE_TYPES: readonly string[] = ['jpg', 'jpeg', 'png', 'gif', 'webp']
export const VIDEO_TYPES: readonly string[] = ['mp4', 'avi', 'mov', 'mkv', 'webm']
export const MEDIA_TYPES: readonly string[] = [...IMAGE_TYPES, ...VIDEO_TYPES]

/** 用户信息（登录后缓存，所有角色通用） */
export interface UserInfo {
  id: number
  username: string
  realName: string
  roleId: number
  roleCode: string
  roleName: string
  className: string
  classId?: number
  avatar?: string
  email?: string
  phone?: string
  firstLogin?: boolean
}
