import request from '@/utils/request'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function loadAllOutbound(params: any) {
  return request.get('/outbound/loadAllOutbound', { params })
}

export function loadOutboundDetail(id: number) {
  return request.get('/outbound/loadOutboundDetail', { params: { id } })
}

export function loadByInvoice(invoiceNo: string) {
  return request.get('/outbound/loadByInvoice', { params: { invoiceNo } })
}

export function saveOutbound(data: any) {
  return request.post('/outbound/saveOutbound', data, { headers: jsonHeaders })
}

export function confirmOutbound(data: {
  id: number
  shipTime?: string
  einvoiceNo?: string
  einvoicePath?: string
}) {
  return request.post('/outbound/confirmOutbound', data)
}

export function deleteOutbound(id: number) {
  return request.post('/outbound/deleteOutbound', null, { params: { id } })
}

export function loadPendingReceipt(params: any) {
  return request.get('/outbound/loadPendingReceipt', { params })
}

export function loadReceiptDetail(id: number) {
  return request.get('/outbound/loadReceiptDetail', { params: { id } })
}

export function confirmReceipt(data: any) {
  return request.post('/outbound/confirmReceipt', data, { headers: jsonHeaders })
}

export function loadUnpaidOutbound(params: any) {
  return request.get('/outbound/loadUnpaidOutbound', { params })
}

export function markPaid(data: { id: number; paidStatus: string; paidAmount?: number }) {
  return request.post('/outbound/markPaid', data)
}

export function saveReversal(data: any) {
  return request.post('/outbound/saveReversal', data, { headers: jsonHeaders })
}

export function loadReversals(originalInvoiceNo: string) {
  return request.get('/outbound/loadReversals', { params: { originalInvoiceNo } })
}
