import request from '@/utils/request'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function loadAllPurchase(params: any) {
  return request.get('/purchase/loadAllPurchase', { params })
}

export function loadPurchaseDetail(id: number) {
  return request.get('/purchase/loadPurchaseDetail', { params: { id } })
}

export function savePurchase(data: any) {
  return request.post('/purchase/savePurchase', data, { headers: jsonHeaders })
}

export function confirmPurchase(id: number) {
  return request.post('/purchase/confirmPurchase', null, { params: { id } })
}

export function deletePurchase(id: number) {
  return request.post('/purchase/deletePurchase', null, { params: { id } })
}

export function previewSupplierInvoice(supplierId: number, bizDate?: string) {
  return request.get('/purchase/previewSupplierInvoice', { params: { supplierId, bizDate } })
}

export function receiveSupplierInvoice(id: number) {
  return request.post('/purchase/receiveSupplierInvoice', null, { params: { id } })
}
