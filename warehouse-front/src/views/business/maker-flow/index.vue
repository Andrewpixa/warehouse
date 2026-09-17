<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-header-title">厂家流向查询</h1>
      <p class="page-header-desc">采供部维护品种集合并开通账号，厂家次日可查询销售流向；有误则联系 IT 修正</p>
    </div>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card header="已开通流向账号">
          <el-table :data="accounts" size="small">
            <el-table-column prop="loginName" label="账号" />
            <el-table-column prop="supplierName" label="厂家" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="厂家自助查询（演示：gzyy / 123456）">
          <el-form inline>
            <el-form-item label="账号"><el-input v-model="loginName" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="password" type="password" show-password /></el-form-item>
            <el-form-item><el-button type="primary" @click="query">查询流向</el-button></el-form-item>
          </el-form>
          <p v-if="result" class="hint">{{ result.supplierName }} · {{ result.count }} 条 · 流向次日可见</p>
          <el-table :data="result?.flows || []" size="small" max-height="420">
            <el-table-column prop="bizDate" label="日期" width="110" />
            <el-table-column prop="invoiceNo" label="发票号" min-width="160" />
            <el-table-column prop="customerName" label="客户" min-width="140" />
            <el-table-column prop="drugName" label="品种" />
            <el-table-column prop="batchNo" label="批号" width="120" />
            <el-table-column prop="qty" label="数量" width="80" />
            <el-table-column prop="flowStatus" label="发布" width="90" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { loadMakerAccounts, makerQuery } from '@/api/ops'

const accounts = ref<any[]>([])
const loginName = ref('gzyy')
const password = ref('123456')
const result = ref<any>(null)

onMounted(async () => {
  const res: any = await loadMakerAccounts()
  accounts.value = res.data || []
})

const query = async () => {
  const res: any = await makerQuery({ loginName: loginName.value, password: password.value })
  if (res.code === -1) {
    ElMessage.error(res.msg || '查询失败')
    return
  }
  result.value = res.data
}
</script>

<style scoped>
.hint { color: var(--text-secondary); font-size: 13px; margin: 0 0 12px; }
</style>
