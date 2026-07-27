<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">分仓库存</h1>
      <p class="page-header-desc">查看各仓库库存分布，配置分仓预警规则</p>
    </div>
    <el-card>
      <el-tabs v-model="activeTab">
        <!-- ==================== 分仓库存 ==================== -->
        <el-tab-pane label="分仓库存" name="stock">
          <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
            <el-form-item label="仓库">
              <el-select v-model="searchParams.warehouseId" placeholder="全部仓库" clearable style="width: 160px">
                <el-option v-for="w in warehouseList" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品名">
              <el-input v-model="searchParams.goodsname" placeholder="商品名" clearable />
            </el-form-item>
          </SearchForm>

          <CrudTable ref="tableRef" :load-api="loadGoodsStock" :search-params="searchParams">
            <el-table-column prop="goodsname" label="商品" />
            <el-table-column prop="size" label="规格" width="160" />
            <el-table-column prop="warehouseName" label="仓库" width="140" />
            <el-table-column prop="number" label="库存数量" width="120">
              <template #default="{ row }">
                <span :style="{ color: row.number <= 0 ? '#f56c6c' : 'inherit', fontWeight: 600 }">{{ row.number }}</span>
              </template>
            </el-table-column>
          </CrudTable>
        </el-tab-pane>

        <!-- ==================== 预警规则 ==================== -->
        <el-tab-pane label="分仓预警规则" name="warn">
          <div style="margin-bottom: 12px; display: flex; gap: 8px; align-items: center">
            <el-button type="primary" @click="handleAddRule">
              <el-icon><Plus /></el-icon> 添加规则
            </el-button>
            <el-button @click="reloadWarnings">查看当前预警</el-button>
            <span class="warn-tip">未配置规则的商品仍按商品管理中的总库存阈值预警</span>
          </div>
          <el-table :data="ruleList" border v-loading="ruleLoading">
            <el-table-column prop="goodsid" label="商品">
              <template #default="{ row }">{{ goodsName(row.goodsid) }}</template>
            </el-table-column>
            <el-table-column prop="warehouseId" label="仓库" width="140">
              <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
            </el-table-column>
            <el-table-column prop="dangernum" label="预警阈值" width="120" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleEditRule(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDeleteRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 规则编辑对话框 -->
    <CrudDialog ref="ruleDialogRef" :submit-api="handleRuleSubmitApi" :rules="ruleFormRules" width="480px"
      @success="reloadRules">
      <template #default="{ formData }">
        <el-form-item label="商品" prop="goodsid">
          <el-select v-model="formData.goodsid" filterable placeholder="选择商品" style="width: 100%"
            :disabled="isRuleEdit">
            <el-option v-for="g in goodsList" :key="g.id" :label="g.goodsname" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="formData.warehouseId" placeholder="选择仓库" style="width: 100%" :disabled="isRuleEdit">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警阈值" prop="dangernum">
          <el-input-number v-model="formData.dangernum" :min="0" :max="999999" style="width: 100%" />
        </el-form-item>
      </template>
    </CrudDialog>

    <!-- 当前分仓预警 -->
    <el-dialog v-model="warningVisible" title="当前分仓库存预警" width="640px">
      <el-table :data="warningList" border>
        <el-table-column prop="goodsname" label="商品" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="number" label="当前库存" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600">{{ row.number }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dangernum" label="预警阈值" width="100" />
      </el-table>
      <el-empty v-if="warningList.length === 0" description="暂无分仓预警" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import {
  loadGoodsStock, loadAllWarehouseForSelect,
  loadWarnRules, saveWarnRule, deleteWarnRule, loadWarehouseWarnings
} from '@/api/warehouse'
import { loadAllGoodsForSelect } from '@/api/goods'

const activeTab = ref('stock')
const tableRef = ref()

const warehouseList = ref<any[]>([])
const goodsList = ref<any[]>([])

const searchParams = reactive<{ warehouseId: number | null; goodsname: string }>({ warehouseId: null, goodsname: '' })

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => { searchParams.warehouseId = null; searchParams.goodsname = '' }

const goodsName = (id: number) => goodsList.value.find(g => g.id === id)?.goodsname || id
const warehouseName = (id: number) => warehouseList.value.find(w => w.id === id)?.name || id

// ==================== 预警规则 ====================
const ruleList = ref<any[]>([])
const ruleLoading = ref(false)
const ruleDialogRef = ref()
const isRuleEdit = ref(false)

const ruleFormRules = {
  goodsid: [{ required: true, message: '请选择商品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  dangernum: [{ required: true, message: '请输入预警阈值', trigger: 'blur' }]
}

const reloadRules = async () => {
  ruleLoading.value = true
  try {
    const res: any = await loadWarnRules()
    ruleList.value = res.data || []
  } finally {
    ruleLoading.value = false
  }
}

const handleAddRule = () => { isRuleEdit.value = false; ruleDialogRef.value?.open({ dangernum: 10 }, false) }
const handleEditRule = (row: any) => { isRuleEdit.value = true; ruleDialogRef.value?.open(row, true) }
const handleRuleSubmitApi = (data: any) => saveWarnRule(data)
const handleDeleteRule = async (row: any) => {
  await ElMessageBox.confirm('确认删除该预警规则？', '提示', { type: 'warning' })
  await deleteWarnRule(row.id)
  reloadRules()
}

// ==================== 当前预警 ====================
const warningVisible = ref(false)
const warningList = ref<any[]>([])

const reloadWarnings = async () => {
  const res: any = await loadWarehouseWarnings()
  warningList.value = res.data || []
  warningVisible.value = true
}

onMounted(async () => {
  const wRes: any = await loadAllWarehouseForSelect()
  warehouseList.value = wRes.data || []
  const gRes: any = await loadAllGoodsForSelect()
  goodsList.value = gRes.data || []
  reloadRules()
})
</script>

<style scoped>
.warn-tip {
  color: #909399;
  font-size: 13px;
}
</style>
