<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">仓库管理</h1>
      <p class="page-header-desc">维护地点仓名（如广深惠仓库、白云仓），并标注合格/待验/退货等类型</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="仓库名">
          <el-input v-model="searchParams.name" placeholder="如 广深惠仓库、白云仓" clearable />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="searchParams.code" placeholder="编码" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchParams.whType" clearable placeholder="类型" style="width: 140px">
            <el-option label="合格" value="合格" />
            <el-option label="待验" value="待验" />
            <el-option label="退货" value="退货" />
            <el-option label="不合格" value="不合格" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllWarehouse" :search-params="searchParams">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column prop="name" label="仓库名称" min-width="140" />
        <el-table-column prop="whType" label="类型" width="100" />
        <el-table-column prop="address" label="地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="managerId" label="负责人ID" width="100" />
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
            <el-icon><Plus /></el-icon> 添加仓库
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <CrudDialog ref="dialogRef" :submit-api="handleSubmitApi" :rules="rules" width="560px" @success="tableRef?.reload()">
      <template #default="{ formData }">
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="formData.name" placeholder="如 广深惠仓库、白云仓" />
        </el-form-item>
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="formData.code" placeholder="如 WH-OK" />
        </el-form-item>
        <el-form-item label="仓库类型">
          <el-select v-model="formData.whType" clearable style="width: 100%">
            <el-option label="合格" value="合格" />
            <el-option label="待验" value="待验" />
            <el-option label="退货" value="退货" />
            <el-option label="不合格" value="不合格" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input-number v-model="formData.managerId" :min="1" controls-position="right" style="width: 100%" />
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
import { loadAllWarehouse, addWarehouse, updateWarehouse, deleteWarehouse } from '@/api/warehouse'

const tableRef = ref()
const dialogRef = ref()
const isEdit = ref(false)

const searchParams = reactive({
  name: '',
  code: '',
  whType: ''
})

const rules = {
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.name = ''
  searchParams.code = ''
  searchParams.whType = ''
}
const handleAdd = () => {
  isEdit.value = false
  dialogRef.value?.open({ status: 1, whType: '合格' }, false)
}
const handleEdit = (row: any) => {
  isEdit.value = true
  dialogRef.value?.open(row, true)
}
const handleSubmitApi = (data: any) => (isEdit.value ? updateWarehouse(data) : addWarehouse(data))
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该仓库？', '提示', { type: 'warning' })
  await deleteWarehouse(row.id)
  tableRef.value?.reload()
}
</script>
