import request from '@/utils/request'

export function loadAllBatchStock(params: any) {
  return request.get('/batchStock/loadAllBatchStock', { params })
}
