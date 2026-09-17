<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-header-title">出库打印包</h1>
      <p class="page-header-desc">按线路打印随货同行、签收单、路单与发票清单，贴合仓库派车干支线</p>
    </div>
    <el-card>
      <el-form inline @submit.prevent="load">
        <el-form-item label="发票号">
          <el-input v-model="invoiceNo" placeholder="20 位发票号" style="width: 280px" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="button" @click="load">生成打印包</el-button>
          <el-button native-type="button" :disabled="!pack" @click="doPrint">打印当前页</el-button>
        </el-form-item>
      </el-form>
      <div v-if="pack" class="print-sheet" id="print-sheet">
        <div class="brand">药企进销存 · {{ currentDoc }}</div>
        <h2>{{ pack.customerName }}</h2>
        <p class="meta">发票 {{ pack.invoiceNo }} · 出库 {{ pack.orderNo }} · 线路 {{ pack.route }} · {{ pack.warehouseName }}</p>
        <el-radio-group v-model="currentDoc" class="no-print" style="margin-bottom: 12px">
          <el-radio-button v-for="d in pack.docs" :key="d" :value="d">{{ d }}</el-radio-button>
        </el-radio-group>
        <el-table :data="pack.items" border size="small">
          <el-table-column prop="drugName" label="品名" />
          <el-table-column prop="spec" label="规格" width="120" />
          <el-table-column prop="batchNo" label="批号" width="130" />
          <el-table-column prop="expireDate" label="效期" width="120" />
          <el-table-column prop="qty" label="数量" width="80" />
          <el-table-column prop="amount" label="金额" width="100" />
        </el-table>
        <div class="sign-row">
          <span>保管员签字 ________</span>
          <span>送货员签字 ________</span>
          <span>客户签收 ________</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { loadPrintPack } from '@/api/ops'

const invoiceNo = ref('20260907000100000001')
const pack = ref<any>(null)
const currentDoc = ref('随货同行单')

const load = async () => {
  const res: any = await loadPrintPack(invoiceNo.value)
  if (res.code === -1) {
    ElMessage.error(res.msg || '未找到')
    return
  }
  pack.value = res.data
  currentDoc.value = (res.data?.docs || [])[0] || '随货同行单'
}

const doPrint = () => window.print()
</script>

<style scoped>
.print-sheet { max-width: 900px; }
.brand { color: var(--primary-color); font-weight: 700; letter-spacing: 0.08em; font-size: 12px; }
h2 { margin: 8px 0; }
.meta { color: var(--text-secondary); font-size: 13px; }
.sign-row { display: flex; justify-content: space-between; margin-top: 28px; font-size: 13px; }
@media print {
  .no-print, :deep(.el-form), :deep(.page-header) { display: none !important; }
}
</style>
