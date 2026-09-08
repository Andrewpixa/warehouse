<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">追溯码查询</h1>
      <p class="page-header-desc">发票号 → 出库明细 SPDID → 追溯码；仓管可将大包装解析为中包装、最小包装</p>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="发票号">
          <el-input v-model="invoiceNo" placeholder="20位发票号，如 20260907000100000001" clearable style="width: 320px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="order" :column="3" border class="mb16">
        <el-descriptions-item label="出库单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="发票号">{{ order.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ order.customerName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ order.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ order.bizDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
      </el-descriptions>

      <el-table v-if="order" :data="order.items || []" border>
        <el-table-column prop="drugName" label="药品" min-width="140" />
        <el-table-column prop="batchNo" label="批号" width="130" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="spdid" label="SPDID" min-width="180" />
        <el-table-column label="已采码" width="90">
          <template #default="{ row }">
            {{ countBySpdid(row.spdid) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canCollect" type="primary" link @click="openCollect(row)">采集追溯码</el-button>
            <el-button v-if="canParse" type="success" link @click="openParse(row)">解析包装</el-button>
          </template>
        </el-table-column>
      </el-table>

      <h3 v-if="traces.length" class="section-title">追溯码明细</h3>
      <el-table
        v-if="traces.length"
        :data="traceTree"
        row-key="code"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="spdid" label="SPDID" min-width="170" />
        <el-table-column prop="drugName" label="药品" min-width="120" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column prop="code" label="追溯码" min-width="200" />
        <el-table-column prop="packLevel" label="包装层级" width="110" />
        <el-table-column prop="parentCode" label="上级码" min-width="180" />
        <el-table-column prop="bizType" label="业务" width="80" />
        <el-table-column prop="status" label="状态" width="80" />
      </el-table>
    </el-card>

    <el-dialog v-model="collectVisible" title="采集追溯码" width="520px">
      <p class="hint">SPDID：{{ currentSpdid }}</p>
      <el-form label-width="90px">
        <el-form-item label="包装层级">
          <el-select v-model="packLevel" style="width: 100%">
            <el-option label="最小包装" value="最小包装" />
            <el-option label="中包装" value="中包装" />
            <el-option label="大包装" value="大包装" />
          </el-select>
        </el-form-item>
        <el-form-item label="追溯码">
          <el-input v-model="codeText" type="textarea" :rows="6" placeholder="每行一条追溯码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCollect">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseVisible" title="解析包装码" width="640px">
      <p class="hint">SPDID：{{ currentSpdid }}。演示大码 <code>81000000000000000001</code>（1 箱 = 2 中包 = 6 小盒），也可输入中包装码只拆小盒。</p>
      <el-form label-width="90px">
        <el-form-item label="包装码">
          <el-input v-model="parseCode" placeholder="大包装或中包装码" clearable @keyup.enter="handlePreview" />
        </el-form-item>
      </el-form>
      <el-table v-if="previewRows.length" :data="previewRows" border size="small" max-height="280">
        <el-table-column prop="packLevel" label="层级" width="110" />
        <el-table-column prop="code" label="追溯码" min-width="200" />
        <el-table-column prop="parentCode" label="上级码" min-width="180" />
      </el-table>
      <template #footer>
        <el-button @click="parseVisible = false">取消</el-button>
        <el-button @click="handlePreview" :loading="previewing">预览解析</el-button>
        <el-button type="primary" :loading="saving" :disabled="!previewRows.length" @click="handleParse">写入本明细</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addTraceCodes, loadTraceInvoice, parseTraceCodes, previewParse } from '@/api/trace'
import { usePermission } from '@/composables/usePermission'

interface TraceRow {
  code: string
  parentCode?: string
  packLevel?: string
  spdid?: string
  drugName?: string
  batchNo?: string
  bizType?: string
  status?: string
  children?: TraceRow[]
}

const route = useRoute()
const { hasPermission } = usePermission()
const canCollect = computed(() => hasPermission('trace:collect'))
const canParse = computed(() => hasPermission('trace:parse'))

const invoiceNo = ref('')
const order = ref<any>(null)
const traces = ref<TraceRow[]>([])
const collectVisible = ref(false)
const parseVisible = ref(false)
const currentSpdid = ref('')
const packLevel = ref('最小包装')
const codeText = ref('')
const parseCode = ref('')
const previewRows = ref<TraceRow[]>([])
const saving = ref(false)
const previewing = ref(false)

const countBySpdid = (spdid: string) => traces.value.filter((t) => t.spdid === spdid).length

const traceTree = computed(() => buildTraceTree(traces.value))

const buildTraceTree = (list: TraceRow[]): TraceRow[] => {
  const map = new Map<string, TraceRow>()
  list.forEach((t) => map.set(t.code, { ...t, children: [] }))
  const roots: TraceRow[] = []
  list.forEach((t) => {
    const node = map.get(t.code)!
    if (t.parentCode && map.has(t.parentCode)) {
      map.get(t.parentCode)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })
  const prune = (nodes: TraceRow[]): TraceRow[] =>
    nodes.map((n) => {
      const children = prune(n.children || [])
      if (!children.length) {
        return { ...n, children: undefined }
      }
      return { ...n, children }
    })
  return prune(roots)
}

const handleSearch = async () => {
  if (!invoiceNo.value.trim()) {
    ElMessage.warning('请输入发票号')
    return
  }
  const res: any = await loadTraceInvoice(invoiceNo.value.trim())
  order.value = res.data
  traces.value = res.traces || []
}

const openCollect = (row: any) => {
  if (!row.spdid) {
    ElMessage.warning('该明细还没有 SPDID，请先保存出库单')
    return
  }
  currentSpdid.value = row.spdid
  packLevel.value = '最小包装'
  codeText.value = ''
  collectVisible.value = true
}

const openParse = (row: any) => {
  if (!row.spdid) {
    ElMessage.warning('该明细还没有 SPDID，请先保存出库单')
    return
  }
  currentSpdid.value = row.spdid
  parseCode.value = '81000000000000000001'
  previewRows.value = []
  parseVisible.value = true
}

onMounted(async () => {
  const q = route.query.invoiceNo
  if (typeof q === 'string' && q.trim()) {
    invoiceNo.value = q.trim()
    await handleSearch()
  }
})

const handleCollect = async () => {
  const codes = codeText.value.split(/\r?\n/).map((s) => s.trim()).filter(Boolean)
  if (!codes.length) {
    ElMessage.warning('请录入追溯码')
    return
  }
  saving.value = true
  try {
    await addTraceCodes({
      spdid: currentSpdid.value,
      codes,
      packLevel: packLevel.value,
      bizType: '出库'
    })
    ElMessage.success('采集成功')
    collectVisible.value = false
    await handleSearch()
  } finally {
    saving.value = false
  }
}

const handlePreview = async () => {
  if (!parseCode.value.trim()) {
    ElMessage.warning('请输入大包装或中包装码')
    return
  }
  previewing.value = true
  try {
    const res: any = await previewParse(parseCode.value.trim())
    previewRows.value = res.data || []
    if (!previewRows.value.length) {
      ElMessage.warning(res.msg || '没有解析出下级码')
    }
  } catch {
    previewRows.value = []
  } finally {
    previewing.value = false
  }
}

const handleParse = async () => {
  if (!parseCode.value.trim()) {
    ElMessage.warning('请输入大包装或中包装码')
    return
  }
  saving.value = true
  try {
    await parseTraceCodes({
      spdid: currentSpdid.value,
      code: parseCode.value.trim(),
      bizType: '出库'
    })
    ElMessage.success('解析写入成功')
    parseVisible.value = false
    await handleSearch()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.section-title {
  margin: 20px 0 10px;
  font-size: 15px;
}
.hint {
  color: #666;
  margin-bottom: 12px;
}
.hint code {
  font-size: 13px;
  color: #1d4ed8;
}
</style>
