<template>
  <div class="workbench">
    <header class="topbar">
      <div class="brand">
        <span class="logo">药</span>
        <div>
          <strong>员工工作台</strong>
          <small>按账号权限查询发票、库存、红冲</small>
        </div>
      </div>
      <div class="user">
        <span>{{ userName }}</span>
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="main">
      <el-empty v-if="!canInvoice && !canStock && !canReversal" description="当前账号没有员工查询权限，请管理员在 Web 端分配" />

      <template v-else>
        <el-tabs v-model="tab">
          <el-tab-pane v-if="canInvoice" label="发票查询" name="invoice" />
          <el-tab-pane v-if="canInvoice" label="未回款列表" name="unpaid" />
          <el-tab-pane v-if="canStock" label="批号库存" name="stock" />
          <el-tab-pane v-if="canReversal" label="红冲记录" name="reversal" />
        </el-tabs>

        <section v-show="tab === 'invoice'" class="panel">
          <div class="search-hero">
            <el-input
              v-model="invoiceNo"
              size="large"
              maxlength="20"
              placeholder="输入 20 位发票号，例如 20260907000100000001"
              clearable
              @keyup.enter="searchInvoice"
            >
              <template #prepend>发票号</template>
            </el-input>
            <el-button type="primary" size="large" :loading="invoiceLoading" @click="searchInvoice">查询</el-button>
          </div>
          <div v-if="recent.length" class="recent">
            <span>最近查询：</span>
            <el-tag
              v-for="item in recent"
              :key="item"
              class="recent-tag"
              effect="plain"
              @click="quickSearch(item)"
            >{{ item }}</el-tag>
          </div>

          <el-empty v-if="!order && !invoiceLoading" description="输入发票号查询出库明细、回款、红冲和追溯码" />

          <template v-if="order">
            <el-descriptions :column="3" border class="block">
              <el-descriptions-item label="发票号">{{ order.invoiceNo }}</el-descriptions-item>
              <el-descriptions-item label="出库单号">{{ order.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="单据">
                <el-tag :type="order.orderType === '红冲' ? 'danger' : 'primary'" size="small">{{ order.orderType || '正常' }}</el-tag>
                <span v-if="order.originalInvoiceNo" class="paid-amt">原票 {{ order.originalInvoiceNo }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="回款">
                <el-tag :type="paidType(order.paidStatus)" size="small">{{ order.paidStatus || '未回款' }}</el-tag>
                <span class="paid-amt">已回 {{ order.paidAmount || 0 }} / {{ order.totalAmount || 0 }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="客户">{{ order.customerName }}</el-descriptions-item>
              <el-descriptions-item label="仓库">{{ order.warehouseName }}</el-descriptions-item>
              <el-descriptions-item label="出库日期">{{ order.bizDate }}</el-descriptions-item>
              <el-descriptions-item label="单据状态">{{ order.status }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
            </el-descriptions>

            <h3 class="section-title">业务明细</h3>
            <el-table :data="order.items || []" border stripe>
              <el-table-column prop="drugName" label="药品" min-width="160" />
              <el-table-column prop="drugSpec" label="规格" min-width="120" />
              <el-table-column prop="batchNo" label="批号" width="130" />
              <el-table-column prop="qty" label="数量" width="80" />
              <el-table-column v-if="order.orderType !== '红冲'" prop="remainingQty" label="还可红冲" width="100" />
              <el-table-column prop="salePrice" label="售价" width="90" />
              <el-table-column prop="amount" label="金额" width="90" />
              <el-table-column prop="spdid" label="SPDID" min-width="170" />
            </el-table>

            <template v-if="reversals.length">
              <h3 class="section-title">关联红冲</h3>
              <el-table :data="reversalLines" border stripe>
                <el-table-column prop="invoiceNo" label="红冲发票号" min-width="190" />
                <el-table-column prop="orderNo" label="红冲单号" min-width="140" />
                <el-table-column prop="status" label="状态" width="90" />
                <el-table-column prop="drugName" label="药品" min-width="140" />
                <el-table-column prop="batchNo" label="批号" width="120" />
                <el-table-column prop="qty" label="冲减数量" width="100" />
                <el-table-column prop="bizDate" label="日期" width="120" />
              </el-table>
            </template>

            <h3 class="section-title">追溯码</h3>
            <el-table v-if="traces.length" :data="traces" border stripe>
              <el-table-column prop="code" label="追溯码" min-width="200" />
              <el-table-column prop="spdid" label="SPDID" min-width="170" />
              <el-table-column prop="drugName" label="药品" min-width="140" />
              <el-table-column prop="batchNo" label="批号" width="120" />
              <el-table-column prop="packLevel" label="包装层级" width="110" />
              <el-table-column prop="status" label="状态" width="80" />
            </el-table>
            <el-empty v-else description="该发票尚未采集追溯码" :image-size="72" />
          </template>
        </section>

        <section v-show="tab === 'unpaid'" class="panel">
          <div class="unpaid-filter">
            <el-select v-model="unpaidQuery.customerId" clearable filterable placeholder="按客户筛选" style="width: 280px">
              <el-option v-for="c in customers" :key="c.id" :label="`${c.id} ${c.name}`" :value="c.id" />
            </el-select>
            <el-input v-model="unpaidQuery.invoiceNo" clearable placeholder="发票号模糊查询" style="width: 240px" @keyup.enter="loadUnpaid" />
            <el-button type="primary" :loading="unpaidLoading" @click="loadUnpaid">查询</el-button>
          </div>
          <el-table :data="unpaidRows" border stripe v-loading="unpaidLoading" @row-click="openFromUnpaid">
            <el-table-column prop="invoiceNo" label="发票号" min-width="200" />
            <el-table-column prop="customerName" label="客户" min-width="180" />
            <el-table-column prop="bizDate" label="出库日期" width="120" />
            <el-table-column prop="totalAmount" label="出库金额" width="110" />
            <el-table-column prop="paidAmount" label="已回金额" width="110" />
            <el-table-column label="回款" width="110">
              <template #default="{ row }">
                <el-tag :type="paidType(row.paidStatus)" size="small">{{ row.paidStatus || '未回款' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="100">
              <template #default>
                <el-button type="primary" link>查看明细</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="unpaidTotal"
              :page-size="unpaidQuery.limit"
              :current-page="unpaidQuery.page"
              @current-change="onUnpaidPage"
            />
          </div>
        </section>

        <section v-show="tab === 'stock'" class="panel">
          <div class="unpaid-filter">
            <el-input v-model="stockQuery.drugName" clearable placeholder="品种编号 / 通用名" style="width: 220px" @keyup.enter="loadStock" />
            <el-input v-model="stockQuery.batchNo" clearable placeholder="批号" style="width: 160px" @keyup.enter="loadStock" />
            <el-select v-model="stockQuery.warehouseId" clearable filterable placeholder="仓库" style="width: 200px">
              <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
            </el-select>
            <el-button type="primary" :loading="stockLoading" @click="loadStock">查询</el-button>
          </div>
          <el-table :data="stockRows" border stripe v-loading="stockLoading">
            <el-table-column prop="drugId" label="品种编号" width="110" />
            <el-table-column prop="drugName" label="品种" min-width="160" />
            <el-table-column prop="drugSpec" label="规格" min-width="120" />
            <el-table-column prop="batchNo" label="批号" width="140" />
            <el-table-column prop="warehouseName" label="仓库" min-width="140" />
            <el-table-column prop="qty" label="数量" width="90" />
            <el-table-column prop="qualityStatus" label="质量状态" width="100" />
            <el-table-column prop="expireDate" label="有效期至" width="120" />
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="stockTotal"
              :page-size="stockQuery.limit"
              :current-page="stockQuery.page"
              @current-change="onStockPage"
            />
          </div>
        </section>

        <section v-show="tab === 'reversal'" class="panel">
          <div class="search-hero">
            <el-input
              v-model="reversalInvoiceNo"
              size="large"
              maxlength="20"
              placeholder="输入原蓝字发票号，例如 20260907000100000001"
              clearable
              @keyup.enter="searchReversals"
            >
              <template #prepend>原发票</template>
            </el-input>
            <el-button type="primary" size="large" :loading="reversalLoading" @click="searchReversals">查询红冲</el-button>
          </div>
          <p class="hint">员工端只查红冲记录。开红冲、确认加回库存请在 Web 管理员端操作。</p>
          <el-empty v-if="!reversalRows.length && !reversalLoading" description="输入原发票号查看对应红冲单" />
          <el-table v-else :data="reversalRows" border stripe v-loading="reversalLoading">
            <el-table-column prop="invoiceNo" label="红冲发票号" min-width="190" />
            <el-table-column prop="orderNo" label="红冲单号" min-width="140" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column prop="customerName" label="客户" min-width="150" />
            <el-table-column prop="totalAmount" label="金额" width="100" />
            <el-table-column prop="bizDate" label="日期" width="120" />
            <el-table-column prop="remark" label="原因" min-width="160" />
          </el-table>
          <template v-if="reversalItemRows.length">
            <h3 class="section-title">红冲明细</h3>
            <el-table :data="reversalItemRows" border stripe>
              <el-table-column prop="invoiceNo" label="红冲发票号" min-width="190" />
              <el-table-column prop="drugName" label="药品" min-width="160" />
              <el-table-column prop="batchNo" label="批号" width="130" />
              <el-table-column prop="qty" label="冲减数量" width="100" />
              <el-table-column prop="salePrice" label="单价" width="90" />
              <el-table-column prop="amount" label="金额" width="90" />
            </el-table>
          </template>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { logout } from '../api/login'
import {
  loadAllBatchStock,
  loadAllCustomerForSelect,
  loadAllWarehouseForSelect,
  loadByInvoice,
  loadReversals,
  loadUnpaidOutbound
} from '../api/invoice'
import { hasPerm } from '../utils/perm'

const RECENT_KEY = 'employee_recent_invoices'

const props = defineProps<{ userName: string; permissions: string[] }>()
const emit = defineEmits<{ logout: [] }>()

const canInvoice = computed(() => hasPerm(props.permissions, 'sales:view'))
const canStock = computed(() => hasPerm(props.permissions, 'warehouse:view'))
const canReversal = computed(() => hasPerm(props.permissions, 'sales:return'))

const tab = ref('invoice')
const invoiceNo = ref('')
const invoiceLoading = ref(false)
const order = ref<any>(null)
const traces = ref<any[]>([])
const reversals = ref<any[]>([])
const recent = ref<string[]>(JSON.parse(localStorage.getItem(RECENT_KEY) || '[]'))

const customers = ref<any[]>([])
const unpaidLoading = ref(false)
const unpaidRows = ref<any[]>([])
const unpaidTotal = ref(0)
const unpaidQuery = reactive({
  page: 1,
  limit: 10,
  customerId: undefined as number | undefined,
  invoiceNo: ''
})

const warehouses = ref<any[]>([])
const stockLoading = ref(false)
const stockRows = ref<any[]>([])
const stockTotal = ref(0)
const stockQuery = reactive({
  page: 1,
  limit: 10,
  drugName: '',
  batchNo: '',
  warehouseId: undefined as number | undefined
})

const reversalInvoiceNo = ref('')
const reversalLoading = ref(false)
const reversalRows = ref<any[]>([])

const reversalLines = computed(() =>
  reversals.value.flatMap((row: any) =>
    (row.items || []).map((it: any) => ({
      ...it,
      invoiceNo: row.invoiceNo,
      orderNo: row.orderNo,
      status: row.status,
      bizDate: row.bizDate
    }))
  )
)

const reversalItemRows = computed(() =>
  reversalRows.value.flatMap((row: any) =>
    (row.items || []).map((it: any) => ({
      ...it,
      invoiceNo: row.invoiceNo
    }))
  )
)

const firstTab = () => {
  if (canInvoice.value) return 'invoice'
  if (canStock.value) return 'stock'
  if (canReversal.value) return 'reversal'
  return 'invoice'
}

watch(
  () => [canInvoice.value, canStock.value, canReversal.value],
  () => {
    const allowed = {
      invoice: canInvoice.value,
      unpaid: canInvoice.value,
      stock: canStock.value,
      reversal: canReversal.value
    }
    if (!allowed[tab.value as keyof typeof allowed]) {
      tab.value = firstTab()
    }
  },
  { immediate: true }
)

const paidType = (status?: string) => {
  if (status === '已回款') return 'success'
  if (status === '部分回款') return 'warning'
  return 'danger'
}

const remember = (no: string) => {
  const next = [no, ...recent.value.filter((x) => x !== no)].slice(0, 8)
  recent.value = next
  localStorage.setItem(RECENT_KEY, JSON.stringify(next))
}

const searchInvoice = async () => {
  const no = invoiceNo.value.trim()
  if (!no) {
    ElMessage.warning('请输入发票号')
    return
  }
  invoiceLoading.value = true
  try {
    const res: any = await loadByInvoice(no)
    order.value = res.data
    traces.value = res.traces || []
    reversals.value = res.reversals || []
    remember(no)
  } catch {
    order.value = null
    traces.value = []
    reversals.value = []
  } finally {
    invoiceLoading.value = false
  }
}

const quickSearch = (no: string) => {
  invoiceNo.value = no
  searchInvoice()
}

const openFromUnpaid = (row: any) => {
  tab.value = 'invoice'
  invoiceNo.value = row.invoiceNo
  searchInvoice()
}

const loadUnpaid = async () => {
  unpaidLoading.value = true
  try {
    const res: any = await loadUnpaidOutbound(unpaidQuery)
    unpaidRows.value = res.data || []
    unpaidTotal.value = res.count || 0
  } finally {
    unpaidLoading.value = false
  }
}

const onUnpaidPage = (page: number) => {
  unpaidQuery.page = page
  loadUnpaid()
}

const loadStock = async () => {
  stockLoading.value = true
  try {
    const res: any = await loadAllBatchStock(stockQuery)
    stockRows.value = res.data || []
    stockTotal.value = res.count || 0
  } finally {
    stockLoading.value = false
  }
}

const onStockPage = (page: number) => {
  stockQuery.page = page
  loadStock()
}

const searchReversals = async () => {
  const no = reversalInvoiceNo.value.trim()
  if (!no) {
    ElMessage.warning('请输入原发票号')
    return
  }
  reversalLoading.value = true
  try {
    const res: any = await loadReversals(no)
    reversalRows.value = res.data || []
    if (!reversalRows.value.length) {
      ElMessage.info('该发票没有红冲记录')
    }
  } catch {
    reversalRows.value = []
  } finally {
    reversalLoading.value = false
  }
}

const handleLogout = async () => {
  await logout()
  emit('logout')
}

onMounted(async () => {
  if (canInvoice.value) {
    try {
      const res: any = await loadAllCustomerForSelect()
      customers.value = res.data || []
    } catch {}
    loadUnpaid()
  }
  if (canStock.value) {
    try {
      const res: any = await loadAllWarehouseForSelect()
      warehouses.value = res.data || []
    } catch {}
    loadStock()
  }
})
</script>

<style scoped>
.workbench {
  min-height: 100vh;
  background: #f4f7fb;
}
.topbar {
  height: 64px;
  padding: 0 24px;
  background: #12324d;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  gap: 12px;
  align-items: center;
}
.logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #1b7fbf;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.brand small {
  display: block;
  opacity: 0.75;
  font-size: 12px;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user :deep(.el-button) {
  color: #cfe6f7;
}
.main {
  padding: 16px 24px 32px;
}
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  min-height: 520px;
}
.search-hero {
  display: flex;
  gap: 12px;
}
.search-hero :deep(.el-input) {
  flex: 1;
}
.recent {
  margin: 12px 0 8px;
  color: #667;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}
.recent-tag {
  cursor: pointer;
}
.block {
  margin-top: 16px;
}
.section-title {
  margin: 20px 0 10px;
  font-size: 15px;
}
.paid-amt {
  margin-left: 8px;
  color: #567;
  font-size: 13px;
}
.unpaid-filter {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.hint {
  margin: 12px 0 16px;
  color: #667;
  font-size: 13px;
}
</style>
