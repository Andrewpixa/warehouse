import request from '@/utils/request'

export function loadAllDrug(params: any) {
  return request.get('/drug/loadAllDrug', { params })
}

export function addDrug(data: any) {
  return request.post('/drug/addDrug', data)
}

export function updateDrug(data: any) {
  return request.post('/drug/updateDrug', data)
}

export function deleteDrug(id: number) {
  return request.post('/drug/deleteDrug', null, { params: { id } })
}

export function loadAllDrugForSelect() {
  return request.get('/drug/loadAllDrugForSelect')
}
