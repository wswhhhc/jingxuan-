import request from '../request'

export interface RankingItem {
  rankNo: number
  workId: number
  workTitle: string
  techStack: string
  coverUrl: string
  avgScore: number
  avgInnovation: number
  avgDifficulty: number
  avgCompletion: number
  avgPracticality: number
  teacherCount: number
  submitTime: string
  /** 获奖等级文案，如「一等奖」 */
  rewardLevel: string
  /** 兼容字段，与 rewardLevel 相同 */
  rewardName: string
  /** 奖品说明，如「荣誉证书 + 500元京东卡」 */
  prizeName: string
}

export interface CategoryItem {
  label: string
  value: string
}

export function getRanking(params: { batchId?: number; type?: string; topN?: number }) {
  return request.get<RankingItem[]>('/teacher/ranking/list', { params })
}

export function getRankingBatches() {
  return request.get('/teacher/ranking/batches')
}

export function getRankingCategories(batchId?: number) {
  return request.get<CategoryItem[]>('/teacher/ranking/categories', { params: { batchId } })
}

export function refreshRanking(batchId: number) {
  return request.post(`/teacher/ranking/refresh/${batchId}`)
}
