import { ref } from 'vue'

/**
 * API 分页列表查询 composable
 *
 * 统一管理 loading / list / total 状态和 try-finally 模板代码。
 * page/size 由各视图自行管理（ref 或 query 对象），通过 params 传入 loadList。
 *
 * @example
 *   const { loading, list, total, loadList } = useApiList<Item>(getList)
 *   const reload = () => loadList({ page: page.value, size: size.value, keyword: keyword.value })
 *   onMounted(reload)
 */
export function useApiList<T>(
  fetchFn: (params: Record<string, any>) => Promise<any>,
  mapResponse?: (data: any) => { records: T[]; total: number }
) {
  const loading = ref(false)
  const list = ref<T[]>([])
  const total = ref(0)

  const loadList = async (params: Record<string, any> = {}) => {
    loading.value = true
    try {
      const res = await fetchFn(params)
      const data = res?.data
      if (mapResponse) {
        const mapped = mapResponse(data)
        list.value = mapped.records
        total.value = mapped.total
      } else if (data && typeof data === 'object' && 'records' in data) {
        list.value = (data.records ?? []) as T[]
        total.value = (data.total as number) || 0
      } else if (Array.isArray(data)) {
        list.value = data as T[]
        total.value = data.length
      } else {
        list.value = []
        total.value = 0
      }
    } finally {
      loading.value = false
    }
  }

  return { loading, list, total, loadList }
}
