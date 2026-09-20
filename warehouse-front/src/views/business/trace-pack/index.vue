<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">大码解析</h1>
      <p class="page-header-desc">输入大码（也可输入中码/小码，自动上溯到箱码），查看这一批货、全部包装明细，以及入库、出库记录。</p>
    </div>

    <el-card>
      <el-form inline @submit.prevent="handleSearch">
        <el-form-item label="大码">
          <el-input
            v-model="code"
            placeholder="如 81000000000000000001"
            clearable
            style="width: 360px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="button" :loading="loading" @click="handleSearch">解析</el-button>
          <el-button @click="$router.push('/business/trace')">回单据查询</el-button>
        </el-form-item>
      </el-form>

      <div v-if="data" class="result">
        <el-descriptions :column="3" border class="mb16">
          <el-descriptions-item label="输入码">{{ data.inputCode }}</el-descriptions-item>
          <el-descriptions-item label="箱码/大码">{{ data.rootCode }}</el-descriptions-item>
          <el-descriptions-item label="层级">{{ data.rootLevel || '大包装' }}</el-descriptions-item>
          <el-descriptions-item label="品种">{{ data.drugName || '仅有包装关系，未挂入出库单据' }}</el-descriptions-item>
          <el-descriptions-item label="批号">{{ data.batchNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="规格 / 效期">{{ data.spec || '—' }} / {{ data.expireDate || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="pack-grid">
          <section class="pack-col pack-big">
            <header>大码 <em>{{ (data.bigCodes || []).length }}</em></header>
            <div v-for="c in data.bigCodes" :key="c" class="code-card">{{ c }}</div>
            <div v-if="!(data.bigCodes || []).length" class="empty">无</div>
          </section>
          <section class="pack-col pack-mid">
            <header>中码 <em>{{ (data.midCodes || []).length }}</em></header>
            <div v-for="c in data.midCodes" :key="c" class="code-card">{{ c }}</div>
            <div v-if="!(data.midCodes || []).length" class="empty">无</div>
          </section>
          <section class="pack-col pack-small">
            <header>小码 <em>{{ (data.smallCodes || []).length }}</em></header>
            <div v-for="c in data.smallCodes" :key="c" class="code-card">{{ c }}</div>
            <div v-if="!(data.smallCodes || []).length" class="empty">无</div>
          </section>
        </div>

        <h3 class="section-title">入库记录</h3>
        <el-table :data="data.inbounds || []" border class="mb16">
          <el-table-column prop="orderNo" label="入库单号" min-width="140" />
          <el-table-column prop="invoiceNo" label="供应商发票" min-width="150" />
          <el-table-column prop="partyName" label="供应商" min-width="140" />
          <el-table-column prop="drugName" label="品种" min-width="120" />
          <el-table-column prop="batchNo" label="批号" width="120" />
          <el-table-column prop="qty" label="数量" width="80" />
          <el-table-column prop="bizDate" label="日期" width="120" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column prop="spdid" label="SPDID" min-width="160" />
        </el-table>
        <el-empty v-if="!(data.inbounds || []).length" description="没有匹配到入库记录" />

        <h3 class="section-title">出库记录</h3>
        <el-table :data="data.outbounds || []" border>
          <el-table-column prop="orderNo" label="出库单号" min-width="140" />
          <el-table-column prop="invoiceNo" label="销售发票" min-width="150" />
          <el-table-column prop="partyName" label="客户" min-width="140" />
          <el-table-column prop="drugName" label="品种" min-width="120" />
          <el-table-column prop="batchNo" label="批号" width="120" />
          <el-table-column prop="qty" label="数量" width="80" />
          <el-table-column prop="bizDate" label="日期" width="120" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column prop="spdid" label="SPDID" min-width="160" />
        </el-table>
        <el-empty v-if="!(data.outbounds || []).length" description="没有匹配到出库记录" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { explainPack } from '@/api/trace'

const route = useRoute()
const code = ref('')
const loading = ref(false)
const data = ref<any>(null)

const handleSearch = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请输入大码')
    return
  }
  loading.value = true
  try {
    const res: any = await explainPack(code.value.trim())
    data.value = res.data
    if (!data.value) ElMessage.warning(res.msg || '没有解析结果')
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const q = route.query.code
  if (typeof q === 'string' && q.trim()) {
    code.value = q.trim()
    handleSearch()
  }
})
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.section-title { margin: 20px 0 10px; font-size: 15px; }
.pack-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 24px;
}
.pack-col { border-radius: 12px; padding: 12px; min-height: 160px; }
.pack-col header {
  font-weight: 700;
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
}
.pack-col header em { font-style: normal; font-size: 13px; opacity: 0.7; }
.pack-big { background: #eef4ff; }
.pack-mid { background: #f4f0ff; }
.pack-small { background: #f3faf4; }
.code-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  font-family: ui-monospace, Menlo, monospace;
  font-size: 13px;
  word-break: break-all;
}
.empty { color: #98a2b3; font-size: 13px; }
@media (max-width: 900px) {
  .pack-grid { grid-template-columns: 1fr; }
}
</style>
