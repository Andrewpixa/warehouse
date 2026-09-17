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
        <el-table-column prop="invoiceNo" label="供应商发票" min-width="150" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1040px" destroy-on-close>
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
          <el-col :span="8">
            <el-form-item label="供应商发票" required>
              <el-input v-model="form.invoiceNo" maxlength="64" placeholder="选供应商后自动带出，草稿可改" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" :disabled="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="供应商发票由供方随货提供，不是仓库自己开。选供应商后自动模拟带出发票号；草稿状态可改错票，再点「模拟接收供应商票据」。确认入库后锁定。"
          style="margin-bottom: 12px"
        />

      <div class="item-toolbar" v-if="!readonly">
        <el-button type="primary" size="small" @click="addItem">添加明细</el-button>
        <el-button type="success" size="small" :disabled="!form.id" :loading="receiving" @click="handleReceiveInvoice">模拟接收供应商票据</el-button>
        <span class="hint-inline">须先保存草稿。会生成供方发票和随货同行单（演示用）。</span>
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

        <VoucherPanel
          ref="voucherRef"
          biz-type="purchase"
          :biz-id="form.id"
          :readonly="readonly && form.status === '已确认'"
          :sign-roles="purchaseSignRoles"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
        <el-button v-if="form.id && form.status !== '已确认' && canConfirm" type="success" :loading="confirming" @click="handleConfirmDialog">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllPurchase, loadPurchaseDetail, savePurchase, confirmPurchase, deletePurchase, previewSupplierInvoice, receiveSupplierInvoice } from '@/api/purchase'
import { loadAllSupplierForSelect } from '@/api/supplier'
import { loadAllWarehouseForSelect } from '@/api/warehouse'
import { loadAllDrugForSelect } from '@/api/drug'
import { usePermission } from '@/composables/usePermission'
import VoucherPanel from '@/components/VoucherPanel.vue'

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
const confirming = ref(false)
const receiving = ref(false)
const voucherRef = ref()
const dialogTitle = ref('开采购入库单')
const purchaseSignRoles = [
  { role: 'purchase_check', label: '进货验收签字' },
  { role: 'purchase_keep', label: '到货保管签字' }
]

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
  invoiceNo: '',
  status: '',
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
    invoiceNo: data.invoiceNo || '',
    status: data.status || '',
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

watch(() => form.supplierId, async (id) => {
  if (readonly.value || !id) return
  const cur = form.invoiceNo || ''
  if (cur && !cur.startsWith('FP')) return
  try {
    const res: any = await previewSupplierInvoice(id, form.bizDate)
    if (res.invoiceNo) form.invoiceNo = res.invoiceNo
  } catch {}
})

const handleReceiveInvoice = async () => {
  if (!form.id) {
    ElMessage.warning('请先保存草稿')
    return
  }
  receiving.value = true
  try {
    const res: any = await receiveSupplierInvoice(form.id)
    if (res.code !== 200) {
      ElMessage.error(res.msg || '模拟接收失败')
      return
    }
    ElMessage.success(res.msg || '已接收供应商发票')
    fillForm(res.data || {})
    voucherRef.value?.reload?.()
    tableRef.value?.reload()
  } finally {
    receiving.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    const wasNew = !form.id
    const res: any = await savePurchase({ ...form })
    ElMessage.success('草稿已保存')
    fillForm(res.data || {})
    tableRef.value?.reload()
    if (wasNew && form.id) {
      await handleReceiveInvoice()
    }
  } finally {
    saving.value = false
  }
}

const handleConfirmDialog = async () => {
  if (!form.id) return
  await ElMessageBox.confirm('确认后将增加批号库存，且不能再改。须已挂发票、随货同行单并完成验收/到货签字。', '确认入库', { type: 'warning' })
  confirming.value = true
  try {
    await confirmPurchase(form.id)
    ElMessage.success('已确认入库')
    dialogVisible.value = false
    tableRef.value?.reload()
  } finally {
    confirming.value = false
  }
}

const handleConfirm = async (row: any) => {
  const res: any = await loadPurchaseDetail(row.id)
  dialogTitle.value = '确认采购入库 ' + (res.data?.orderNo || '')
  readonly.value = true
  fillForm(res.data)
  dialogVisible.value = true
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.hint-inline {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
