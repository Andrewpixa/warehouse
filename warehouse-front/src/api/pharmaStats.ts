import request from '@/utils/request'

export function loadPurchaseStats(params: { startDate?: string; endDate?: string }) {
  return request.get('/pharmaStats/purchaseStats', { params })
}

export function loadSalesStats(params: { startDate?: string; endDate?: string }) {
  return request.get('/pharmaStats/salesStats', { params })
}
