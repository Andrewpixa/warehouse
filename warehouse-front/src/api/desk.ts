import request from '@/utils/request'

export function loadDeskSummary() {
  return request.get('/desk/summary')
}

export function loadPendingSuppliers(params: any) {
  return request.get('/quality/pendingSuppliers', { params })
}

export function approveFirstCamp(id: number) {
  return request.post('/quality/approveFirstCamp', null, { params: { id } })
}

export function loadPendingBatches(params: any) {
  return request.get('/quality/pendingBatches', { params })
}

export function setBatchQuality(id: number, qualityStatus: string) {
  return request.post('/quality/setBatchQuality', null, { params: { id, qualityStatus } })
}
