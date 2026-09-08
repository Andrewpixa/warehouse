<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">进货统计</h1>
      <p class="page-header-desc">按已确认采购入库单汇总数量与金额。</p>
    </div>
    <el-card class="toolbar-card">
      <el-form inline>
        <el-form-item label="日期">
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            :clearable="false"
            @change="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <div class="stats-row" v-if="summary">
      <div class="stat-card">
        <div class="stat-label">入库单数</div>
        <div class="stat-value">{{ summary.orderCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">入库数量</div>
        <div class="stat-value">{{ money(summary.qty) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">入库金额</div>
        <div class="stat-value">{{ money(summary.amount) }}</div>
      </div>
    </div>
    <el-card class="section-card">
      <template #header>按日</template>
      <el-table :data="days" size="small" stripe>
        <el-table-column prop="bizDate" label="日期" width="140" />
        <el-table-column prop="orderCount" label="单数" width="90" />
        <el-table-column prop="qty" label="数量" width="120" />
        <el-table-column prop="amount" label="金额" width="140" />
      </el-table>
    </el-card>
    <el-card class="section-card">
      <template #header>按品种</template>
      <el-table :data="drugs" size="small" stripe>
        <el-table-column prop="drugName" label="品种" min-width="180" />
        <el-table-column prop="qty" label="数量" width="120" />
        <el-table-column prop="amount" label="金额" width="140" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { loadPurchaseStats } from '@/api/pharmaStats'

const pad = (n: number) => String(n).padStart(2, '0')
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const monthStart = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01`
}

const range = ref<[string, string]>([monthStart(), today()])
const loading = ref(false)
const data = ref<any>(null)
const summary = computed(() => data.value?.summary)
const days = computed(() => (data.value?.days || []).filter((r: any) => Number(r.orderCount) > 0 || Number(r.amount) > 0))
const drugs = computed(() => data.value?.drugs || [])
const money = (n?: number) => Number(n || 0).toFixed(2)

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await loadPurchaseStats({ startDate: range.value[0], endDate: range.value[1] })
    data.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar-card { margin-bottom: 16px; }
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
.section-card { margin-bottom: 16px; }
</style>
