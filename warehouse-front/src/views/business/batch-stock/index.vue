<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">批号库存</h1>
      <p class="page-header-desc">一物一批一库一质量状态；采购入库确认增加、销售出库确认扣减</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="品种">
          <el-input v-model="searchParams.drugName" placeholder="编号/通用名/商品名" clearable />
        </el-form-item>
        <el-form-item label="批号">
          <el-input v-model="searchParams.batchNo" placeholder="批号" clearable />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchParams.warehouseId" clearable placeholder="仓库" style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="质量状态">
          <el-select v-model="searchParams.qualityStatus" clearable placeholder="全部" style="width: 120px">
            <el-option label="合格" value="合格" />
            <el-option label="不合格" value="不合格" />
          </el-select>
        </el-form-item>
        <el-form-item label="效期">
          <el-select v-model="searchParams.expireFilter" clearable placeholder="全部" style="width: 130px">
            <el-option label="近效期90天" value="near" />
            <el-option label="已过期" value="expired" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllBatchStock" :search-params="searchParams">
        <el-table-column prop="drugId" label="品种编号" width="100" />
        <el-table-column prop="drugName" label="品种" min-width="140" />
        <el-table-column prop="drugSpec" label="规格" width="120" />
        <el-table-column prop="batchNo" label="批号" width="140" />
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="qty" label="数量" width="100" />
        <el-table-column prop="qualityStatus" label="质量状态" width="100" />
        <el-table-column prop="expireDate" label="有效期至" width="120" />
        <el-table-column prop="lastMoveAt" label="最后出入库" width="180" />
      </CrudTable>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllBatchStock } from '@/api/batchStock'
import { loadAllWarehouseForSelect } from '@/api/warehouse'

const tableRef = ref()
const warehouses = ref<any[]>([])
const searchParams = reactive({
  drugName: '',
  batchNo: '',
  warehouseId: undefined as number | undefined,
  qualityStatus: '',
  expireFilter: ''
})

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.drugName = ''
  searchParams.batchNo = ''
  searchParams.warehouseId = undefined
  searchParams.qualityStatus = ''
  searchParams.expireFilter = ''
}

onMounted(async () => {
  const res: any = await loadAllWarehouseForSelect()
  warehouses.value = res.data || []
})
</script>
