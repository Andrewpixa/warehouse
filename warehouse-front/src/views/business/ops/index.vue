<template>
  <div class="page-container ops-page">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">{{ cfg.title }}</h1>
      <p class="page-header-desc">{{ cfg.desc }}</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="单号">
          <el-input v-model="searchParams.docNo" placeholder="单据号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" clearable placeholder="状态" style="width: 140px">
            <el-option v-for="s in cfg.statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllOps" :search-params="searchParams">
        <el-table-column prop="docNo" :label="cfg.noLabel" min-width="140" />
        <el-table-column prop="title" label="摘要" min-width="160" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="140" />
        <el-table-column prop="supplierName" label="供应商" min-width="140" />
        <el-table-column prop="drugName" label="品种" min-width="120" />
        <el-table-column prop="warehouseName" label="仓库" min-width="110" />
        <el-table-column prop="relatedNo" label="关联单/发票" min-width="150" />
        <el-table-column v-if="isOffset" prop="receiptId" label="到账流水" width="100" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="amount" label="金额" width="100" />
        <el-table-column prop="bizDate" label="日期" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="canEdit(row.status)" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canAdvance(row.status)" type="success" link @click="handleConfirm(row)">{{ cfg.confirmText }}</el-button>
            <el-button v-if="canEdit(row.status)" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="openAdd">
            <el-icon><Plus /></el-icon> {{ cfg.addText }}
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="960px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="摘要" required>
              <el-input v-model="form.title" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="日期">
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联单号">
              <el-input v-model="form.relatedNo" :disabled="readonly" placeholder="发票号/出库单/合同号" />
            </el-form-item>
          </el-col>
        </el-row>
        <template v-if="isOffset">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="到账流水" required>
                <el-select v-model="form.receiptId" filterable :disabled="readonly" style="width: 100%" @change="onReceipt">
                  <el-option
                    v-for="r in receipts"
                    :key="r.id"
                    :label="`${r.receiptNo} ${r.channel} 余${r.remainAmount} / ${r.amount}`"
                    :value="r.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="挂账">
                <el-switch v-model="form.hangFlag" :active-value="1" :inactive-value="0" :disabled="readonly" />
                <span class="hang-hint">金额对不上先挂，不改发票回款</span>
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="客户">
              <el-select v-model="form.customerId" filterable clearable :disabled="readonly" style="width: 100%">
                <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-select v-model="form.supplierId" filterable clearable :disabled="readonly" style="width: 100%">
                <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库">
              <el-select v-model="form.warehouseId" filterable clearable :disabled="readonly" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="品种">
              <el-select v-model="form.drugId" filterable clearable :disabled="readonly" style="width: 100%">
                <el-option v-for="d in drugs" :key="d.id" :label="`${d.genericName} ${d.spec || ''}`" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原因/说明">
              <el-input v-model="form.reason" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="金额">
              <el-input-number v-model="form.amount" :min="0" :controls="false" style="width: 100%" :disabled="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" :disabled="readonly" />
        </el-form-item>
      </el-form>
      <div class="item-toolbar" v-if="!readonly">
        <el-button type="primary" size="small" @click="addItem">添加明细</el-button>
        <el-button v-if="isOffset" type="primary" size="small" plain @click="loadUnpaidItems">带入该客户未回票</el-button>
      </div>
      <el-table v-if="isOffset" :data="form.items" border size="small">
        <el-table-column label="发票号" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.relatedNo" :disabled="readonly" placeholder="20位发票号或出库单号" />
          </template>
        </el-table-column>
        <el-table-column label="本次冲账金额" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.amount" :min="0" :precision="2" :controls="false" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="" width="60">
          <template #default="{ $index }">
            <el-button type="danger" link @click="form.items.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-table v-else :data="form.items" border size="small">
        <el-table-column label="品种" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.drugId" filterable :disabled="readonly" style="width: 100%">
              <el-option v-for="d in drugs" :key="d.id" :label="d.genericName" :value="d.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="批号" width="130">
          <template #default="{ row }"><el-input v-model="row.batchNo" :disabled="readonly" /></template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }"><el-input-number v-model="row.qty" :min="0" :controls="false" style="width: 100%" :disabled="readonly" /></template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :controls="false" style="width: 100%" :disabled="readonly" /></template>
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
      </el-table>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!readonly" type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllOps, loadOpsDetail, saveOps, confirmOps, deleteOps } from '@/api/ops'
import { loadAllCustomer } from '@/api/customer'
import { loadAllSupplier } from '@/api/supplier'
import { loadAllWarehouse } from '@/api/warehouse'
import { loadAllDrug } from '@/api/drug'
import { loadAllBankReceipt } from '@/api/bankReceipt'
import { loadUnpaidOutbound } from '@/api/outbound'

const TYPE_CFG: Record<string, any> = {
  STOCKOUT: { title: '缺货登记 / 补货', desc: '开票缺货登记后，采购按上下限与缺货提示补货', noLabel: '缺货单号', addText: '登记缺货', confirmText: '标记已补货', statuses: ['草稿', '已登记', '已补货'] },
  INBOUND_EX: { title: '到货异常', desc: '合同不符、残损、超温、缺资料、待复检；质量部调整合格后才能勾单', noLabel: '异常单号', addText: '登记异常', confirmText: '推进处理', statuses: ['待处理', '待复检', '合格', '拒收'] },
  RETURN_NOTICE: { title: '销退收货通知单', desc: '拒收再送 / 销退入库：签名 → 运营释放 → 仓库按实物入库并产生负流向', noLabel: '通知单号', addText: '开收货通知单', confirmText: '推进（签/放/入）', statuses: ['草稿', '已签名', '已释放', '已入库'] },
  OFFSET: { title: '收款冲账', desc: '先选银行到账流水，再勾兑一张或多张发票。财务确认后才改已回款；对不上请挂账。', noLabel: '冲账单号', addText: '开冲账单', confirmText: '确认冲账', statuses: ['草稿', '已挂账', '已冲账'] },
  ALLOCATE: { title: '分货', desc: '订单预处理后按客户分货，分货完成开票员才能开票', noLabel: '分货单号', addText: '开分货单', confirmText: '分货完成', statuses: ['待分货', '已分货'] },
  LOGISTICS: { title: '物流联系单', desc: '超 100 件预约、特殊送货、取消订单须通知仓和财务', noLabel: '联系单号', addText: '开物流联系单', confirmText: '运营同意', statuses: ['草稿', '已同意'] },
  CREDIT: { title: '客户信誉额', desc: '开票前校验信誉额与客户证照，超限不得释放订单', noLabel: '信誉单号', addText: '维护信誉额', confirmText: '生效', statuses: ['草稿', '已生效'] },
  QUOTA: { title: '库容统筹值', desc: '无/超统筹值由采购反馈运营，向物流申请增加入库或库容', noLabel: '统筹单号', addText: '申请统筹值', confirmText: '生效', statuses: ['草稿', '已生效'] }
}

const route = useRoute()
const cfg = computed(() => TYPE_CFG[String(route.meta.docType)] || TYPE_CFG.STOCKOUT)
const docType = computed(() => String(route.meta.docType || 'STOCKOUT'))
const isOffset = computed(() => docType.value === 'OFFSET')

const searchParams = reactive<any>({ page: 1, limit: 10, docType: docType.value, docNo: '', status: '' })
const tableRef = ref<any>()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const readonly = ref(false)
const form = reactive<any>({ items: [] })
const customers = ref<any[]>([])
const suppliers = ref<any[]>([])
const warehouses = ref<any[]>([])
const drugs = ref<any[]>([])
const receipts = ref<any[]>([])

watch(docType, () => {
  searchParams.docType = docType.value
  tableRef.value?.reload()
})

onMounted(async () => {
  searchParams.docType = docType.value
  const [c, s, w, d] = await Promise.all([
    loadAllCustomer({ page: 1, limit: 200 }),
    loadAllSupplier({ page: 1, limit: 200 }),
    loadAllWarehouse({ page: 1, limit: 50 }),
    loadAllDrug({ page: 1, limit: 200 })
  ])
  customers.value = (c as any).data || []
  suppliers.value = (s as any).data || []
  warehouses.value = (w as any).data || []
  drugs.value = (d as any).data || []
  await reloadReceipts()
})

const reloadReceipts = async () => {
  if (!isOffset.value) return
  const res: any = await loadAllBankReceipt({ page: 1, limit: 200 })
  receipts.value = res.data || []
}

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.docNo = ''
  searchParams.status = ''
  tableRef.value?.reload()
}

const tagType = (status: string) => {
  if (['已补货', '合格', '已入库', '已冲账', '已分货', '已同意', '已生效'].includes(status)) return 'success'
  if (['待复检', '待处理', '已登记', '已签名', '已释放', '已挂账'].includes(status)) return 'warning'
  if (status === '拒收') return 'danger'
  return 'info'
}

const canEdit = (status: string) => ['草稿', '待处理', '待分货', '已登记', '已挂账'].includes(status)
const canAdvance = (status: string) => !['已补货', '合格', '拒收', '已入库', '已冲账', '已分货', '已同意', '已生效'].includes(status)

const blankForm = () => ({
  id: undefined,
  docType: docType.value,
  title: '',
  bizDate: new Date().toISOString().slice(0, 10),
  relatedNo: '',
  customerId: undefined,
  supplierId: undefined,
  warehouseId: 1,
  drugId: undefined,
  reason: '',
  amount: 0,
  receiptId: undefined,
  hangFlag: 0,
  remark: '',
  items: []
})

const fillForm = (data: any) => {
  Object.assign(form, blankForm(), data, { items: data.items || [] })
}

const openAdd = () => {
  readonly.value = false
  dialogTitle.value = cfg.value.addText
  fillForm(blankForm())
  dialogVisible.value = true
}

const openEdit = async (row: any) => {
  const res: any = await loadOpsDetail(row.id)
  readonly.value = false
  dialogTitle.value = '编辑 ' + (res.data?.docNo || '')
  fillForm(res.data || {})
  dialogVisible.value = true
}

const openDetail = async (row: any) => {
  const res: any = await loadOpsDetail(row.id)
  readonly.value = true
  dialogTitle.value = '详情 ' + (res.data?.docNo || '')
  fillForm(res.data || {})
  dialogVisible.value = true
}

const addItem = () => {
  if (isOffset.value) {
    form.items.push({ relatedNo: form.relatedNo || '', amount: 0 })
    return
  }
  form.items.push({ qualityStatus: '合格', qty: 1, price: 0 })
}

const onReceipt = (id: number) => {
  const r = receipts.value.find((x: any) => x.id === id)
  if (r) {
    form.customerId = r.customerId
    if (!form.title) form.title = `${r.channel}到账冲账`
  }
}

const loadUnpaidItems = async () => {
  if (!form.customerId) {
    ElMessage.warning('请先选客户或到账流水')
    return
  }
  const res: any = await loadUnpaidOutbound({ page: 1, limit: 50, customerId: form.customerId })
  const list = res.data || []
  form.items = list.map((o: any) => ({
    relatedNo: o.invoiceNo || o.orderNo,
    amount: Math.max(0, Number(o.totalAmount || 0) - Number(o.paidAmount || 0))
  }))
}

const handleSave = async () => {
  const res: any = await saveOps({ ...form, docType: docType.value })
  ElMessage.success('已保存')
  fillForm(res.data || {})
  tableRef.value?.reload()
}

const handleConfirm = async (row: any) => {
  await ElMessageBox.confirm('确认按流程图推进该单据？', cfg.value.confirmText, { type: 'warning' })
  await confirmOps(row.id)
  ElMessage.success('已推进')
  tableRef.value?.reload()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('删除该草稿？', '提示', { type: 'warning' })
  await deleteOps(row.id)
  ElMessage.success('已删除')
  tableRef.value?.reload()
}
</script>

<style scoped>
.hang-hint { margin-left: 8px; font-size: 12px; color: var(--el-text-color-secondary); }
.item-toolbar { margin: 8px 0; }
</style>
