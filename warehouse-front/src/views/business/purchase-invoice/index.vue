<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">供应商发票查询</h1>
      <p class="page-header-desc">采购部按供应商发票、采购单号核对金额与数量；含已入库与未入库草稿。</p>
    </div>

    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="发票号">
          <el-input v-model="searchParams.invoiceNo" placeholder="供应商发票号" clearable />
        </el-form-item>
        <el-form-item label="采购单号">
          <el-input v-model="searchParams.orderNo" placeholder="采购入库单号" clearable />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="searchParams.supplierId" filterable clearable placeholder="全部" style="width: 220px">
            <el-option v-for="s in suppliers" :key="s.id" :label="`${s.id} ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库">
          <el-select v-model="searchParams.inboundStatus" clearable placeholder="全部" style="width: 140px">
            <el-option label="已入库" value="已入库" />
            <el-option label="未入库" value="未入库" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 240px"
          />
        </el-form-item>
      </SearchForm>
    </el-card>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">发票数</div>
        <div class="stat-value">{{ summary.invoiceCount ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">采购单数</div>
        <div class="stat-value">{{ summary.orderCount ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">数量合计</div>
        <div class="stat-value">{{ money(summary.qty) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">金额合计</div>
        <div class="stat-value">{{ money(summary.amount) }}</div>
      </div>
    </div>

    <el-card>
      <el-table :data="rows" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="invoiceNo" label="供应商发票" min-width="160" />
        <el-table-column prop="orderNo" label="采购单号" min-width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="140" />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="bizDate" label="日期" width="120" />
        <el-table-column label="入库" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.inboundStatus === '已入库' ? 'success' : 'warning'">
              {{ row.inboundStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="drugName" label="品种" min-width="180" />
        <el-table-column prop="batchNo" label="批号" min-width="120" />
        <el-table-column prop="receiveQty" label="数量" width="90" />
        <el-table-column prop="stockInQty" label="入库数量" width="100" />
        <el-table-column prop="purchasePrice" label="采购价" width="100" />
        <el-table-column prop="amount" label="金额" width="110" />
        <el-table-column prop="qualityStatus" label="质量" width="90" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openOrder(row)">入库单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="limit"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SearchForm from '@/components/SearchForm.vue'
import { loadPurchaseInvoiceLedger } from '@/api/purchase'
import { loadAllSupplierForSelect } from '@/api/supplier'

const router = useRouter()
const suppliers = ref<any[]>([])
const rows = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const limit = ref(20)
const total = ref(0)
const dateRange = ref<string[]>([])
const summary = reactive<any>({ invoiceCount: 0, orderCount: 0, qty: 0, amount: 0 })

const searchParams = reactive({
  invoiceNo: '',
  orderNo: '',
  supplierId: undefined as number | undefined,
  inboundStatus: ''
})

const money = (n?: number) => Number(n || 0).toFixed(2)

const query = () => ({
  page: page.value,
  limit: limit.value,
  invoiceNo: searchParams.invoiceNo || undefined,
  orderNo: searchParams.orderNo || undefined,
  supplierId: searchParams.supplierId,
  inboundStatus: searchParams.inboundStatus || undefined,
  startDate: dateRange.value?.[0],
  endDate: dateRange.value?.[1]
})

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await loadPurchaseInvoiceLedger(query())
    rows.value = res.data || []
    total.value = Number(res.count || 0)
    const s = res.summary || {}
    summary.invoiceCount = Number(s.invoiceCount ?? s.invoicecount ?? 0)
    summary.orderCount = Number(s.orderCount ?? s.ordercount ?? 0)
    summary.qty = Number(s.qty ?? 0)
    summary.amount = Number(s.amount ?? 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchParams.invoiceNo = ''
  searchParams.orderNo = ''
  searchParams.supplierId = undefined
  searchParams.inboundStatus = ''
  dateRange.value = []
  page.value = 1
  loadData()
}

const openOrder = (row: any) => {
  router.push({ path: '/business/purchase', query: { orderNo: row.orderNo } })
}

onMounted(async () => {
  const res: any = await loadAllSupplierForSelect()
  suppliers.value = res.data || []
  loadData()
})
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}
.stat-card {
  background: var(--bg-primary);
  border-radius: var(--border-radius-lg);
  padding: 14px 16px;
  box-shadow: var(--shadow-sm);
}
.stat-label { font-size: 12px; color: var(--text-secondary); }
.stat-value { font-size: 22px; font-weight: 600; }
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
