import request from '../request'

export interface PublicRankItem {
  rankNo: number
  workId: number
  workTitle: string
  techStack: string
  coverUrl: string
  advisor: string
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

export function getRankingList(params: { batchId?: number; topN?: number; techStack?: string }) {
  return request.get<PublicRankItem[]>('/public/ranking/list', { params })
}

export function getRankingBatches() {
  return request.get('/public/ranking/batches')
}

export function getRankingCategories(batchId?: number) {
  return request.get<string[]>('/public/ranking/categories', { params: { batchId } })
}
