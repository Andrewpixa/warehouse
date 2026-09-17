<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">开票员作业（信息部 · IT）</h1>
      <p class="page-header-desc">客户要货先进系统，预处理过关后再模拟开票。网单不能直接发货。改票成本高，价格须事先谈妥。</p>
    </div>

    <el-card class="flow-card">
      <template #header>开票员流程 · 三、销售订单入口</template>
      <p class="flow-lead">解决：客户要货怎么进系统，并在开票前先过合规与商务门槛。</p>
      <div class="flow">
        <div class="box start">1 客户需求<br /><small>整条销售链起点</small></div>
        <span class="arrow">↓</span>
        <div class="split">
          <div class="box plat">2 全药网 / 药交平台网单<br /><small>自动或半自动进系统，仍要预处理</small></div>
          <div class="box off">3 口头、表格等线下计划<br /><small>开票员手工录入 BMS 订单</small></div>
        </div>
        <span class="arrow">↓ 汇入同一套风控</span>
        <div class="box gate">4 订单预处理<br /><small>信誉额 · 客户证照 · 货品分货 · 价格锁定</small></div>
        <span class="arrow">↓ 通过才开票</span>
        <div class="box end">开票员开票 → 下载票面</div>
      </div>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="5. 开票价格应提前与客户协商好，尽量减少改票。改票要退货证明、计划员签字，差额超 1000 还要信息运营总监加签。"
        style="margin-top: 12px"
      />
      <div class="flow-actions">
        <el-button type="primary" :loading="importing" @click="doImport">模拟接入网单</el-button>
        <el-button @click="$router.push('/business/outbound')">开票员录入 BMS 订单</el-button>
        <el-button @click="$router.push('/business/credit')">信誉额</el-button>
        <el-button @click="$router.push('/business/allocate')">分货</el-button>
        <el-button @click="$router.push('/business/customer')">客户证照</el-button>
      </div>
    </el-card>

    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="单号">
          <el-input v-model="searchParams.orderNo" placeholder="出库单号" clearable />
        </el-form-item>
        <el-form-item label="入口">
          <el-select v-model="searchParams.orderChannel" clearable placeholder="全部" style="width: 150px">
            <el-option label="平台网单" value="PLATFORM" />
            <el-option label="线下 BMS" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="预处理">
          <el-select v-model="searchParams.preprocessStatus" clearable placeholder="全部" style="width: 140px">
            <el-option label="待预处理" value="待预处理" />
            <el-option label="已通过" value="已通过" />
            <el-option label="未通过" value="未通过" />
          </el-select>
        </el-form-item>
        <el-form-item label="单据">
          <el-select v-model="searchParams.status" placeholder="状态" style="width: 140px">
            <el-option label="待开票草稿" value="草稿" />
            <el-option label="已确认" value="已确认" />
          </el-select>
        </el-form-item>
      </SearchForm>
      <CrudTable ref="tableRef" :load-api="loadAllOutbound" :search-params="searchParams">
        <el-table-column prop="orderNo" label="BMS单号" min-width="140" />
        <el-table-column label="入口" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.orderChannel === 'PLATFORM' ? 'warning' : 'info'">
              {{ row.orderChannel === 'PLATFORM' ? '网单' : '线下BMS' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="platformNo" label="平台单号" min-width="140" />
        <el-table-column prop="customerName" label="客户" min-width="130" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column label="预处理" min-width="220">
          <template #default="{ row }">
            <el-tag size="small" :type="row.preprocessStatus === '已通过' ? 'success' : row.preprocessStatus === '未通过' ? 'danger' : 'info'">
              {{ row.preprocessStatus || '待预处理' }}
            </el-tag>
            <div class="mini">{{ row.preprocessRemark }}</div>
          </template>
        </el-table-column>
        <el-table-column label="票面" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.einvoicePath ? 'success' : 'warning'">{{ row.einvoicePath ? '已开' : '未开' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '草稿'" type="primary" link @click="openEdit(row)">修改</el-button>
            <el-button v-if="row.status === '草稿'" type="warning" link :loading="preping === row.id" @click="doPrep(row)">预处理</el-button>
            <el-button v-if="row.status === '草稿'" type="success" link :loading="issuing === row.id" @click="issue(row)">开票</el-button>
            <el-button v-if="row.einvoicePath" type="primary" link @click="download(row)">下载</el-button>
            <el-button v-if="row.einvoicePath" type="primary" link @click="openFile(row.einvoicePath)">查看</el-button>
          </template>
        </el-table-column>
      </CrudTable>
    </el-card>

    <el-dialog v-model="editVisible" title="修改 BMS 订单（草稿）" width="920px" destroy-on-close>
      <el-alert type="warning" :closable="false" title="改单价会作废已通过的预处理，须重新过闸再开票。请尽量一次谈妥价格。" style="margin-bottom: 12px" />
      <el-form label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="入口">
              <el-select v-model="editForm.orderChannel" style="width: 100%">
                <el-option label="平台网单" value="PLATFORM" />
                <el-option label="线下 BMS" value="OFFLINE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="平台单号">
              <el-input v-model="editForm.platformNo" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="editForm.remark" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-table :data="editForm.items" border size="small">
        <el-table-column prop="drugName" label="品种" min-width="160" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0.001" :controls="false" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="成交价" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.salePrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存并待预处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllOutbound, loadOutboundDetail, saveOutbound, simulateEinvoice, preprocessOrder, importPlatformOrder } from '@/api/outbound'
import { getImageUrl } from '@/api/file'
import { BASE_URL } from '@/utils/request'

const tableRef = ref()
const issuing = ref<number | null>(null)
const preping = ref<number | null>(null)
const importing = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const editForm = reactive<any>({ items: [] })
const searchParams = reactive({
  page: 1,
  limit: 10,
  orderNo: '',
  invoiceNo: '',
  status: '草稿',
  orderChannel: '',
  preprocessStatus: ''
})

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.orderNo = ''
  searchParams.status = '草稿'
  searchParams.orderChannel = ''
  searchParams.preprocessStatus = ''
  tableRef.value?.reload()
}

const doImport = async () => {
  importing.value = true
  try {
    const res: any = await importPlatformOrder()
    if (res.code === 200) {
      ElMessage.success(res.msg)
      tableRef.value?.reload()
    } else {
      ElMessage.error(res.msg || '接入失败')
    }
  } finally {
    importing.value = false
  }
}

const doPrep = async (row: any) => {
  preping.value = row.id
  try {
    const res: any = await preprocessOrder(row.id)
    if (res.code === 200) {
      ElMessage[res.data?.preprocessStatus === '已通过' ? 'success' : 'warning'](res.data?.preprocessRemark || res.msg)
      tableRef.value?.reload()
    } else {
      ElMessage.error(res.msg || '预处理失败')
    }
  } finally {
    preping.value = null
  }
}

const issue = async (row: any) => {
  issuing.value = row.id
  try {
    const res: any = await simulateEinvoice(row.id)
    if (res.code === 200) {
      ElMessage.success(res.msg || '已开票')
      tableRef.value?.reload()
    } else {
      ElMessage.error(res.msg || '开票失败')
    }
  } finally {
    issuing.value = null
  }
}

const openEdit = async (row: any) => {
  const res: any = await loadOutboundDetail(row.id)
  Object.assign(editForm, res.data || { items: [] })
  if (!editForm.items) editForm.items = []
  editVisible.value = true
}

const saveEdit = async () => {
  saving.value = true
  try {
    const res: any = await saveOutbound({ ...editForm })
    if (res.code === 200) {
      ElMessage.success('已保存，请重新预处理')
      editVisible.value = false
      tableRef.value?.reload()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const download = (row: any) => {
  window.open(BASE_URL + '/outbound/downloadEinvoice?id=' + row.id, '_blank')
}

const openFile = (path: string) => {
  window.open(getImageUrl(path), '_blank')
}
</script>

<style scoped>
.flow-card { margin-bottom: 16px; }
.flow-lead { color: var(--text-secondary); margin: 0 0 12px; font-size: 13px; }
.flow { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.split { display: flex; gap: 16px; flex-wrap: wrap; justify-content: center; width: 100%; }
.box {
  border: 1px solid var(--border-light);
  border-radius: 10px;
  padding: 12px 16px;
  text-align: center;
  min-width: 220px;
  background: var(--bg-primary);
  font-weight: 600;
  font-size: 14px;
}
.box small { display: block; font-weight: 400; color: var(--text-secondary); margin-top: 4px; }
.box.start { background: #fef9c3; border-color: #facc15; }
.box.plat { background: #e0f2fe; }
.box.off { background: #ede9fe; }
.box.gate { background: #ffedd5; min-width: 420px; }
.box.end { background: #dcfce7; }
.arrow { color: var(--text-secondary); }
.flow-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.mini { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
</style>
