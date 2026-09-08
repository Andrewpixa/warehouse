import request from '@/utils/request'

export function loadDailyCloseChecklist(bizDate: string) {
  return request.get('/dailyClose/loadChecklist', { params: { bizDate } })
}

export function confirmDailyClose(bizDate: string) {
  return request.post('/dailyClose/confirm', { bizDate })
}
