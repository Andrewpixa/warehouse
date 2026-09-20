import request from '@/utils/request'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function loadAllBankReceipt(params: any) {
  return request.get('/bankReceipt/loadAll', { params })
}

export function saveBankReceipt(data: any) {
  return request.post('/bankReceipt/save', data, { headers: jsonHeaders })
}
