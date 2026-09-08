<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">品种管理</h1>
      <p class="page-header-desc">药品编号 3xxxxx，器械编号 4xxxxx</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="通用名">
          <el-input v-model="searchParams.genericName" placeholder="通用名" clearable />
        </el-form-item>
        <el-form-item label="品种编号">
          <el-input v-model="searchParams.code" placeholder="3xxxxx / 4xxxxx" clearable />
        </el-form-item>
        <el-form-item label="品种类型">
          <el-select v-model="searchParams.category" clearable placeholder="类型" style="width: 140px">
            <el-option label="药品" value="药品" />
            <el-option label="器械" value="器械" />
          </el-select>
        </el-form-item>
        <el-form-item label="批准文号">
          <el-input v-model="searchParams.approvalNo" placeholder="批准文号" clearable />
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllDrug" :search-params="searchParams">
        <el-table-column prop="id" label="品种编号" width="100" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.category === '器械' ? 'warning' : 'success'" size="small">
              {{ row.category === '器械' ? '器械' : '药品' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="genericName" label="通用名" min-width="120" />
        <el-table-column prop="tradeName" label="商品名" min-width="100" />
        <el-table-column prop="spec" label="规格" width="110" />
        <el-table-column prop="dosageForm" label="剂型" width="90" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="manufacturer" label="生产企业" min-width="120" show-overflow-tooltip />
        <el-table-column prop="approvalNo" label="批准文号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="rxType" label="处方分类" width="90" />
        <el-table-column label="冷链" width="70">
          <template #default="{ row }">
            <el-tag :type="row.isColdChain === 1 ? 'warning' : 'info'" size="small">
              {{ row.isColdChain === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
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
            <el-icon><Plus /></el-icon> 添加品种
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <CrudDialog ref="dialogRef" :submit-api="handleSubmitApi" :rules="rules" width="640px" @success="tableRef?.reload()">
      <template #default="{ formData }">
        <el-form-item label="品种类型" prop="category">
          <el-select v-model="formData.category" :disabled="!!formData.id" style="width: 100%">
            <el-option label="药品（3xxxxx）" value="药品" />
            <el-option label="器械（4xxxxx）" value="器械" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种编号">
          <el-input :model-value="formData.id ? String(formData.id) : '保存后按类型自动生成'" disabled />
        </el-form-item>
        <el-form-item label="通用名" prop="genericName">
          <el-input v-model="formData.genericName" />
        </el-form-item>
        <el-form-item label="商品名">
          <el-input v-model="formData.tradeName" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="formData.spec" placeholder="如 0.25g*24粒" />
        </el-form-item>
        <el-form-item label="剂型">
          <el-input v-model="formData.dosageForm" placeholder="片剂/胶囊/注射液" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="formData.unit" placeholder="盒/瓶/支" />
        </el-form-item>
        <el-form-item label="生产企业">
          <el-input v-model="formData.manufacturer" />
        </el-form-item>
        <el-form-item label="批准文号">
          <el-input v-model="formData.approvalNo" />
        </el-form-item>
        <el-form-item label="处方分类">
          <el-select v-model="formData.rxType" clearable style="width: 100%">
            <el-option label="处方药" value="处方药" />
            <el-option label="OTC" value="OTC" />
          </el-select>
        </el-form-item>
        <el-form-item label="冷链">
          <el-radio-group v-model="formData.isColdChain">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
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
import { loadAllDrug, addDrug, updateDrug, deleteDrug } from '@/api/drug'

const tableRef = ref()
const dialogRef = ref()
const isEdit = ref(false)

const searchParams = reactive({
  genericName: '',
  code: '',
  category: '',
  approvalNo: ''
})

const rules = {
  category: [{ required: true, message: '请选择品种类型', trigger: 'change' }],
  genericName: [{ required: true, message: '请输入通用名', trigger: 'blur' }]
}

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.genericName = ''
  searchParams.code = ''
  searchParams.category = ''
  searchParams.approvalNo = ''
}
const handleAdd = () => {
  isEdit.value = false
  dialogRef.value?.open({ status: 1, isColdChain: 0, category: '药品' }, false)
}
const handleEdit = (row: any) => {
  isEdit.value = true
  dialogRef.value?.open(row, true)
}
const handleSubmitApi = (data: any) => (isEdit.value ? updateDrug(data) : addDrug(data))
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该品种？', '提示', { type: 'warning' })
  await deleteDrug(row.id)
  tableRef.value?.reload()
}
</script>
