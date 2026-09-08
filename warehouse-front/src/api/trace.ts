import request from '@/utils/request'

export function loadTraceInvoice(invoiceNo: string) {
  return request.get('/trace/loadInvoice', { params: { invoiceNo } })
}

export function loadTraceByInvoice(invoiceNo: string) {
  return request.get('/trace/loadByInvoice', { params: { invoiceNo } })
}

export function loadTraceBySpdid(spdid: string) {
  return request.get('/trace/loadBySpdid', { params: { spdid } })
}

export function addTraceCodes(data: any) {
  return request.post('/trace/addCodes', data, {
    headers: { 'Content-Type': 'application/json' }
  })
}

export function previewParse(code: string) {
  return request.get('/trace/previewParse', { params: { code } })
}

export function parseTraceCodes(data: { spdid: string; code: string; bizType?: string }) {
  return request.post('/trace/parseCodes', data, {
    headers: { 'Content-Type': 'application/json' }
  })
}
