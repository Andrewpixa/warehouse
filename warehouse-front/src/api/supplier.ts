import request from '@/utils/request'

export function loadAllSupplier(params: any) {
  return request.get('/supplier/loadAllSupplier', { params })
}

export function addSupplier(data: any) {
  return request.post('/supplier/addSupplier', data)
}

export function updateSupplier(data: any) {
  return request.post('/supplier/updateSupplier', data)
}

export function deleteSupplier(id: number) {
  return request.post('/supplier/deleteSupplier', null, { params: { id } })
}

export function loadAllSupplierForSelect() {
  return request.get('/supplier/loadAllSupplierForSelect')
}
