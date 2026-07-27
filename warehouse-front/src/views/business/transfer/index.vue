<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">库存调拨</h1>
      <p class="page-header-desc">仓库之间的库存调拨（草稿 → 发出 → 在途 → 收货完成）</p>
    </div>
    <el-card>
      <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
        <el-form-item label="调拨单号">
          <el-input v-model="searchParams.transferNo" placeholder="调拨单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="在途" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
      </SearchForm>

      <CrudTable ref="tableRef" :load-api="loadAllTransfer" :search-params="searchParams">
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table :data="row._items || []" border size="small" style="margin: 8px 16px">
              <el-table-column prop="goodsname" label="商品" />
              <el-table-column prop="size" label="规格" width="140" />
              <el-table-column prop="number" label="调拨数量" width="100" />
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="transferNo" label="调拨单号" width="200" />
        <el-table-column prop="fromWarehouseName" label="调出仓" width="120" />
        <el-table-column prop="toWarehouseName" label="调入仓" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewItems(row)">明细</el-button>
            <el-button v-if="row.status === 0" type="success" link @click="handleShip(row)">发出</el-button>
            <el-button v-if="row.status === 1" type="success" link @click="handleReceive(row)">收货</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" type="warning" link @click="handleCancel(row)">取消</el-button>
            <el-button v-if="row.status === 0 || row.status === 3" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新建调拨单
          </el-button>
        </template>
      </CrudTable>
    </el-card>

    <!-- 新建调拨单 -->
    <el-dialog v-model="createVisible" title="新建调拨单" width="780px" destroy-on-close>
      <el-form label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="调出仓" required>
              <el-select v-model="createForm.fromWarehouseId" placeholder="选择调出仓" style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入仓" required>
              <el-select v-model="createForm.toWarehouseId" placeholder="选择调入仓" style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.id" :label="w.name" :value="w.id"
                  :disabled="w.id === createForm.fromWarehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" placeholder="选填" />
        </el-form-item>

        <el-divider content-position="left">调拨明细</el-divider>
        <div v-for="(item, index) in createForm.items" :key="index" class="transfer-item-row">
          <el-select v-model="item.goodsid" placeholder="选择商品" filterable style="flex: 2"
            @change="(val: number) => handleGoodsChange(item, val)">
            <el-option v-for="g in goodsList" :key="g.id" :label="`${g.goodsname}（${g.size || '-'}）`" :value="g.id" />
          </el-select>
          <el-input-number v-model="item.number" :min="1" :max="999999" placeholder="数量" style="flex: 1" />
          <el-button type="danger" link @click="createForm.items.splice(index, 1)">移除</el-button>
        </div>
        <el-button type="primary" plain size="small" @click="createForm.items.push({ goodsid: null, number: 1 })">
          <el-icon><Plus /></el-icon> 添加商品
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleSubmitCreate">创建（草稿）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import {
  loadAllTransfer, loadTransferItems, createTransfer,
  shipTransfer, receiveTransfer, cancelTransfer, deleteTransfer
} from '@/api/transfer'
import { loadAllWarehouseForSelect } from '@/api/warehouse'
import { loadAllGoodsForSelect } from '@/api/goods'

const tableRef = ref()
const searchParams = reactive<{ transferNo: string; status: number | null }>({ transferNo: '', status: null })

const warehouseList = ref<any[]>([])
const goodsList = ref<any[]>([])

const statusText = (s: number) => ['草稿', '在途', '已完成', '已取消'][s] ?? '未知'
const statusTag = (s: number) => (['info', 'warning', 'success', 'danger'] as const)[s] ?? 'info'

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => { searchParams.transferNo = ''; searchParams.status = null }

const handleViewItems = async (row: any) => {
  if (!row._items) {
    const res: any = await loadTransferItems(row.id)
    row._items = res.data || []
  }
  // 展开/收起由 el-table expand 行处理；此处仅预载数据
  ElMessage.info('请点击行首箭头展开明细')
}

// ==================== 新建 ====================
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive<{ fromWarehouseId: number | null; toWarehouseId: number | null; remark: string; items: any[] }>({
  fromWarehouseId: null,
  toWarehouseId: null,
  remark: '',
  items: []
})

const handleCreate = () => {
  createForm.fromWarehouseId = null
  createForm.toWarehouseId = null
  createForm.remark = ''
  createForm.items = [{ goodsid: null, number: 1 }]
  createVisible.value = true
}

const handleGoodsChange = (item: any, _val: number) => {
  // 同一张单内不允许重复商品（数量合并到一行，避免明细歧义）
  const dup = createForm.items.filter(i => i.goodsid === item.goodsid)
  if (dup.length > 1) {
    ElMessage.warning('该商品已在明细中，请直接修改数量')
    item.goodsid = null
  }
}

const handleSubmitCreate = async () => {
  if (!createForm.fromWarehouseId || !createForm.toWarehouseId) {
    ElMessage.warning('请选择调出仓与调入仓')
    return
  }
  const items = createForm.items.filter(i => i.goodsid && i.number > 0)
  if (items.length === 0) {
    ElMessage.warning('请至少添加一条有效调拨明细')
    return
  }
  createLoading.value = true
  try {
    await createTransfer({ ...createForm, items })
    ElMessage.success('调拨单创建成功（草稿）')
    createVisible.value = false
    tableRef.value?.reload()
  } finally {
    createLoading.value = false
  }
}

// ==================== 状态操作 ====================
const handleShip = async (row: any) => {
  await ElMessageBox.confirm(`确认发出调拨单 ${row.transferNo}？发出后调出仓库存立即扣减。`, '发出确认', { type: 'warning' })
  await shipTransfer(row.id)
  ElMessage.success('已发出')
  tableRef.value?.reload()
}
const handleReceive = async (row: any) => {
  await ElMessageBox.confirm(`确认调拨单 ${row.transferNo} 已收货？收货后调入仓库存增加。`, '收货确认', { type: 'success' })
  await receiveTransfer(row.id)
  ElMessage.success('收货完成')
  tableRef.value?.reload()
}
const handleCancel = async (row: any) => {
  const tip = row.status === 1
    ? `调拨单 ${row.transferNo} 正在途，取消后调出仓库存将回补，确认取消？`
    : `确认取消调拨单 ${row.transferNo}？`
  await ElMessageBox.confirm(tip, '取消确认', { type: 'warning' })
  await cancelTransfer(row.id)
  ElMessage.success('已取消')
  tableRef.value?.reload()
}
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(`确认删除调拨单 ${row.transferNo}？`, '提示', { type: 'warning' })
  await deleteTransfer(row.id)
  ElMessage.success('已删除')
  tableRef.value?.reload()
}

onMounted(async () => {
  const wRes: any = await loadAllWarehouseForSelect()
  warehouseList.value = wRes.data || []
  const gRes: any = await loadAllGoodsForSelect()
  goodsList.value = gRes.data || []
})
</script>

<style scoped>
.transfer-item-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
</style>
