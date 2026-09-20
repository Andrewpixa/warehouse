<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">单位欠款</h1>
      <p class="page-header-desc">按客户汇总已确认出库发票的票面、已回与欠款。催款用此表，勾兑请按发票号进冲账。</p>
    </div>
    <el-card>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column prop="customerName" label="客户" min-width="180" />
        <el-table-column prop="invoiceCount" label="发票张数" width="110" />
        <el-table-column prop="totalAmount" label="票面合计" width="120" />
        <el-table-column prop="paidAmount" label="已回" width="120" />
        <el-table-column prop="unpaidAmount" label="欠款" width="120" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push({ path: '/business/outbound', query: { customerId: row.customerId, paidStatus: '未回款' } })">未回票</el-button>
            <el-button type="primary" link @click="$router.push('/business/offset')">去冲账</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { loadCustomerDebt } from '@/api/outbound'

const rows = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await loadCustomerDebt()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>
