<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">银行到账</h1>
      <p class="page-header-desc">财务按银行真实到账登记。公对公与支票分开记。认领后才能改发票回款状态。</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="流水号">
          <el-input v-model="searchParams.receiptNo" clearable />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="searchParams.customerId" filterable clearable style="width: 220px">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="searchParams.channel" clearable style="width: 120px">
            <el-option label="公对公" value="公对公" />
            <el-option label="支票" value="支票" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" clearable style="width: 130px">
            <el-option label="未认领" value="未认领" />
            <el-option label="部分认领" value="部分认领" />
            <el-option label="已认领" value="已认领" />
            <el-option label="挂账" value="挂账" />
          </el-select>
        </el-form-item>
      </SearchForm>
      <CrudTable ref="tableRef" :load-api="loadAllBankReceipt" :search-params="searchParams">
        <el-table-column prop="receiptNo" label="流水号" min-width="140" />
        <el-table-column prop="customerName" label="付款单位" min-width="160" />
        <el-table-column prop="payerName" label="银行户名" min-width="140" />
        <el-table-column prop="channel" label="渠道" width="90" />
        <el-table-column prop="voucherNo" label="回单/支票号" min-width="140" />
        <el-table-column prop="receivedDate" label="到账日" width="120" />
        <el-table-column prop="amount" label="到账金额" width="110" />
        <el-table-column prop="allocatedAmount" label="已认领" width="100" />
        <el-table-column prop="remainAmount" label="未认领" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">改登记</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="openAdd">登记到账</el-button>
        </template>
      </CrudTable>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '改到账登记' : '登记银行到账'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="客户" required>
          <el-select v-model="form.customerId" filterable style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.id} ${c.name}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="到账日" required>
          <el-date-picker v-model="form.receivedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="渠道" required>
          <el-select v-model="form.channel" style="width: 100%">
            <el-option label="公对公" value="公对公" />
            <el-option label="支票" value="支票" />
          </el-select>
        </el-form-item>
        <el-form-item label="回单/支票号">
          <el-input v-model="form.voucherNo" />
        </el-form-item>
        <el-form-item label="银行户名">
          <el-input v-model="form.payerName" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import { loadAllBankReceipt, saveBankReceipt } from '@/api/bankReceipt'
import { loadAllCustomer } from '@/api/customer'

const searchParams = reactive<any>({ page: 1, limit: 10, receiptNo: '', customerId: undefined, channel: '', status: '' })
const tableRef = ref<any>()
const customers = ref<any[]>([])
const visible = ref(false)
const saving = ref(false)
const form = reactive<any>({})

onMounted(async () => {
  const c: any = await loadAllCustomer({ page: 1, limit: 200 })
  customers.value = c.data || []
})

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.receiptNo = ''
  searchParams.customerId = undefined
  searchParams.channel = ''
  searchParams.status = ''
  tableRef.value?.reload()
}

const blank = () => ({
  id: undefined,
  customerId: undefined,
  receivedDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  channel: '公对公',
  voucherNo: '',
  payerName: '',
  remark: ''
})

const openAdd = () => {
  Object.assign(form, blank())
  visible.value = true
}

const openEdit = (row: any) => {
  Object.assign(form, blank(), row)
  visible.value = true
}

const handleSave = async () => {
  if (!form.customerId || !form.amount) {
    ElMessage.warning('请填写客户和金额')
    return
  }
  saving.value = true
  try {
    const res: any = await saveBankReceipt({ ...form })
    if (res.code === 200 || res.data) {
      ElMessage.success(res.msg || '已登记')
      visible.value = false
      tableRef.value?.reload()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } finally {
    saving.value = false
  }
}
</script>
