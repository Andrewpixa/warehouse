import request from '@/utils/request'

// ==================== 仓库 ====================

export function loadAllWarehouse(params: any) {
  return request.get('/warehouse/loadAllWarehouse', { params })
}

export function loadAllWarehouseForSelect() {
  return request.get('/warehouse/loadAllWarehouseForSelect')
}

export function addWarehouse(data: any) {
  return request.post('/warehouse/addWarehouse', data)
}

export function updateWarehouse(data: any) {
  return request.post('/warehouse/updateWarehouse', data)
}

export function setDefaultWarehouse(id: number) {
  return request.post('/warehouse/setDefaultWarehouse', null, { params: { id } })
}

export function deleteWarehouse(id: number) {
  return request.post('/warehouse/deleteWarehouse', null, { params: { id } })
}

// ==================== 库位 ====================

export function loadLocations(warehouseId: number) {
  return request.get('/warehouse/loadLocations', { params: { warehouseId } })
}

export function addLocation(data: any) {
  return request.post('/warehouse/addLocation', data)
}

export function updateLocation(data: any) {
  return request.post('/warehouse/updateLocation', data)
}

export function deleteLocation(id: number) {
  return request.post('/warehouse/deleteLocation', null, { params: { id } })
}

// ==================== 分仓库存 ====================

export function loadGoodsStock(params: any) {
  return request.get('/warehouse/loadGoodsStock', { params })
}

export function loadStockByGoodsId(goodsid: number) {
  return request.get('/warehouse/loadStockByGoodsId', { params: { goodsid } })
}

// ==================== 分仓预警 ====================

export function loadWarehouseWarnings() {
  return request.get('/warehouse/loadWarehouseWarnings')
}

export function loadWarnRules(params?: any) {
  return request.get('/warehouse/loadWarnRules', { params })
}

export function saveWarnRule(data: any) {
  return request.post('/warehouse/saveWarnRule', data)
}

export function deleteWarnRule(id: number) {
  return request.post('/warehouse/deleteWarnRule', null, { params: { id } })
}
