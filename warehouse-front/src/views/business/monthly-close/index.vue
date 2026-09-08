<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">月结对账</h1>
      <p class="page-header-desc">按自然月汇总客户应收、供应商进货。月结未回款可挂账，确认后标记已月结。</p>
    </div>

    <el-card class="toolbar-card">
      <el-form inline>
        <el-form-item label="月份">
          <el-date-picker
            v-model="yearMonth"
            type="month"
            value-format="YYYY-MM"
            :clearable="false"
            @change="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
          <el-button type="success" :disabled="statement?.closed" :loading="confirming" @click="handleConfirm">
            标记月结
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="statement"
        :title="statusTitle"
        :type="statement.closed ? 'success' : 'info'"
        :closable="false"
        show-icon
      />
    </el-card>

    <div class="stats-row" v-if="summary">
      <div class="stat-card" v-for="card in statCards" :key="card.key">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-hint">{{ card.hint }}</div>
      </div>
    </div>

    <el-card class="section-card">
      <template #header>客户应收</template>
      <el-table :data="statement?.customers || []" size="small" stripe>
        <el-table-column prop="partnerName" label="客户" min-width="180" />
        <el-table-column prop="payType" label="结算" width="90" />
        <el-table-column prop="orderCount" label="单数" width="80" />
        <el-table-column prop="totalAmount" label="应收" width="120" />
        <el-table-column prop="paidAmount" label="已回" width="120" />
        <el-table-column prop="unpaidAmount" label="未回" width="120" />
      </el-table>
    </el-card>

    <el-card class="section-card">
      <template #header>供应商进货</template>
      <el-table :data="statement?.suppliers || []" size="small" stripe>
        <el-table-column prop="partnerName" label="供应商" min-width="180" />
        <el-table-column prop="orderCount" label="单数" width="80" />
        <el-table-column prop="totalAmount" label="进货金额" width="140" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmMonthlyClose, loadMonthlyCloseStatement } from '@/api/monthlyClose'

const pad = (n: number) => String(n).padStart(2, '0')
const currentMonth = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

const yearMonth = ref(currentMonth())
const loading = ref(false)
const confirming = ref(false)
const statement = ref<any>(null)
const summary = computed(() => statement.value?.summary)
const money = (n?: number) => Number(n || 0).toFixed(2)

const statusTitle = computed(() => {
  if (!statement.value) return ''
  if (statement.value.closed) {
    return `${statement.value.yearMonth} 已月结${statement.value.closedByName ? '（' + statement.value.closedByName + '）' : ''}`
  }
  return `${statement.value.yearMonth} 对账清单已生成，确认后标记月结`
})

const statCards = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { key: 'c', label: '客户数', value: s.customerCount, hint: '本月有出库' },
    { key: 'sa', label: '销售金额', value: money(s.salesAmount), hint: '已确认出库' },
    { key: 'u', label: '未回款', value: money(s.unpaidAmount), hint: '含月结挂账' },
    { key: 'p', label: '进货金额', value: money(s.purchaseAmount), hint: `${s.supplierCount || 0} 家供应商` }
  ]
})

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await loadMonthlyCloseStatement(yearMonth.value)
    statement.value = res.data
  } finally {
    loading.value = false
  }
}

const handleConfirm = async () => {
  await ElMessageBox.confirm(`确认将 ${yearMonth.value} 标记为已月结？`, '月结', { type: 'warning' })
  confirming.value = true
  try {
    await confirmMonthlyClose(yearMonth.value)
    ElMessage.success('月结成功')
    await loadData()
  } finally {
    confirming.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar-card { margin-bottom: 16px; }
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: var(--bg-primary);
  border-radius: var(--border-radius-lg);
  padding: 14px 16px;
  box-shadow: var(--shadow-sm);
}
.stat-label { font-size: 12px; color: var(--text-secondary); }
.stat-value { font-size: 22px; font-weight: 600; }
.stat-hint { font-size: 12px; color: var(--text-placeholder); }
.section-card { margin-bottom: 16px; }
</style>
