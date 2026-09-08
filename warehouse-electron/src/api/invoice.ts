import request from '../utils/request'

export function loadByInvoice(invoiceNo: string) {
  return request.get('/outbound/loadByInvoice', { params: { invoiceNo } })
}

export function loadUnpaidOutbound(params: { page?: number; limit?: number; customerId?: number; invoiceNo?: string }) {
  return request.get('/outbound/loadUnpaidOutbound', { params })
}

export function loadAllCustomerForSelect() {
  return request.get('/customer/loadAllCustomerForSelect')
}

export function loadReversals(originalInvoiceNo: string) {
  return request.get('/outbound/loadReversals', { params: { originalInvoiceNo } })
}

export function loadAllBatchStock(params: any) {
  return request.get('/batchStock/loadAllBatchStock', { params })
}

export function loadAllWarehouseForSelect() {
  return request.get('/warehouse/loadAllWarehouseForSelect')
}
