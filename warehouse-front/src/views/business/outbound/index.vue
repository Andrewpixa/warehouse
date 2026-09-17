<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">销售出库单</h1>
      <p class="page-header-desc">确认发货须挂发票影像、随货同行单并完成送货签字；数量、批号、金额与批号库存勾稽后才能过账。</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="单号">
          <el-input v-model="searchParams.orderNo" placeholder="销售出库单号" clearable />
        </el-form-item>
        <el-form-item label="发票号">
          <el-input v-model="searchParams.invoiceNo" placeholder="20位发票号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="草稿" value="草稿" />
            <el-option label="已确认" value="已确认" />
          </el-select>
        </el-form-item>
        <el-form-item label="回款">
          <el-select v-model="searchParams.paidStatus" clearable placeholder="回款状态" style="width: 140px">
            <el-option label="未回款" value="未回款" />
            <el-option label="部分回款" value="部分回款" />
            <el-option label="已回款" value="已回款" />
          </el-select>
        </el-form-item>
        <el-form-item label="收货">
          <el-select v-model="searchParams.receiveStatus" clearable placeholder="收货状态" style="width: 140px">
            <el-option label="待收货" value="待收货" />
            <el-option label="已签收" value="已签收" />
            <el-option label="部分签收" value="部分签收" />
            <el-option label="拒收" value="拒收" />
          </el-select>
        </el-form-item>
        <el-form-item label="单据">
          <el-select v-model="searchParams.orderType" clearable placeholder="类型" style="width: 120px">
            <el-option label="正常出库" value="正常" />
            <el-option label="红冲" value="红冲" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllOutbound" :search-params="searchParams">
        <el-table-column prop="orderNo" label="出库单号" min-width="140" />
        <el-table-column label="全电发票" min-width="220">
          <template #default="{ row }">
            <div>{{ row.invoiceNo }}</div>
            <div v-if="row.invoiceNo && String(row.invoiceNo).length === 20" class="inv-sub">
              年度 {{ String(row.invoiceNo).slice(0,2) }} · 赋码 {{ String(row.invoiceNo).slice(2,10) }} · 顺序号 {{ String(row.invoiceNo).slice(10) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.orderType === '红冲' ? 'danger' : 'primary'" size="small">{{ row.orderType || '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="originalInvoiceNo" label="原发票号" min-width="180" />
        <el-table-column prop="customerName" label="下游客户" min-width="140" />
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="bizDate" label="出库日期" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column label="回款" width="110">
          <template #default="{ row }">
            <el-tag :type="paidTagType(row.paidStatus)" size="small">{{ row.paidStatus || '未回款' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收货" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.receiveStatus" :type="receiveTagType(row.receiveStatus)" size="small">{{ row.receiveStatus }}</el-tag>
            <span v-else-if="row.status === '草稿'">—</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '已确认' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '草稿'" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === '草稿'" type="success" link @click="openShip(row)">{{ row.orderType === '红冲' ? '确认红冲' : '确认发货' }}</el-button>
            <el-button v-if="row.status === '已确认' && row.orderType !== '红冲'" type="danger" link @click="openReversal(row)">开红冲</el-button>
            <el-button v-if="row.status === '已确认' && row.orderType !== '红冲'" type="warning" link @click="openPaid(row)">标记回款</el-button>
            <el-button v-if="row.status === '草稿'" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="openAdd">
            <el-icon><Plus /></el-icon> 开销售出库单
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1040px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="下游客户" required>
              <el-select v-model="form.customerId" filterable placeholder="选择医院/药店/诊所" style="width: 100%" :disabled="readonly">
                <el-option v-for="c in customers" :key="c.id" :label="`${c.id} ${c.name}`" :value="c.id" />
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
            <el-form-item label="出库日期" required>
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发票号">
              <el-input v-model="form.invoiceNo" maxlength="20" placeholder="留空按全电规则生成：年度+95700000+10位顺序号" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结算">
              <el-select v-model="form.payType" placeholder="结算方式" style="width: 100%" :disabled="readonly">
                <el-option label="月结" value="月结" />
                <el-option label="现结" value="现结" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="进单入口">
              <el-select v-model="form.orderChannel" style="width: 100%" :disabled="readonly">
                <el-option label="线下 BMS（开票员录入）" value="OFFLINE" />
                <el-option label="全药网/药交网单" value="PLATFORM" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.orderChannel === 'PLATFORM'" :span="8">
            <el-form-item label="平台单号">
              <el-input v-model="form.platformNo" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.orderType === '红冲'" :span="8">
            <el-form-item label="类型">红冲</el-form-item>
          </el-col>
          <el-col v-if="form.originalInvoiceNo" :span="8">
            <el-form-item label="原发票">{{ form.originalInvoiceNo }}</el-form-item>
          </el-col>
          <el-col v-if="readonly && form.shipTime" :span="8">
            <el-form-item label="发货时间">{{ form.shipTime }}</el-form-item>
          </el-col>
          <el-col v-if="readonly && form.einvoiceNo" :span="8">
            <el-form-item label="电子发票">{{ form.einvoiceNo }}</el-form-item>
          </el-col>
          <el-col v-if="readonly && form.receiveStatus" :span="8">
            <el-form-item label="收货">{{ form.receiveStatus }}</el-form-item>
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
        <el-table-column label="批号" width="120">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="效期" width="140">
          <template #default="{ row }">
            <el-date-picker v-model="row.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0" :controls="false" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="售价" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.salePrice" :min="0" :precision="2" :controls="false" style="width: 100%" :disabled="readonly" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row }">{{ ((Number(row.qty) || 0) * (Number(row.salePrice) || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column v-if="readonly" label="实收" width="80">
          <template #default="{ row }">{{ row.receivedQty ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="SPDID" min-width="160">
          <template #default="{ row }">
            <span>{{ row.spdid || '保存后自动生成' }}</span>
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

    <el-dialog v-model="paidVisible" title="标记回款" width="420px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="发票号">{{ paidForm.invoiceNo }}</el-form-item>
        <el-form-item label="出库金额">{{ paidForm.totalAmount }}</el-form-item>
        <el-form-item label="回款状态" required>
          <el-select v-model="paidForm.paidStatus" style="width: 100%">
            <el-option label="未回款" value="未回款" />
            <el-option label="部分回款" value="部分回款" />
            <el-option label="已回款" value="已回款" />
          </el-select>
        </el-form-item>
        <el-form-item label="已回金额">
          <el-input-number v-model="paidForm.paidAmount" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paidVisible = false">取消</el-button>
        <el-button type="primary" :loading="paidSaving" @click="handleMarkPaid">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revVisible" title="开红冲单" width="920px" destroy-on-close>
      <p class="rev-hint">红冲是一张独立出库单。确认后按冲减数量加回批号库存，不能超过原发票未冲完的数量。</p>
      <el-form label-width="100px">
        <el-row :gutter="12">
          <el-col :span="10">
            <el-form-item label="原发票号">{{ revForm.originalInvoiceNo }}</el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="红冲日期" required>
              <el-date-picker v-model="revForm.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="原因">
              <el-input v-model="revForm.remark" placeholder="如质量投诉、开票错误" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-table :data="revForm.items" border size="small">
        <el-table-column prop="drugName" label="药品" min-width="160" />
        <el-table-column prop="batchNo" label="批号" width="130" />
        <el-table-column prop="qtyOrigin" label="原出库" width="90" />
        <el-table-column prop="remainingQty" label="还可冲" width="90" />
        <el-table-column label="本次红冲" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0" :max="Number(row.remainingQty || 0)" :controls="false" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="revVisible = false">取消</el-button>
        <el-button type="danger" :loading="revSaving" @click="handleSaveReversal">保存红冲草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" :title="shipForm.orderType === '红冲' ? '确认红冲' : '确认发货'" width="920px" destroy-on-close>
      <p class="rev-hint">核对数量、金额、批号后填写发货时间和电子发票。确认后扣减批号库存，单据进入「待收货」，由下游医院签收。</p>
      <el-form label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="发票号">{{ shipForm.invoiceNo }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下游">{{ shipForm.customerName }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发货时间" required>
              <el-date-picker v-model="shipForm.shipTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子发票号">
              <el-input v-model="shipForm.einvoiceNo" maxlength="32" placeholder="默认等于发票号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="电子发票">
              <el-upload
                :action="uploadUrl"
                name="mf"
                :show-file-list="false"
                :on-success="handleInvoiceSuccess"
                :before-upload="beforeInvoiceUpload"
                :with-credentials="true"
                accept=".pdf,image/*"
              >
                <el-button type="primary" plain>上传 PDF / 图片</el-button>
                <el-button type="success" plain style="margin-left: 8px" :loading="simulating" @click.stop.prevent="handleSimulateEinvoice">模拟开具电子发票</el-button>
                <a v-if="shipForm.einvoicePath" class="upload-ok" :href="einvoiceHref" target="_blank" rel="noopener">查看模拟发票</a>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-table :data="shipForm.items" border size="small">
        <el-table-column prop="drugName" label="品种" min-width="160" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column prop="expireDate" label="效期" width="120" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="salePrice" label="售价" width="90" />
        <el-table-column label="金额" width="90">
          <template #default="{ row }">{{ ((Number(row.qty) || 0) * (Number(row.salePrice) || 0)).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="success" :loading="shipSaving" @click="handleConfirmShip">{{ shipForm.orderType === '红冲' ? '确认红冲' : '确认发货' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllOutbound, loadOutboundDetail, saveOutbound, confirmOutbound, deleteOutbound, markPaid, saveReversal, simulateEinvoice } from '@/api/outbound'
import { loadAllCustomerForSelect } from '@/api/customer'
import { loadAllWarehouseForSelect } from '@/api/warehouse'
import { loadAllDrugForSelect } from '@/api/drug'
import { BASE_URL } from '@/utils/request'
import { getImageUrl } from '@/api/file'

const tableRef = ref()
const customers = ref<any[]>([])
const warehouses = ref<any[]>([])
const drugs = ref<any[]>([])
const dialogVisible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const dialogTitle = ref('开销售出库单')
const paidVisible = ref(false)
const paidSaving = ref(false)
const paidForm = reactive({
  id: 0,
  invoiceNo: '',
  totalAmount: 0,
  paidStatus: '未回款',
  paidAmount: 0
})
const revVisible = ref(false)
const revSaving = ref(false)
const revForm = reactive({
  originalInvoiceNo: '',
  bizDate: '',
  remark: '',
  items: [] as any[]
})
const shipVisible = ref(false)
const shipSaving = ref(false)
const simulating = ref(false)
const uploadUrl = BASE_URL + '/file/uploadFile'
const einvoiceHref = computed(() => shipForm.einvoicePath ? getImageUrl(shipForm.einvoicePath) : '')
const shipForm = reactive({
  id: 0,
  orderType: '正常',
  invoiceNo: '',
  customerName: '',
  shipTime: '',
  einvoiceNo: '',
  einvoicePath: '',
  items: [] as any[]
})

const searchParams = reactive({
  orderNo: '',
  invoiceNo: '',
  status: '',
  paidStatus: '',
  receiveStatus: '',
  orderType: ''
})

const emptyForm = () => ({
  id: undefined as number | undefined,
  customerId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  invoiceNo: '',
  orderType: '正常',
  originalInvoiceNo: '',
  payType: '月结',
  orderChannel: 'OFFLINE',
  platformNo: '',
  bizDate: '',
  shipTime: '',
  einvoiceNo: '',
  einvoicePath: '',
  receiveStatus: '',
  remark: '',
  items: [] as any[]
})

const form = reactive(emptyForm())

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.orderNo = ''
  searchParams.invoiceNo = ''
  searchParams.status = ''
  searchParams.paidStatus = ''
  searchParams.receiveStatus = ''
  searchParams.orderType = ''
}

const receiveTagType = (status?: string) => {
  if (status === '已签收') return 'success'
  if (status === '部分签收') return 'warning'
  if (status === '拒收') return 'danger'
  return 'info'
}

const paidTagType = (status?: string) => {
  if (status === '已回款') return 'success'
  if (status === '部分回款') return 'warning'
  return 'danger'
}

const resetForm = (data?: any) => {
  Object.assign(form, emptyForm(), data || {})
  if (!form.items) form.items = []
}

const openAdd = () => {
  dialogTitle.value = '开销售出库单'
  readonly.value = false
  resetForm({
    bizDate: new Date().toISOString().slice(0, 10),
    items: [{ qualityStatus: '合格', qty: 1, salePrice: 0 }]
  })
  dialogVisible.value = true
}

const fillForm = (data: any) => {
  resetForm({
    id: data.id,
    customerId: data.customerId,
    warehouseId: data.warehouseId,
    invoiceNo: data.invoiceNo,
    orderType: data.orderType || '正常',
    originalInvoiceNo: data.originalInvoiceNo || '',
    payType: data.payType || '月结',
    orderChannel: data.orderChannel || 'OFFLINE',
    platformNo: data.platformNo || '',
    bizDate: data.bizDate,
    shipTime: data.shipTime || '',
    einvoiceNo: data.einvoiceNo || '',
    einvoicePath: data.einvoicePath || '',
    receiveStatus: data.receiveStatus || '',
    remark: data.remark,
    items: (data.items || []).map((it: any) => ({ ...it }))
  })
}

const openEdit = async (row: any) => {
  const res: any = await loadOutboundDetail(row.id)
  dialogTitle.value = '编辑销售出库单 ' + (res.data?.orderNo || '')
  readonly.value = false
  fillForm(res.data)
  dialogVisible.value = true
}

const openDetail = async (row: any) => {
  const res: any = await loadOutboundDetail(row.id)
  dialogTitle.value = '销售出库单详情 ' + (res.data?.orderNo || '')
  readonly.value = true
  fillForm(res.data)
  dialogVisible.value = true
}

const addItem = () => {
  form.items.push({ qualityStatus: '合格', qty: 1, salePrice: 0 })
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveOutbound({ ...form })
    ElMessage.success('已保存草稿')
    dialogVisible.value = false
    tableRef.value?.reload()
  } finally {
    saving.value = false
  }
}

const handleConfirm = async (row: any) => {
  const isRev = row.orderType === '红冲'
  await ElMessageBox.confirm(
    isRev ? '确认后将按红冲数量加回批号库存，且不能再改。' : '确认后将扣减批号库存，且不能再改。库存不够会立刻提示。',
    isRev ? '确认红冲' : '确认出库',
    { type: 'warning' }
  )
  try {
    await confirmOutbound({ id: row.id })
    ElMessage.success(isRev ? '已确认红冲，库存已加回' : '已确认出库')
    tableRef.value?.reload()
  } catch {
    tableRef.value?.reload()
  }
}

const nowShipTime = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const openShip = async (row: any) => {
  if (row.orderType === '红冲') {
    await handleConfirm(row)
    return
  }
  const res: any = await loadOutboundDetail(row.id)
  const data = res.data || {}
  const items = data.items || []
  if (!items.length) {
    ElMessage.warning('请先保存明细再发货')
    return
  }
  const missing = items.find((it: any) => !it.batchNo || !it.qty)
  if (missing) {
    ElMessage.warning('每行必须填写批号和数量')
    return
  }
  shipForm.id = data.id
  shipForm.orderType = data.orderType || '正常'
  shipForm.invoiceNo = data.invoiceNo
  shipForm.customerName = data.customerName
  shipForm.shipTime = nowShipTime()
  shipForm.einvoiceNo = data.einvoiceNo || data.invoiceNo || ''
  shipForm.einvoicePath = data.einvoicePath || ''
  shipForm.items = items
  shipVisible.value = true
}

const beforeInvoiceUpload = (file: File) => {
  const okType = file.type.startsWith('image/') || file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!okType) {
    ElMessage.error('请上传 PDF 或图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('文件不能超过 5MB')
    return false
  }
  return true
}

const handleInvoiceSuccess = (res: any) => {
  if (res && res.path) {
    shipForm.einvoicePath = res.path
    ElMessage.success('电子发票已上传')
  } else {
    ElMessage.error(res?.msg || '上传失败')
  }
}

const handleSimulateEinvoice = async () => {
  if (!shipForm.id) return
  simulating.value = true
  try {
    const res: any = await simulateEinvoice(shipForm.id)
    if (res.code === 200 && res.data) {
      shipForm.einvoiceNo = res.data.einvoiceNo
      shipForm.einvoicePath = res.data.einvoicePath
      ElMessage.success(res.msg || '已模拟开票')
    } else {
      ElMessage.error(res.msg || '模拟开票失败')
    }
  } finally {
    simulating.value = false
  }
}

const handleConfirmShip = async () => {
  if (!shipForm.shipTime) {
    ElMessage.warning('请填写发货时间')
    return
  }
  shipSaving.value = true
  try {
    await confirmOutbound({
      id: shipForm.id,
      shipTime: shipForm.shipTime,
      einvoiceNo: shipForm.einvoiceNo,
      einvoicePath: shipForm.einvoicePath
    })
    ElMessage.success('已发货，等待医院确认收货')
    shipVisible.value = false
    tableRef.value?.reload()
  } catch {
    tableRef.value?.reload()
  } finally {
    shipSaving.value = false
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该草稿出库单？', '提示', { type: 'warning' })
  await deleteOutbound(row.id)
  tableRef.value?.reload()
}

const openPaid = (row: any) => {
  paidForm.id = row.id
  paidForm.invoiceNo = row.invoiceNo
  paidForm.totalAmount = Number(row.totalAmount || 0)
  paidForm.paidStatus = row.paidStatus || '未回款'
  paidForm.paidAmount = Number(row.paidAmount || 0)
  paidVisible.value = true
}

const handleMarkPaid = async () => {
  paidSaving.value = true
  try {
    await markPaid({
      id: paidForm.id,
      paidStatus: paidForm.paidStatus,
      paidAmount: paidForm.paidAmount
    })
    ElMessage.success('回款状态已更新')
    paidVisible.value = false
    tableRef.value?.reload()
  } finally {
    paidSaving.value = false
  }
}

const openReversal = async (row: any) => {
  const res: any = await loadOutboundDetail(row.id)
  const data = res.data || {}
  const items = (data.items || [])
    .map((it: any) => ({
      drugId: it.drugId,
      drugName: it.drugName,
      batchNo: it.batchNo,
      expireDate: it.expireDate,
      salePrice: it.salePrice,
      qualityStatus: it.qualityStatus || '合格',
      qtyOrigin: it.qty,
      remainingQty: Number(it.remainingQty ?? it.qty ?? 0),
      qty: Number(it.remainingQty ?? 0)
    }))
    .filter((it: any) => it.remainingQty > 0)
  if (!items.length) {
    ElMessage.warning('该发票已全部红冲，没有可冲数量')
    return
  }
  revForm.originalInvoiceNo = data.invoiceNo
  revForm.bizDate = new Date().toISOString().slice(0, 10)
  revForm.remark = ''
  revForm.items = items
  revVisible.value = true
}

const handleSaveReversal = async () => {
  const items = revForm.items.filter((it: any) => Number(it.qty) > 0)
  if (!items.length) {
    ElMessage.warning('请填写本次红冲数量')
    return
  }
  revSaving.value = true
  try {
    await saveReversal({
      originalInvoiceNo: revForm.originalInvoiceNo,
      bizDate: revForm.bizDate,
      remark: revForm.remark,
      items
    })
    ElMessage.success('红冲草稿已保存，请在列表中确认红冲')
    revVisible.value = false
    tableRef.value?.reload()
  } finally {
    revSaving.value = false
  }
}

onMounted(async () => {
  const [c, w, d]: any[] = await Promise.all([
    loadAllCustomerForSelect(),
    loadAllWarehouseForSelect(),
    loadAllDrugForSelect()
  ])
  customers.value = c.data || []
  warehouses.value = w.data || []
  drugs.value = d.data || []
})
</script>

<style scoped>
.item-toolbar {
  margin: 8px 0;
}
.rev-hint {
  margin: 0 0 12px;
  color: #667;
  font-size: 13px;
}
.upload-ok {
  margin-left: 8px;
  color: #67c23a;
  font-size: 13px;
}
.inv-sub {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}
</style>
