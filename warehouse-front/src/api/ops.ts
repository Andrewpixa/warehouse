import request from '@/utils/request'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function loadAllOps(params: any) {
  return request.get('/ops/loadAll', { params })
}

export function loadOpsDetail(id: number) {
  return request.get('/ops/loadDetail', { params: { id } })
}

export function saveOps(data: any) {
  return request.post('/ops/save', data, { headers: jsonHeaders })
}

export function confirmOps(id: number, action?: string) {
  return request.post('/ops/confirm', null, { params: { id, action } })
}

export function deleteOps(id: number) {
  return request.post('/ops/delete', null, { params: { id } })
}

export function loadOpsWorkbench() {
  return request.get('/ops/workbench')
}

export function loadPrintPack(invoiceNo: string) {
  return request.get('/ops/printPack', { params: { invoiceNo } })
}

export function loadMakerAccounts() {
  return request.get('/ops/makerAccounts')
}

export function makerQuery(params: { loginName: string; password: string; from?: string; to?: string }) {
  return request.get('/ops/makerQuery', { params })
}
