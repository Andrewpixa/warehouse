import request from '@/utils/request'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function loadAllSurplus(params: any) {
  return request.get('/surplus/loadAllSurplus', { params })
}

export function loadSurplusDetail(id: number) {
  return request.get('/surplus/loadSurplusDetail', { params: { id } })
}

export function loadWarehouseBatches(warehouseId: number) {
  return request.get('/surplus/loadWarehouseBatches', { params: { warehouseId } })
}

export function saveSurplus(data: any) {
  return request.post('/surplus/saveSurplus', data, { headers: jsonHeaders })
}

export function confirmSurplus(id: number) {
  return request.post('/surplus/confirmSurplus', null, { params: { id } })
}

export function deleteSurplus(id: number) {
  return request.post('/surplus/deleteSurplus', null, { params: { id } })
}
