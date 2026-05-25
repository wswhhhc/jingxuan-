import { describe, it, expect } from 'vitest'

/* ============ 公共适配函数（从源文件提取或内联） ============ */

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
        runDescription: item.runDesc || '',
        status: STATUS_MAP[item.status] || 'draft',
        submitterId: item.submitterId,
        submitterName: item.submitterName || '',
        submitTime: item.submitTime || '',
        publishStatus: PUBLISH_STATUS_MAP[item.publishStatus],
        featured: item.featured,
        likeCount: item.likeCount ?? 0,
        viewCount: item.viewCount ?? 0,
        liked: item.liked ?? false,
        tags: item.tags || [],
        memberCount: item.memberCount ?? 0,
        members: (item.members || []).map((m: any) => ({
            id: m.id, studentId: m.studentId,
            studentName: m.studentName || '', studentNo: m.studentNo || '',
            isLeader: m.isLeader === 1 || m.isLeader === true,
        })),
        attachments: (item.attachments || []).map((a: any) => ({
            id: a.id, fileName: a.fileName || '',
            fileType: a.fileType || '', fileUrl: a.fileUrl || '',
        })),
    }
}

function adaptDetailVO(item: any): any {
    return {
        ...adaptWorkEntity(item),
        videoUrl: item.videoUrl || '',
        score: item.avgScore != null ? Number(item.avgScore) : undefined,
        rank: item.rank,
        previewUrl: item.previewUrl,
    }
}

function adaptPageResult(res: any): any {
    return {
        records: (res.records || []).map(adaptWorkEntity),
        total: res.total || 0,
        page: res.pageNum ?? res.page ?? 1,
        pageSize: res.pageSize || 12,
    }
}

/* ============ Mock 后端数据 ============ */

const mockWorkItem = {
    id: '1912345678901234567',
    title: '测试作品',
    summary: '测试简介',
    techStack: 'Java/Spring Boot',
    advisor: '张教授',
    coverUrl: '/uploads/cover.jpg',
    runDesc: '运行说明',
    status: 3,
    submitterId: 100,
    submitterName: '张三',
    submitTime: '2025-03-15 10:00:00',
    publishStatus: 1,
    featured: 1,
    likeCount: 10,
    viewCount: 100,
    liked: true,
    tags: ['Java', 'Vue'],
    memberCount: 2,
    members: [
        { id: 1, studentId: 100, studentName: '张三', studentNo: '2022001', isLeader: 1 },
        { id: 2, studentId: 101, studentName: '李四', studentNo: '2022002', isLeader: 0 },
    ],
    attachments: [
        { id: 1, fileName: 'report.pdf', fileType: 'pdf', fileUrl: '/uploads/report.pdf' },
    ],
    avgScore: '85.50',
    rank: 2,
    previewUrl: 'http://preview.example.com',
}

/* ============ 测试用例 ============ */

describe('API 适配层', () => {

    describe('枚举映射', () => {

        it('后端 status 0/1/2/3 映射到前端字符串', () => {
            expect(STATUS_MAP[0]).toBe('draft')
            expect(STATUS_MAP[1]).toBe('submitted')
            expect(STATUS_MAP[2]).toBe('rejected')
            expect(STATUS_MAP[3]).toBe('approved')
        })

        it('未知 status 降级为 draft', () => {
            const adapted = adaptWorkEntity({ ...mockWorkItem, status: 99 })
            expect(adapted.status).toBe('draft')
        })

        it('publishStatus 0/1/2 映射正确', () => {
            expect(PUBLISH_STATUS_MAP[0]).toBe('unpublished')
            expect(PUBLISH_STATUS_MAP[1]).toBe('published')
            expect(PUBLISH_STATUS_MAP[2]).toBe('offline')
        })

        it('null/undefined publishStatus 映射为 undefined', () => {
            const adapted1 = adaptWorkEntity({ ...mockWorkItem, publishStatus: null })
            expect(adapted1.publishStatus).toBeUndefined()
            const adapted2 = adaptWorkEntity({ ...mockWorkItem, publishStatus: undefined })
            expect(adapted2.publishStatus).toBeUndefined()
        })
    })

    describe('VO 字段适配', () => {

        it('runDesc 转为 runDescription', () => {
            const adapted = adaptWorkEntity(mockWorkItem)
            expect(adapted.runDescription).toBe('运行说明')
            expect(adapted.runDesc).toBeUndefined()
        })

        it('isLeader 数字 1 转布尔 true', () => {
            const adapted = adaptWorkEntity(mockWorkItem)
            const leader = adapted.members.find((m: any) => m.studentId === 100)
            expect(leader.isLeader).toBe(true)
        })

        it('isLeader 数字 0 转布尔 false', () => {
            const adapted = adaptWorkEntity(mockWorkItem)
            const member = adapted.members.find((m: any) => m.studentId === 101)
            expect(member.isLeader).toBe(false)
        })

        it('likeCount/viewCount 空保护', () => {
            const adapted = adaptWorkEntity({ ...mockWorkItem, likeCount: null, viewCount: undefined })
            expect(adapted.likeCount).toBe(0)
            expect(adapted.viewCount).toBe(0)
        })

        it('liked 空保护', () => {
            const adapted = adaptWorkEntity({ ...mockWorkItem, liked: undefined })
            expect(adapted.liked).toBe(false)
        })
    })

    describe('detailVO 适配', () => {

        it('包含 score / rank / previewUrl 额外字段', () => {
            const detail = adaptDetailVO(mockWorkItem)
            expect(detail.score).toBe(85.5)
            expect(detail.rank).toBe(2)
            expect(detail.previewUrl).toBe('http://preview.example.com')
        })

        it('avgScore 为 null 时 score 为 undefined', () => {
            const detail = adaptDetailVO({ ...mockWorkItem, avgScore: null })
            expect(detail.score).toBeUndefined()
        })
    })

    describe('分页适配', () => {

        it('后端 pageNum/pageSize 转为前端 page/pageSize', () => {
            const backendPage = {
                records: [mockWorkItem],
                total: 1,
                pageNum: 2,
                pageSize: 10,
            }
            const adapted = adaptPageResult(backendPage)
            expect(adapted.page).toBe(2)
            expect(adapted.pageSize).toBe(10)
            expect(adapted.total).toBe(1)
            expect(adapted.records).toHaveLength(1)
            expect(adapted.records[0].title).toBe('测试作品')
        })

        it('缺少 pageNum 时回退到 page', () => {
            const adapted = adaptPageResult({ records: [], total: 0, page: 3, pageSize: 20 })
            expect(adapted.page).toBe(3)
        })

        it('records 为空时返回空数组', () => {
            const adapted = adaptPageResult({ records: null, total: 0, pageNum: 1, pageSize: 12 })
            expect(adapted.records).toEqual([])
        })
    })
})
