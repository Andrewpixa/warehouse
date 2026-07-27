import request from '@/utils/request'

export function loadAllTransfer(params: any) {
  return request.get('/transfer/loadAllTransfer', { params })
}

export function loadTransferItems(transferId: number) {
  return request.get('/transfer/loadTransferItems', { params: { transferId } })
}

export function createTransfer(data: any) {
  // 嵌套明细结构必须走 JSON（默认拦截器会把 POST 对象转 form 表单）
  return request.post('/transfer/createTransfer', JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' }
  })
}

export function shipTransfer(id: number) {
  return request.post('/transfer/shipTransfer', null, { params: { id } })
}

export function receiveTransfer(id: number) {
  return request.post('/transfer/receiveTransfer', null, { params: { id } })
}

export function cancelTransfer(id: number) {
  return request.post('/transfer/cancelTransfer', null, { params: { id } })
}

export function deleteTransfer(id: number) {
  return request.post('/transfer/deleteTransfer', null, { params: { id } })
}
