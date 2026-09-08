<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">日清检查</h1>
      <p class="page-header-desc">当天草稿清完、药品采码、现结回齐后可日清。月结未回款只提示，不挡日清。</p>
    </div>

    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-form inline>
          <el-form-item label="业务日期">
            <el-date-picker
              v-model="bizDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              :clearable="false"
              @change="loadData"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
            <el-button
              type="success"
              :disabled="!checklist?.cleared || checklist?.closed"
              :loading="confirming"
              @click="handleConfirm"
            >标记日清</el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="checklist"
          :title="statusTitle"
          :type="checklist.closed ? 'success' : checklist.cleared ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />
      </div>
    </el-card>

    <div class="stats-row" v-if="summary">
      <div class="stat-card" v-for="card in statCards" :key="card.key">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value" :class="card.danger ? 'is-danger' : ''">{{ card.value }}</div>
        <div class="stat-hint">{{ card.hint }}</div>
      </div>
    </div>

    <div class="check-grid" v-if="checklist">
      <el-card v-for="item in checkItems" :key="item.key" class="check-card">
        <div class="check-head">
          <el-tag :type="item.pass ? 'success' : item.block ? 'danger' : 'info'" size="small">
            {{ item.pass ? '已清' : item.block ? '未清' : '待对账' }}
          </el-tag>
          <span class="check-title">{{ item.title }}</span>
          <span class="check-count">{{ item.count }}</span>
        </div>
        <p class="check-desc">{{ item.desc }}</p>
        <el-button type="primary" link @click="go(item.href)">去处理</el-button>
      </el-card>
    </div>

    <el-card v-if="checklist?.draftPurchases?.length" class="section-card">
      <template #header>进货草稿</template>
      <el-table :data="checklist.draftPurchases" size="small" stripe>
        <el-table-column prop="orderNo" label="单号" min-width="150" />
        <el-table-column prop="partnerName" label="供应商" min-width="140" />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="100">
          <template #default>
            <el-button type="primary" link @click="go('/business/purchase')">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="checklist?.draftOutbounds?.length" class="section-card">
      <template #header>出库 / 红冲草稿</template>
      <el-table :data="checklist.draftOutbounds" size="small" stripe>
        <el-table-column prop="orderNo" label="单号" min-width="140" />
        <el-table-column prop="invoiceNo" label="发票号" min-width="190" />
        <el-table-column prop="partnerName" label="客户" min-width="140" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column label="操作" width="100">
          <template #default>
            <el-button type="primary" link @click="go('/business/outbound')">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="checklist?.draftSurplus?.length" class="section-card">
      <template #header>升益草稿</template>
      <el-table :data="checklist.draftSurplus" size="small" stripe>
        <el-table-column prop="orderNo" label="单号" min-width="150" />
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="100">
          <template #default>
            <el-button type="primary" link @click="go('/business/surplus')">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="checklist?.missingTraces?.length" class="section-card">
      <template #header>药品出库未采码（器械不要求）</template>
      <el-table :data="checklist.missingTraces" size="small" stripe>
        <el-table-column prop="invoiceNo" label="发票号" min-width="190" />
        <el-table-column prop="customerName" label="客户" min-width="140" />
        <el-table-column prop="drugName" label="品种" min-width="140" />
        <el-table-column prop="batchNo" label="批号" width="130" />
        <el-table-column prop="spdid" label="SPDID" min-width="170" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button type="primary" link @click="goTrace(row.invoiceNo)">去采码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="checklist?.cashUnpaid?.length" class="section-card">
      <template #header>现结未回齐（挡日清）</template>
      <el-table :data="checklist.cashUnpaid" size="small" stripe>
        <el-table-column prop="invoiceNo" label="发票号" min-width="190" />
        <el-table-column prop="partnerName" label="客户" min-width="140" />
        <el-table-column prop="paidStatus" label="回款" width="110" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column label="操作" width="100">
          <template #default>
            <el-button type="primary" link @click="go('/business/outbound')">标记回款</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="checklist?.monthlyUnpaid?.length" class="section-card">
      <template #header>月结未回齐（不挡日清，月底对账）</template>
      <el-table :data="checklist.monthlyUnpaid" size="small" stripe>
        <el-table-column prop="invoiceNo" label="发票号" min-width="190" />
        <el-table-column prop="partnerName" label="客户" min-width="140" />
        <el-table-column prop="paidStatus" label="回款" width="110" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column label="操作" width="100">
          <template #default>
            <el-button type="primary" link @click="go('/business/outbound')">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="checklist?.pendingReceipts?.length" class="section-card">
      <template #header>医院待收货（不挡日清）</template>
      <el-table :data="checklist.pendingReceipts" size="small" stripe>
        <el-table-column prop="invoiceNo" label="发票号" min-width="190" />
        <el-table-column prop="partnerName" label="医院" min-width="140" />
        <el-table-column prop="totalAmount" label="金额" width="110" />
        <el-table-column label="操作" width="100">
          <template #default>
            <el-button type="primary" link @click="go('/business/receipt')">去签收</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmDailyClose, loadDailyCloseChecklist } from '@/api/dailyClose'

type Checklist = {
  bizDate: string
  cleared: boolean
  closed: boolean
  closedAt?: string
  closedByName?: string
  blockerCount: number
  summary: {
    draftPurchaseCount: number
    draftOutboundCount: number
    draftSurplusCount: number
    confirmedPurchaseCount: number
    confirmedPurchaseAmount: number
    confirmedOutboundCount: number
    confirmedOutboundAmount: number
    missingTraceCount: number
    cashUnpaidCount: number
    monthlyUnpaidCount: number
    pendingReceiptCount: number
  }
  draftPurchases: any[]
  draftOutbounds: any[]
  draftSurplus: any[]
  missingTraces: any[]
  cashUnpaid: any[]
  monthlyUnpaid: any[]
  pendingReceipts: any[]
}

const pad = (n: number) => String(n).padStart(2, '0')
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const router = useRouter()
const bizDate = ref(today())
const loading = ref(false)
const confirming = ref(false)
const checklist = ref<Checklist | null>(null)

const summary = computed(() => checklist.value?.summary)

const money = (n?: number) => Number(n || 0).toFixed(2)

const statusTitle = computed(() => {
  if (!checklist.value) return ''
  if (checklist.value.closed) {
    return `${checklist.value.bizDate} 已日清${checklist.value.closedByName ? '（' + checklist.value.closedByName + '）' : ''}`
  }
  if (checklist.value.cleared) {
    return `${checklist.value.bizDate} 检查通过，可以日清`
  }
  return `${checklist.value.bizDate} 日清未完成，还有 ${checklist.value.blockerCount} 项要处理`
})

const statCards = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { key: 'in', label: '已确认入库', value: s.confirmedPurchaseCount, hint: `金额 ${money(s.confirmedPurchaseAmount)}`, danger: false },
    { key: 'out', label: '已确认出库', value: s.confirmedOutboundCount, hint: `金额 ${money(s.confirmedOutboundAmount)}`, danger: false },
    { key: 'draft', label: '未清草稿', value: s.draftPurchaseCount + s.draftOutboundCount + s.draftSurplusCount, hint: '进货 / 出库 / 升益', danger: s.draftPurchaseCount + s.draftOutboundCount + s.draftSurplusCount > 0 },
    { key: 'trace', label: '未采码明细', value: s.missingTraceCount, hint: '仅药品出库', danger: s.missingTraceCount > 0 },
    { key: 'cash', label: '现结未回齐', value: s.cashUnpaidCount, hint: '挡日清', danger: s.cashUnpaidCount > 0 },
    { key: 'month', label: '月结未回齐', value: s.monthlyUnpaidCount, hint: '不挡日结', danger: false }
  ]
})

const checkItems = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { key: 'p', title: '进货草稿', count: s.draftPurchaseCount, pass: s.draftPurchaseCount === 0, block: s.draftPurchaseCount > 0, desc: '当天进货单须确认入库或删除草稿', href: '/business/purchase' },
    { key: 'o', title: '出库草稿', count: s.draftOutboundCount, pass: s.draftOutboundCount === 0, block: s.draftOutboundCount > 0, desc: '当天出库/红冲须确认过账或删除草稿', href: '/business/outbound' },
    { key: 's', title: '升益草稿', count: s.draftSurplusCount, pass: s.draftSurplusCount === 0, block: s.draftSurplusCount > 0, desc: '盘点升益/损耗须确认过账或删除', href: '/business/surplus' },
    { key: 't', title: '药品追溯码', count: s.missingTraceCount, pass: s.missingTraceCount === 0, block: s.missingTraceCount > 0, desc: '已确认药品出库须按 SPDID 采码', href: '/business/trace' },
    { key: 'c', title: '现结回款', count: s.cashUnpaidCount, pass: s.cashUnpaidCount === 0, block: s.cashUnpaidCount > 0, desc: '现结客户当天应收齐或标已回款', href: '/business/outbound' },
    { key: 'm', title: '月结挂账', count: s.monthlyUnpaidCount, pass: s.monthlyUnpaidCount === 0, block: false, desc: '月底在月结对账处理，不挡日清', href: '/business/monthly-close' },
    { key: 'r', title: '医院待收货', count: s.pendingReceiptCount || 0, pass: (s.pendingReceiptCount || 0) === 0, block: false, desc: '已发货待医院签收，不挡日清', href: '/business/receipt' }
  ]
})

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await loadDailyCloseChecklist(bizDate.value)
    checklist.value = res.data
  } finally {
    loading.value = false
  }
}

const handleConfirm = async () => {
  await ElMessageBox.confirm(`确认将 ${bizDate.value} 标记为已日清？`, '日清', { type: 'warning' })
  confirming.value = true
  try {
    await confirmDailyClose(bizDate.value)
    ElMessage.success('日清成功')
    await loadData()
  } finally {
    confirming.value = false
  }
}

const go = (href: string) => router.push(href)
const goTrace = (invoiceNo?: string) => {
  if (!invoiceNo) {
    router.push('/business/trace')
    return
  }
  router.push({ path: '/business/trace', query: { invoiceNo } })
}

onMounted(loadData)
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: var(--bg-primary);
  border-radius: var(--border-radius-lg);
  padding: 14px 16px;
  box-shadow: var(--shadow-sm);
}
.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-primary);
}
.stat-value.is-danger {
  color: var(--danger-color);
}
.stat-hint {
  font-size: 12px;
  color: var(--text-placeholder);
}
.check-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.check-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.check-title {
  font-weight: 600;
  flex: 1;
}
.check-count {
  font-size: 18px;
  font-weight: 600;
}
.check-desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-secondary);
}
.section-card {
  margin-bottom: 16px;
}
@media (max-width: 1100px) {
  .stats-row,
  .check-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
