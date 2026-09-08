<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">批号升益</h1>
      <p class="page-header-desc">按药品 + 仓库 + 批号盘点；实盘多于账面记升益量/升益额，少于账面记损耗</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="单号">
          <el-input v-model="searchParams.orderNo" placeholder="升益单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="草稿" value="草稿" />
            <el-option label="已确认" value="已确认" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllSurplus" :search-params="searchParams">
        <el-table-column prop="orderNo" label="升益单号" min-width="150" />
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="bizDate" label="业务日期" width="120" />
        <el-table-column prop="totalSurplusQty" label="升益量" width="100" />
        <el-table-column prop="totalSurplusAmount" label="升益额" width="110" />
        <el-table-column prop="totalLossQty" label="损耗量" width="100" />
        <el-table-column prop="totalLossAmount" label="损耗额" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '已确认' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '草稿'" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === '草稿'" type="success" link @click="handleConfirm(row)">确认过账</el-button>
            <el-button v-if="row.status === '草稿'" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="openAdd">
            <el-icon><Plus /></el-icon> 开升益单
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1180px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="仓库" required>
              <el-select v-model="form.warehouseId" filterable placeholder="选择仓库" style="width: 100%" :disabled="readonly">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务日期" required>
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" :disabled="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="item-toolbar" v-if="!readonly">
        <el-button type="primary" size="small" :loading="loadingBatches" @click="handleLoadBatches">载入该仓库批号库存</el-button>
        <el-button size="small" @click="addItem">添加批号行</el-button>
        <span class="item-hint">账面无、实物有的批号可手工加一行，账面数量填 0</span>
      </div>

      <el-table :data="form.items" border size="small" max-height="420" show-summary :summary-method="summarize">
        <el-table-column label="品种" min-width="200">
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
        <el-table-column label="质量" width="96">
          <template #default="{ row }">
            <el-select v-model="row.qualityStatus" :disabled="readonly" style="width: 100%">
              <el-option label="合格" value="合格" />
              <el-option label="待验" value="待验" />
              <el-option label="不合格" value="不合格" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="账面" width="88">
          <template #default="{ row }">
            <el-input-number v-model="row.bookQty" :min="0" :controls="false" style="width: 100%" :disabled="readonly" @change="recalc(row)" />
          </template>
        </el-table-column>
        <el-table-column label="实盘" width="88">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQty" :min="0" :controls="false" style="width: 100%" :disabled="readonly" @change="recalc(row)" />
          </template>
        </el-table-column>
        <el-table-column label="成本单价" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.unitCost" :min="0" :precision="2" :controls="false" style="width: 100%" :disabled="readonly" @change="recalc(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="surplusQty" label="升益量" width="80" align="right">
          <template #default="{ row }">
            <span :class="{ 'num-plus': row.surplusQty > 0 }">{{ formatNum(row.surplusQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="surplusAmount" label="升益额" width="90" align="right">
          <template #default="{ row }">
            <span :class="{ 'num-plus': row.surplusAmount > 0 }">{{ formatMoney(row.surplusAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lossQty" label="损耗量" width="80" align="right">
          <template #default="{ row }">
            <span :class="{ 'num-minus': row.lossQty > 0 }">{{ formatNum(row.lossQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lossAmount" label="损耗额" width="90" align="right">
          <template #default="{ row }">
            <span :class="{ 'num-minus': row.lossAmount > 0 }">{{ formatMoney(row.lossAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="" width="50">
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllSurplus, loadSurplusDetail, loadWarehouseBatches, saveSurplus, confirmSurplus, deleteSurplus } from '@/api/surplus'
import { loadAllWarehouseForSelect } from '@/api/warehouse'
import { loadAllDrugForSelect } from '@/api/drug'

const tableRef = ref()
const warehouses = ref<any[]>([])
const drugs = ref<any[]>([])
const dialogVisible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const loadingBatches = ref(false)
const dialogTitle = ref('开升益单')

const searchParams = reactive({
  orderNo: '',
  status: ''
})

const emptyItem = () => ({
  drugId: undefined as number | undefined,
  batchNo: '',
  qualityStatus: '合格',
  bookQty: 0,
  actualQty: 0,
  unitCost: 0,
  surplusQty: 0,
  surplusAmount: 0,
  lossQty: 0,
  lossAmount: 0,
  expireDate: ''
})

const emptyForm = () => ({
  id: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  bizDate: '',
  remark: '',
  items: [] as any[]
})

const form = reactive(emptyForm())

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.orderNo = ''
  searchParams.status = ''
}

const n = (v: any) => Number(v || 0)

const recalc = (row: any) => {
  const diff = n(row.actualQty) - n(row.bookQty)
  const cost = n(row.unitCost)
  row.surplusQty = diff > 0 ? diff : 0
  row.lossQty = diff < 0 ? -diff : 0
  row.surplusAmount = +(row.surplusQty * cost).toFixed(2)
  row.lossAmount = +(row.lossQty * cost).toFixed(2)
}

const formatNum = (v: any) => n(v).toFixed(3).replace(/\.?0+$/, '') || '0'
const formatMoney = (v: any) => n(v).toFixed(2)

const summarize = ({ columns, data }: { columns: any[]; data: any[] }) => {
  const sums: string[] = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const field = col.property
    if (!field) {
      sums[index] = ''
      return
    }
    const total = data.reduce((acc, row) => acc + n(row[field]), 0)
    sums[index] = field.endsWith('Amount') ? total.toFixed(2) : formatNum(total)
  })
  return sums
}

const resetForm = (data?: any) => {
  Object.assign(form, emptyForm(), data || {})
  if (!form.items) form.items = []
  form.items.forEach(recalc)
}

const openAdd = () => {
  dialogTitle.value = '开升益单'
  readonly.value = false
  resetForm({
    bizDate: new Date().toISOString().slice(0, 10),
    items: []
  })
  dialogVisible.value = true
}

const fillForm = (data: any) => {
  resetForm({
    id: data.id,
    warehouseId: data.warehouseId,
    bizDate: data.bizDate,
    remark: data.remark,
    items: (data.items || []).map((it: any) => ({ ...it }))
  })
}

const openEdit = async (row: any) => {
  const res: any = await loadSurplusDetail(row.id)
  dialogTitle.value = '编辑升益单 ' + (res.data?.orderNo || '')
  readonly.value = false
  fillForm(res.data)
  dialogVisible.value = true
}

const openDetail = async (row: any) => {
  const res: any = await loadSurplusDetail(row.id)
  dialogTitle.value = '升益单详情 ' + (res.data?.orderNo || '')
  readonly.value = true
  fillForm(res.data)
  dialogVisible.value = true
}

const addItem = () => {
  const row = emptyItem()
  form.items.push(row)
}

const handleLoadBatches = async () => {
  if (!form.warehouseId) {
    ElMessage.warning('请先选择仓库')
    return
  }
  loadingBatches.value = true
  try {
    const res: any = await loadWarehouseBatches(form.warehouseId)
    form.items = (res.data || []).map((it: any) => {
      const row = { ...emptyItem(), ...it }
      recalc(row)
      return row
    })
    if (!form.items.length) {
      ElMessage.info('该仓库暂无批号库存，可手工添加账面为 0 的行')
    }
  } finally {
    loadingBatches.value = false
  }
}

const handleSave = async () => {
  if (!form.warehouseId) {
    ElMessage.warning('请选择仓库')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请先载入批号库存或添加明细')
    return
  }
  saving.value = true
  try {
    await saveSurplus({ ...form })
    ElMessage.success('已保存草稿')
    dialogVisible.value = false
    tableRef.value?.reload()
  } finally {
    saving.value = false
  }
}

const handleConfirm = async (row: any) => {
  await ElMessageBox.confirm('确认后将按批号增加升益库存、扣减损耗库存，且不能再改。继续？', '确认过账', { type: 'warning' })
  await confirmSurplus(row.id)
  ElMessage.success('已确认，批号库存已更新')
  tableRef.value?.reload()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该草稿升益单？', '提示', { type: 'warning' })
  await deleteSurplus(row.id)
  tableRef.value?.reload()
}

onMounted(async () => {
  const [w, d]: any[] = await Promise.all([
    loadAllWarehouseForSelect(),
    loadAllDrugForSelect()
  ])
  warehouses.value = w.data || []
  drugs.value = d.data || []
})
</script>

<style scoped>
.item-toolbar {
  margin: 8px 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.item-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.num-plus {
  color: #67c23a;
  font-weight: 600;
}
.num-minus {
  color: #f56c6c;
  font-weight: 600;
}
</style>
