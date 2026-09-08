<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">医院收货确认</h1>
      <p class="page-header-desc">下游医院核对我方发货明细（数量、金额、批号、时间、电子发票）后确认收货或拒收</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="医院">
          <el-select v-model="searchParams.customerId" filterable clearable placeholder="选择医院/客户" style="width: 220px">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发票号">
          <el-input v-model="searchParams.invoiceNo" placeholder="20位发票号" clearable />
        </el-form-item>
        <el-form-item label="收货">
          <el-select v-model="searchParams.receiveStatus" placeholder="收货状态" style="width: 140px">
            <el-option label="待收货" value="待收货" />
            <el-option label="已签收" value="已签收" />
            <el-option label="部分签收" value="部分签收" />
            <el-option label="拒收" value="拒收" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadPendingReceipt" :search-params="searchParams">
        <el-table-column prop="invoiceNo" label="发票号" min-width="200" />
        <el-table-column prop="orderNo" label="出库单号" min-width="140" />
        <el-table-column prop="customerName" label="下游医院" min-width="140" />
        <el-table-column prop="warehouseName" label="发货仓库" min-width="120" />
        <el-table-column prop="shipTime" label="发货时间" min-width="170" />
        <el-table-column prop="einvoiceNo" label="电子发票" min-width="180" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column label="收货" width="110">
          <template #default="{ row }">
            <el-tag :type="receiveTagType(row.receiveStatus)" size="small">{{ row.receiveStatus || '待收货' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.receiveStatus === '待收货'" type="success" link @click="openReceive(row)">确认收货</el-button>
          </template>
        </el-table-column>
      </CrudTable>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1040px" destroy-on-close>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="发票号">{{ form.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="医院">{{ form.customerName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ form.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ form.shipTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="电子发票号">{{ form.einvoiceNo || form.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="收货状态">{{ form.receiveStatus || '—' }}</el-descriptions-item>
        <el-descriptions-item label="电子发票文件" :span="3">
          <a v-if="form.einvoicePath" :href="fileUrl(form.einvoicePath)" target="_blank" rel="noopener">查看电子发票</a>
          <span v-else>未上传</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="form.items" border size="small" class="item-table">
        <el-table-column prop="drugName" label="品种" min-width="160" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column prop="expireDate" label="效期" width="120" />
        <el-table-column prop="qty" label="发货数量" width="90" />
        <el-table-column prop="salePrice" label="单价" width="90" />
        <el-table-column label="金额" width="90">
          <template #default="{ row }">{{ Number(row.amount ?? (row.qty || 0) * (row.salePrice || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="实收数量" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="canEdit"
              v-model="row.receivedQty"
              :min="0"
              :max="Number(row.qty || 0)"
              :controls="false"
              style="width: 100%"
            />
            <span v-else>{{ row.receivedQty ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="行备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.receiveRemark" placeholder="少收说明" />
          </template>
        </el-table-column>
      </el-table>

      <el-form v-if="canEdit" label-width="90px" class="remark-form">
        <el-form-item label="收货备注">
          <el-input v-model="form.receiveRemark" placeholder="整单备注，可空" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <template v-if="canEdit">
          <el-button type="danger" :loading="saving" @click="handleReject">整单拒收</el-button>
          <el-button type="success" :loading="saving" @click="handleSign">确认收货</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadPendingReceipt, loadReceiptDetail, confirmReceipt } from '@/api/outbound'
import { loadAllCustomerForSelect } from '@/api/customer'
import { BASE_URL } from '@/utils/request'

const tableRef = ref()
const customers = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('收货明细')
const canEdit = ref(false)
const saving = ref(false)

const searchParams = reactive({
  customerId: undefined as number | undefined,
  invoiceNo: '',
  receiveStatus: '待收货'
})

const form = reactive({
  id: 0,
  invoiceNo: '',
  customerName: '',
  warehouseName: '',
  shipTime: '',
  einvoiceNo: '',
  einvoicePath: '',
  receiveStatus: '',
  receiveRemark: '',
  items: [] as any[]
})

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.customerId = undefined
  searchParams.invoiceNo = ''
  searchParams.receiveStatus = '待收货'
}

const receiveTagType = (status?: string) => {
  if (status === '已签收') return 'success'
  if (status === '部分签收') return 'warning'
  if (status === '拒收') return 'danger'
  return 'info'
}

const fileUrl = (path: string) => BASE_URL + '/file/showImageByPath?path=' + encodeURIComponent(path)

const fillForm = (data: any) => {
  form.id = data.id
  form.invoiceNo = data.invoiceNo
  form.customerName = data.customerName
  form.warehouseName = data.warehouseName
  form.shipTime = data.shipTime
  form.einvoiceNo = data.einvoiceNo
  form.einvoicePath = data.einvoicePath
  form.receiveStatus = data.receiveStatus
  form.receiveRemark = data.receiveRemark || ''
  form.items = (data.items || []).map((it: any) => ({
    ...it,
    receivedQty: it.receivedQty == null ? Number(it.qty || 0) : Number(it.receivedQty)
  }))
}

const openDetail = async (row: any) => {
  const res: any = await loadReceiptDetail(row.id)
  dialogTitle.value = '发货明细 ' + (res.data?.invoiceNo || '')
  canEdit.value = false
  fillForm(res.data || {})
  dialogVisible.value = true
}

const openReceive = async (row: any) => {
  const res: any = await loadReceiptDetail(row.id)
  dialogTitle.value = '确认收货 ' + (res.data?.invoiceNo || '')
  canEdit.value = true
  fillForm(res.data || {})
  dialogVisible.value = true
}

const handleSign = async () => {
  const short = form.items.filter((it: any) => Number(it.receivedQty) < Number(it.qty || 0))
  const hint = short.length
    ? `有 ${short.length} 行实收少于发货，将记为部分签收。少收部分可随后开红冲。`
    : '确认按实收数量签收？签收后货权转移完成。'
  await ElMessageBox.confirm(hint, '确认收货', { type: 'warning' })
  saving.value = true
  try {
    await confirmReceipt({
      id: form.id,
      receiveAction: '签收',
      receiveRemark: form.receiveRemark,
      items: form.items.map((it: any) => ({
        id: it.id,
        receivedQty: it.receivedQty,
        receiveRemark: it.receiveRemark
      }))
    })
    ElMessage.success('收货已确认')
    dialogVisible.value = false
    tableRef.value?.reload()
  } finally {
    saving.value = false
  }
}

const handleReject = async () => {
  await ElMessageBox.confirm('整单拒收后本单不再待收货，请随后在出库单开红冲把库存加回。', '整单拒收', { type: 'warning' })
  saving.value = true
  try {
    await confirmReceipt({
      id: form.id,
      receiveAction: '拒收',
      receiveRemark: form.receiveRemark || '医院整单拒收'
    })
    ElMessage.success('已拒收')
    dialogVisible.value = false
    tableRef.value?.reload()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const c: any = await loadAllCustomerForSelect()
    customers.value = c.data || []
  } catch {
    customers.value = []
  }
})
</script>

<style scoped>
.item-table {
  margin-top: 16px;
}
.remark-form {
  margin-top: 12px;
}
</style>
