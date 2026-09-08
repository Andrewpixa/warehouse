<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">客户管理</h1>
      <p class="page-header-desc">维护医院、药店、诊所等下游购进单位</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="客户名称">
          <el-input v-model="searchParams.name" placeholder="医院/药店/诊所名称" clearable />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="searchParams.contact" placeholder="联系人" clearable />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="searchParams.phone" placeholder="电话" clearable />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchParams.customerType" clearable placeholder="类别" style="width: 140px">
            <el-option label="医院" value="医院" />
            <el-option label="药店" value="药店" />
            <el-option label="诊所" value="诊所" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllCustomer" :search-params="searchParams">
        <el-table-column prop="id" label="客户编号" width="100" />
        <el-table-column prop="name" label="客户名称" min-width="140" />
        <el-table-column prop="customerType" label="类别" width="90" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="settleType" label="结算方式" width="100" />
        <el-table-column prop="address" label="地址" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <CrudDialog ref="dialogRef" :submit-api="handleSubmitApi" :rules="rules" width="560px" @success="tableRef?.reload()">
      <template #default="{ formData }">
        <el-form-item label="客户编号">
          <el-input :model-value="formData.id ? String(formData.id) : '保存后自动生成（1xxxxx）'" disabled />
        </el-form-item>
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="formData.customerType" clearable style="width: 100%">
            <el-option label="医院" value="医院" />
            <el-option label="药店" value="药店" />
            <el-option label="诊所" value="诊所" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="许可证号">
          <el-input v-model="formData.licenseNo" placeholder="药品经营/医疗机构执业许可" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="formData.contact" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item label="结算方式">
          <el-select v-model="formData.settleType" clearable style="width: 100%">
            <el-option label="现结" value="现结" />
            <el-option label="月结" value="月结" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </template>
    </CrudDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import { loadAllCustomer, addCustomer, updateCustomer, deleteCustomer } from '@/api/customer'

const tableRef = ref()
const dialogRef = ref()
const isEdit = ref(false)

const searchParams = reactive({
  name: '',
  contact: '',
  phone: '',
  customerType: ''
})

const rules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.name = ''
  searchParams.contact = ''
  searchParams.phone = ''
  searchParams.customerType = ''
}
const handleAdd = () => {
  isEdit.value = false
  dialogRef.value?.open({ status: 1 }, false)
}
const handleEdit = (row: any) => {
  isEdit.value = true
  dialogRef.value?.open(row, true)
}
const handleSubmitApi = (data: any) => (isEdit.value ? updateCustomer(data) : addCustomer(data))
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该客户？', '提示', { type: 'warning' })
  await deleteCustomer(row.id)
  tableRef.value?.reload()
}
</script>
