import request from '@/utils/request'

export function loadMonthlyCloseStatement(yearMonth: string) {
  return request.get('/monthlyClose/loadStatement', { params: { yearMonth } })
}

export function confirmMonthlyClose(yearMonth: string) {
  return request.post('/monthlyClose/confirm', { yearMonth })
}
