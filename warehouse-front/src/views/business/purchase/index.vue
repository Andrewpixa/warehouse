<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">采购入库单</h1>
      <p class="page-header-desc">采购开草稿，验收合格后由具备「确认入库」权限的保管员过账</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="单号">
          <el-input v-model="searchParams.orderNo" placeholder="采购入库单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="草稿" value="草稿" />
            <el-option label="已确认" value="已确认" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllPurchase" :search-params="searchParams">
        <el-table-column prop="orderNo" label="采购单号" min-width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="140" />
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="bizDate" label="入库日期" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column prop="checkResult" label="验收" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '已确认' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '草稿' && canCreate" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === '草稿' && canConfirm" type="success" link @click="handleConfirm(row)">确认入库</el-button>
            <el-button v-if="row.status === '草稿' && canDelete" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button v-if="canCreate" type="primary" @click="openAdd">
            <el-icon><Plus /></el-icon> 开采购入库单
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="980px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="供应商" required>
              <el-select v-model="form.supplierId" filterable placeholder="选择供应商" style="width: 100%" :disabled="readonly">
                <el-option v-for="s in suppliers" :key="s.id" :label="`${s.id} ${s.name}`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" required>
              <el-select v-model="form.warehouseId" filterable placeholder="选择仓库" style="width: 100%" :disabled="readonly">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库日期" required>
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="验收结论">
              <el-select v-model="form.checkResult" clearable placeholder="待验收" style="width: 100%" :disabled="readonly">
                <el-option label="合格" value="合格" />
                <el-option label="不合格" value="不合格" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" :disabled="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="item-toolbar" v-if="!readonly">
        <el-button type="primary" size="small" @click="addItem">添加明细</el-button>
      </div>
      <el-table :data="form.items" border size="small">
        <el-table-column label="品种" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.drugId" filterable placeholder="药品/器械" :disabled="readonly" style="width: 100%">
              <el-option v-for="d in drugs" :key="d.id" :label="`${d.id} ${d.genericName} ${d.spec || ''}`" :value="d.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="批号" width="130">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="效期" width="150">
          <template #default="{ row }">
            <el-date-picker v-model="row.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="入库数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.stockInQty" :min="0" :controls="false" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="采购价" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.purchasePrice" :min="0" :precision="2" :controls="false" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="质量" width="110">
          <template #default="{ row }">
            <el-select v-model="row.qualityStatus" :disabled="readonly" style="width: 100%">
              <el-option label="合格" value="合格" />
              <el-option label="待验" value="待验" />
              <el-option label="不合格" value="不合格" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="" width="60">
          <template #default="{ $index }">
            <el-button type="danger" link @click="form.items.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllPurchase, loadPurchaseDetail, savePurchase, confirmPurchase, deletePurchase } from '@/api/purchase'
import { loadAllSupplierForSelect } from '@/api/supplier'
import { loadAllWarehouseForSelect } from '@/api/warehouse'
import { loadAllDrugForSelect } from '@/api/drug'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()
const canCreate = computed(() => hasPermission('inport:create'))
const canConfirm = computed(() => hasPermission('inport:confirm'))
const canDelete = computed(() => hasPermission('inport:delete'))

const tableRef = ref()
const suppliers = ref<any[]>([])
const warehouses = ref<any[]>([])
const drugs = ref<any[]>([])
const dialogVisible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const dialogTitle = ref('开采购入库单')

const searchParams = reactive({
  orderNo: '',
  status: ''
})

const emptyForm = () => ({
  id: undefined as number | undefined,
  supplierId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  bizDate: '',
  checkResult: '' as string,
  remark: '',
  items: [] as any[]
})

const form = reactive(emptyForm())

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.orderNo = ''
  searchParams.status = ''
}

const resetForm = (data?: any) => {
  Object.assign(form, emptyForm(), data || {})
  if (!form.items) form.items = []
}

const openAdd = () => {
  dialogTitle.value = '开采购入库单'
  readonly.value = false
  resetForm({
    bizDate: new Date().toISOString().slice(0, 10),
    items: [{ qualityStatus: '合格', stockInQty: 1, purchasePrice: 0 }]
  })
  dialogVisible.value = true
}

const fillForm = (data: any) => {
  resetForm({
    id: data.id,
    supplierId: data.supplierId,
    warehouseId: data.warehouseId,
    bizDate: data.bizDate,
    checkResult: data.checkResult || '',
    remark: data.remark,
    items: (data.items || []).map((it: any) => ({ ...it }))
  })
}

const openEdit = async (row: any) => {
  const res: any = await loadPurchaseDetail(row.id)
  dialogTitle.value = '编辑采购入库单 ' + (res.data?.orderNo || '')
  readonly.value = false
  fillForm(res.data)
  dialogVisible.value = true
}

const openDetail = async (row: any) => {
  const res: any = await loadPurchaseDetail(row.id)
  dialogTitle.value = '采购入库单详情 ' + (res.data?.orderNo || '')
  readonly.value = true
  fillForm(res.data)
  dialogVisible.value = true
}

const addItem = () => {
  form.items.push({ qualityStatus: '合格', stockInQty: 1, purchasePrice: 0 })
}

const handleSave = async () => {
  saving.value = true
  try {
    await savePurchase({ ...form })
    ElMessage.success('已保存草稿')
    dialogVisible.value = false
    tableRef.value?.reload()
  } finally {
    saving.value = false
  }
}

const handleConfirm = async (row: any) => {
  await ElMessageBox.confirm('确认后将增加批号库存，且不能再改。请确认已验收合格。', '确认入库', { type: 'warning' })
  await confirmPurchase(row.id)
  ElMessage.success('已确认入库')
  tableRef.value?.reload()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该草稿采购入库单？', '提示', { type: 'warning' })
  await deletePurchase(row.id)
  tableRef.value?.reload()
}

onMounted(async () => {
  const [s, w, d]: any[] = await Promise.all([
    loadAllSupplierForSelect(),
    loadAllWarehouseForSelect(),
    loadAllDrugForSelect()
  ])
  suppliers.value = s.data || []
  warehouses.value = w.data || []
  drugs.value = d.data || []
})
</script>

<style scoped>
.item-toolbar {
  margin: 8px 0;
}
</style>
