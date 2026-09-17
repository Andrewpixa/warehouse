import request from '@/utils/request'

export function loadVoucher(bizType: string, bizId: number) {
  return request.get('/voucher/loadVoucher', { params: { bizType, bizId } })
}

export function saveAttachment(data: {
  bizType: string
  bizId: number
  attachType: string
  filePath: string
  fileName?: string
}) {
  return request.post('/voucher/saveAttachment', data)
}

export function deleteAttachment(id: number) {
  return request.post('/voucher/deleteAttachment', { id })
}

export function saveSignature(data: {
  bizType: string
  bizId: number
  signRole: string
  signerName: string
  signSource: string
  imagePath: string
}) {
  return request.post('/voucher/saveSignature', data)
}

export function deleteSignature(id: number) {
  return request.post('/voucher/deleteSignature', { id })
}
