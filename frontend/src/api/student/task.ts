import request from '../request'

export interface StudentTask {
  id: number
  userId: number
  batchId: number
  workId: number | null
  title: string
  content: string
  status: number   // 0=待处理 1=已完成 2=已驳回 3=已截止
  createTime: string
  updateTime: string
  batchName?: string
  startTime?: string
  endTime?: string
}

export function getMyTasks() {
  return request.get('/student/tasks')
}

export function completeTask(taskId: number, workId: number) {
  return request.post(`/student/tasks/${taskId}/complete`, null, {
    params: { workId }
  })
}
