import type { RouteRecordRaw } from 'vue-router'

const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('../../layout/PublicLayout.vue'),
    meta: { noAuth: true },
    redirect: '/works',
    children: [
      {
        path: 'works',
        name: 'PublicWorkList',
        component: () => import('../../views/public/WorkList.vue'),
        meta: { title: '作品展示' },
      },
      {
        path: 'works/:id',
        name: 'PublicWorkDetail',
        component: () => import('../../views/public/WorkDetail.vue'),
        meta: { title: '作品详情' },
      },
      {
        path: 'ranking',
        name: 'PublicRanking',
        component: () => import('../../views/public/Ranking.vue'),
        meta: { title: '排行榜' },
      },
    ],
  },
]

export default publicRoutes
