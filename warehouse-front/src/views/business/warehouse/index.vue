<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">仓库管理</h1>
      <p class="page-header-desc">管理仓库与库位信息</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="仓库名">
          <el-input v-model="searchParams.name" placeholder="仓库名" clearable />
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllWarehouse" :search-params="searchParams">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="仓库名称">
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-left: 6px">默认仓</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="address" label="地址" show-overflow-tooltip />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.available === 1 ? 'success' : 'danger'" size="small">
              {{ row.available === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleLocations(row)">库位管理</el-button>
            <el-button v-if="row.isDefault !== 1" type="success" link @click="handleSetDefault(row)">设为默认</el-button>
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
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="formData.code" placeholder="如 WH002" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="formData.manager" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.available">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </template>
    </CrudDialog>

    <!-- 库位管理抽屉 -->
    <el-drawer v-model="locationDrawerVisible" :title="`库位管理 - ${currentWarehouse?.name || ''}`" size="640px">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleAddLocation">
          <el-icon><Plus /></el-icon> 添加库位
        </el-button>
      </div>
      <el-table :data="locationList" border v-loading="locationLoading">
        <el-table-column prop="code" label="库位编码" width="110" />
        <el-table-column prop="zone" label="库区" width="110" />
        <el-table-column prop="name" label="库位名称" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.available === 1 ? 'success' : 'danger'" size="small">
              {{ row.available === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEditLocation(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeleteLocation(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <CrudDialog ref="locationDialogRef" :submit-api="handleLocationSubmitApi" :rules="locationRules" width="500px"
      @success="reloadLocations">
      <template #default="{ formData }">
        <el-form-item label="库位编码" prop="code">
          <el-input v-model="formData.code" placeholder="如 A-01-03" />
        </el-form-item>
        <el-form-item label="库区">
          <el-select v-model="formData.zone" placeholder="选择库区" clearable style="width: 100%">
            <el-option label="存储区" value="存储区" />
            <el-option label="拣货区" value="拣货区" />
            <el-option label="退货区" value="退货区" />
            <el-option label="不良品区" value="不良品区" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位名称">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.available">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" />
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
import {
  loadAllWarehouse, addWarehouse, updateWarehouse, deleteWarehouse, setDefaultWarehouse,
  loadLocations, addLocation, updateLocation, deleteLocation
} from '@/api/warehouse'

const tableRef = ref()
const dialogRef = ref()
const isEdit = ref(false)

const searchParams = reactive({ name: '' })

const rules = {
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }]
}

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => { searchParams.name = '' }
const handleAdd = () => { isEdit.value = false; dialogRef.value?.open({ available: 1, isDefault: 0 }, false) }
const handleEdit = (row: any) => { isEdit.value = true; dialogRef.value?.open(row, true) }
const handleSubmitApi = (data: any) => isEdit.value ? updateWarehouse(data) : addWarehouse(data)
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该仓库？尚有库存的仓库无法删除。', '提示', { type: 'warning' })
  await deleteWarehouse(row.id)
  tableRef.value?.reload()
}
const handleSetDefault = async (row: any) => {
  await ElMessageBox.confirm(`确认将【${row.name}】设为默认仓库？`, '提示', { type: 'warning' })
  await setDefaultWarehouse(row.id)
  tableRef.value?.reload()
}

// ==================== 库位管理 ====================
const locationDrawerVisible = ref(false)
const locationLoading = ref(false)
const locationList = ref<any[]>([])
const currentWarehouse = ref<any>(null)
const locationDialogRef = ref()
const isLocationEdit = ref(false)

const locationRules = {
  code: [{ required: true, message: '请输入库位编码', trigger: 'blur' }]
}

const handleLocations = async (row: any) => {
  currentWarehouse.value = row
  locationDrawerVisible.value = true
  await reloadLocations()
}

const reloadLocations = async () => {
  if (!currentWarehouse.value) return
  locationLoading.value = true
  try {
    const res: any = await loadLocations(currentWarehouse.value.id)
    locationList.value = res.data || []
  } finally {
    locationLoading.value = false
  }
}

const handleAddLocation = () => {
  isLocationEdit.value = false
  locationDialogRef.value?.open({ warehouseId: currentWarehouse.value.id, available: 1 }, false)
}
const handleEditLocation = (row: any) => {
  isLocationEdit.value = true
  locationDialogRef.value?.open(row, true)
}
const handleLocationSubmitApi = (data: any) => {
  data.warehouseId = currentWarehouse.value.id
  return isLocationEdit.value ? updateLocation(data) : addLocation(data)
}
const handleDeleteLocation = async (row: any) => {
  await ElMessageBox.confirm('确认删除该库位？', '提示', { type: 'warning' })
  await deleteLocation(row.id)
  reloadLocations()
}
</script>
