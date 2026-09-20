<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">追溯码查询</h1>
      <p class="page-header-desc">查发票即可看到物流已匹配的追溯码和状态。业务员只在错码/异常时扫码或用 01 码代替；缺码由客户描述，不现场采集。</p>
    </div>

    <el-card>
      <el-form inline @submit.prevent="handleSearch">
        <el-form-item label="单据">
          <el-input
            v-model="keyword"
            placeholder="供应商发票 / 销售发票 / 采购入库单号 / 出库单号"
            clearable
            style="width: 440px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="button" :loading="searching" @click="handleSearch">查询明细</el-button>
          <el-button @click="$router.push('/business/trace-pack')">去解析大码</el-button>
        </el-form-item>
      </el-form>

      <div v-if="recent.length && !purchase && !outbound" class="recent">
        <div class="recent-label">最近单据：</div>
        <el-button v-for="row in recent" :key="row.key" size="small" @click="pickRecent(row)">{{ row.label }}</el-button>
      </div>

      <el-table v-if="candidates.length" :data="candidates" border class="mb16" @row-click="pickCandidate">
        <el-table-column prop="billType" label="业务" width="80" />
        <el-table-column prop="orderNo" label="单号" min-width="150" />
        <el-table-column prop="invoiceNo" label="发票号" min-width="160" />
        <el-table-column prop="partyName" label="供应商/客户" min-width="140" />
        <el-table-column prop="drugName" label="药品" min-width="120" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column label="" width="90">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="pickCandidate(row)">打开</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-descriptions v-if="purchase" :column="3" border class="mb16">
        <el-descriptions-item label="采购入库单">{{ purchase.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商发票">{{ purchase.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ purchase.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ purchase.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ purchase.bizDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ purchase.status }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions v-if="outbound" :column="3" border class="mb16">
        <el-descriptions-item label="出库单号">{{ outbound.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="销售发票">{{ outbound.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ outbound.customerName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ outbound.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ outbound.bizDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ outbound.status }}</el-descriptions-item>
      </el-descriptions>

      <el-table
        v-if="lineItems.length"
        :data="lineItems"
        border
        highlight-current-row
        @row-click="openLine"
      >
        <el-table-column prop="drugName" label="药品" min-width="160" />
        <el-table-column prop="batchNo" label="批号" width="130" />
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column prop="spdid" label="流水号 SPDID" min-width="180" />
        <el-table-column label="匹配码" width="90">
          <template #default="{ row }">{{ countBySpdid(row.spdid) }}</template>
        </el-table-column>
        <el-table-column label="" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openLine(row)">查看追溯码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" size="920px" :title="drawerTitle" destroy-on-close>
      <div class="drawer-toolbar">
        <el-button v-if="canFix" type="primary" :disabled="!selectedTrace" @click="openFixScan">扫码纠错</el-button>
        <el-button v-if="canFix" :disabled="!selectedTrace" @click="handleUse01">用 01 码代替</el-button>
        <el-button v-if="canFix" @click="openMissing">登记缺码</el-button>
        <el-button v-if="canParse" @click="openParse">解析包装写入本行</el-button>
        <el-button @click="$router.push({ path: '/business/trace-pack', query: { code: firstBig } })" :disabled="!firstBig">用大码看履历</el-button>
      </div>
      <p class="hint">点选一条码再纠错。正常票的码来自物流匹配，不必采集。</p>
      <div class="pack-grid">
        <section class="pack-col pack-big">
          <header>大码 <em>{{ packs.big.length }}</em></header>
          <div v-if="!packs.big.length" class="empty">本行暂无大码（物流未传大包装）</div>
          <button v-for="c in packs.big" :key="c.code" class="code-card" type="button" :class="{ on: selectedTrace?.id === c.id }" @click="pickTrace(c)">
            {{ c.code }}
            <small>{{ c.codeKind || c.status }}</small>
          </button>
        </section>
        <section class="pack-col pack-mid">
          <header>中码 <em>{{ packs.mid.length }}</em></header>
          <div v-if="!packs.mid.length" class="empty">本行暂无中码</div>
          <div v-for="c in packs.mid" :key="c.code" class="code-card" :class="{ on: selectedTrace?.id === c.id }" @click="pickTrace(c)">
            <span>{{ c.code }}</span>
            <small>{{ c.codeKind || c.status }}<template v-if="c.parentCode"> · 上级 {{ c.parentCode }}</template></small>
          </div>
        </section>
        <section class="pack-col pack-small">
          <header>小码 <em>{{ packs.small.length }}</em></header>
          <div v-if="!packs.small.length" class="empty">本行暂无小码</div>
          <div v-for="c in packs.small" :key="c.code" class="code-card" :class="{ on: selectedTrace?.id === c.id }" @click="pickTrace(c)">
            <span>{{ c.code }}</span>
            <small>{{ c.codeKind || c.status }}<template v-if="c.parentCode"> · 上级 {{ c.parentCode }}</template></small>
          </div>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="fixVisible" title="扫码纠正异常码" width="560px" @opened="focusScanInput">
      <p class="hint">仅处理当前选中码：{{ selectedTrace?.code }}</p>
      <el-input ref="scanInputRef" v-model="scanInput" placeholder="扫描或输入正确追溯码，回车确认" clearable @keyup.enter="handleFixScan" />
      <template #footer>
        <el-button @click="fixVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!scanInput.trim()" @click="handleFixScan">替换</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="missingVisible" title="登记缺码（客户描述）" width="560px">
      <p class="hint">缺码不由业务员扫码补数，填写客户说明即可。</p>
      <el-input v-model="customerNote" type="textarea" rows="4" placeholder="例如：医院收货少 2 盒最小包装，票面数量与实物不符" />
      <template #footer>
        <el-button @click="missingVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!customerNote.trim()" @click="handleMissing">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseVisible" title="解析包装写入本行" width="640px">
      <p class="hint">演示大码 <code>81000000000000000001</code></p>
      <el-form label-width="90px">
        <el-form-item label="包装码">
          <el-input v-model="parseCode" placeholder="大码或中码" clearable @keyup.enter="handlePreview" />
        </el-form-item>
      </el-form>
      <el-table v-if="previewRows.length" :data="previewRows" border size="small" max-height="280">
        <el-table-column prop="packLevel" label="层级" width="110" />
        <el-table-column prop="code" label="追溯码" min-width="200" />
        <el-table-column prop="parentCode" label="上级码" min-width="180" />
      </el-table>
      <template #footer>
        <el-button @click="parseVisible = false">取消</el-button>
        <el-button @click="handlePreview" :loading="previewing">预览</el-button>
        <el-button type="primary" :loading="saving" :disabled="!previewRows.length" @click="handleParse">写入本明细</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { lookupTrace, loadTraceBySpdid, parseTraceCodes, previewParse, replaceAbnormalTrace, reportMissingTrace } from '@/api/trace'
import { loadAllOutbound } from '@/api/outbound'
import { loadAllPurchase } from '@/api/purchase'
import { usePermission } from '@/composables/usePermission'

interface TraceRow {
  id?: number
  code: string
  parentCode?: string
  packLevel?: string
  spdid?: string
  status?: string
  codeKind?: string
  remark?: string
}

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const canFix = computed(() => hasPermission('sales:view') || hasPermission('trace:collect'))
const canParse = computed(() => hasPermission('trace:parse'))

const keyword = ref('')
const searching = ref(false)
const purchase = ref<any>(null)
const outbound = ref<any>(null)
const traces = ref<TraceRow[]>([])
const lineTraces = ref<TraceRow[]>([])
const candidates = ref<any[]>([])
const recent = ref<any[]>([])
const drawer = ref(false)
const currentSpdid = ref('')
const currentLine = ref<any>(null)
const selectedTrace = ref<TraceRow | null>(null)
const fixVisible = ref(false)
const missingVisible = ref(false)
const customerNote = ref('')
const parseVisible = ref(false)
const scanInput = ref('')
const scanInputRef = ref()
const parseCode = ref('')
const previewRows = ref<TraceRow[]>([])
const saving = ref(false)
const previewing = ref(false)

const collectBizType = computed(() => (purchase.value ? '入库' : '出库'))
const drawerTitle = computed(() => {
  const line = currentLine.value
  if (!line) return '追溯码'
  return `${line.drugName || '明细'} · 批号 ${line.batchNo || '—'} · ${line.spdid || ''}`
})

const lineItems = computed(() => {
  if (purchase.value?.items) {
    return purchase.value.items.map((it: any) => ({
      ...it,
      qty: it.stockInQty ?? it.receiveQty,
      spdid: it.spdid
    }))
  }
  if (outbound.value?.items) {
    return outbound.value.items
  }
  return []
})

const countBySpdid = (spdid: string) => traces.value.filter((t) => t.spdid === spdid).length

const packKind = (level?: string) => {
  if (level && level.includes('大')) return 'big'
  if (level && level.includes('中')) return 'mid'
  return 'small'
}

const packs = computed(() => {
  const big: TraceRow[] = []
  const mid: TraceRow[] = []
  const small: TraceRow[] = []
  lineTraces.value.forEach((t) => {
    const k = packKind(t.packLevel)
    if (k === 'big') big.push(t)
    else if (k === 'mid') mid.push(t)
    else small.push(t)
  })
  return { big, mid, small }
})

const firstBig = computed(() => packs.value.big[0]?.code || '')

const applyResult = (data: any) => {
  purchase.value = data?.purchase || null
  outbound.value = data?.outbound || null
  traces.value = data?.traces || []
  candidates.value = data?.candidates || []
  if (purchase.value?.orderNo) {
    keyword.value = data.keyword || purchase.value.orderNo
  } else if (outbound.value?.invoiceNo || outbound.value?.orderNo) {
    keyword.value = data.keyword || outbound.value.invoiceNo || outbound.value.orderNo
  }
}

const handleSearch = async (e?: Event) => {
  if (e) e.preventDefault()
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入发票号、入库单号或出库单号')
    return
  }
  searching.value = true
  drawer.value = false
  try {
    const res: any = await lookupTrace(keyword.value.trim())
    applyResult(res.data)
    if (candidates.value.length) {
      ElMessage.info('有多张单据，请点选')
    }
  } catch {
    purchase.value = null
    outbound.value = null
    traces.value = []
    candidates.value = []
  } finally {
    searching.value = false
  }
}

const pickRecent = (row: any) => {
  keyword.value = row.query
  handleSearch()
}

const pickCandidate = (row: any) => {
  keyword.value = row.orderNo || row.spdid
  handleSearch()
}

const openLine = async (row: any) => {
  if (!row.spdid) {
    ElMessage.warning('该明细还没有流水号')
    return
  }
  currentLine.value = row
  currentSpdid.value = row.spdid
  const res: any = await loadTraceBySpdid(row.spdid)
  lineTraces.value = res.data || []
  selectedTrace.value = lineTraces.value.find((t) => t.status === '正常') || lineTraces.value[0] || null
  drawer.value = true
}

const pickTrace = (row: TraceRow) => {
  selectedTrace.value = row
}

const goPack = (code: string) => {
  router.push({ path: '/business/trace-pack', query: { code } })
}

const openFixScan = () => {
  if (!selectedTrace.value?.id) {
    ElMessage.warning('请先点选一条要纠正的码')
    return
  }
  scanInput.value = ''
  fixVisible.value = true
}

const openMissing = () => {
  customerNote.value = ''
  missingVisible.value = true
}

const focusScanInput = () => nextTick(() => scanInputRef.value?.focus())

const openParse = () => {
  parseCode.value = firstBig.value || '81000000000000000001'
  previewRows.value = []
  parseVisible.value = true
}

onMounted(async () => {
  try {
    const [outRes, inRes]: any[] = await Promise.all([
      loadAllOutbound({ page: 1, limit: 5 }),
      loadAllPurchase({ page: 1, limit: 5 })
    ])
    const outRows = (outRes.data || []).map((r: any) => ({
      key: 'o-' + r.id,
      query: r.invoiceNo || r.orderNo,
      label: `出库 ${r.invoiceNo || r.orderNo}`
    }))
    const inRows = (inRes.data || []).map((r: any) => ({
      key: 'p-' + r.id,
      query: r.orderNo || r.invoiceNo,
      label: `入库 ${r.orderNo || r.invoiceNo}`
    }))
    recent.value = [...inRows, ...outRows].filter((r) => r.query)
  } catch {}
  const q = route.query.keyword || route.query.q || route.query.invoiceNo || route.query.orderNo || route.query.spdid
  if (typeof q === 'string' && q.trim()) {
    keyword.value = q.trim()
    await handleSearch()
  }
})

const handleFixScan = async () => {
  if (!selectedTrace.value?.id || !scanInput.value.trim()) return
  saving.value = true
  try {
    const res: any = await replaceAbnormalTrace({
      traceId: selectedTrace.value.id,
      newCode: scanInput.value.trim(),
      useUniversal01: false,
      remark: '错码/异常'
    })
    ElMessage.success(res?.msg || '已替换')
    fixVisible.value = false
    await handleSearch()
    await openLine(currentLine.value)
  } finally {
    saving.value = false
  }
}

const handleUse01 = async () => {
  if (!selectedTrace.value?.id) {
    ElMessage.warning('请先点选一条异常码')
    return
  }
  saving.value = true
  try {
    const res: any = await replaceAbnormalTrace({
      traceId: selectedTrace.value.id,
      useUniversal01: true,
      remark: '追溯码异常，01 码代替'
    })
    ElMessage.success(res?.msg || '已用 01 码代替')
    await handleSearch()
    await openLine(currentLine.value)
  } finally {
    saving.value = false
  }
}

const handleMissing = async () => {
  if (!currentSpdid.value || !customerNote.value.trim()) return
  saving.value = true
  try {
    const res: any = await reportMissingTrace({
      spdid: currentSpdid.value,
      customerNote: customerNote.value.trim()
    })
    ElMessage.success(res?.msg || '已登记')
    missingVisible.value = false
    await handleSearch()
    await openLine(currentLine.value)
  } finally {
    saving.value = false
  }
}

const handlePreview = async () => {
  if (!parseCode.value.trim()) {
    ElMessage.warning('请输入大码或中码')
    return
  }
  previewing.value = true
  try {
    const res: any = await previewParse(parseCode.value.trim())
    previewRows.value = res.data || []
    if (!previewRows.value.length) ElMessage.warning(res.msg || '没有解析出下级码')
  } catch {
    previewRows.value = []
  } finally {
    previewing.value = false
  }
}

const handleParse = async () => {
  saving.value = true
  try {
    await parseTraceCodes({
      spdid: currentSpdid.value,
      code: parseCode.value.trim(),
      bizType: collectBizType.value
    })
    ElMessage.success('解析写入成功')
    parseVisible.value = false
    await handleSearch()
    await openLine(currentLine.value)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.hint { color: #667085; margin-bottom: 12px; }
.hint code { color: #1d4ed8; }
.scan-list {
  width: 100%;
  min-height: 60px;
  max-height: 200px;
  overflow-y: auto;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 8px;
}
.scan-empty { color: #9aa4b2; font-size: 13px; }
.scan-tag { margin: 4px 8px 4px 0; font-family: ui-monospace, monospace; }
.recent { margin: 8px 0 16px; }
.recent-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; }
.recent :deep(.el-button) { margin: 0 8px 8px 0; }
.drawer-toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.pack-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  min-height: 420px;
}
.pack-col {
  border-radius: 12px;
  padding: 12px;
  min-height: 380px;
}
.pack-col header {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
}
.pack-col header em { font-style: normal; font-size: 13px; opacity: 0.75; }
.pack-big { background: #eef4ff; }
.pack-mid { background: #f4f0ff; }
.pack-small { background: #f3faf4; }
.empty { color: #98a2b3; font-size: 13px; }
.code-card {
  display: block;
  width: 100%;
  text-align: left;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  line-height: 1.4;
  color: #1d2939;
  cursor: pointer;
}
button.code-card { cursor: pointer; }
button.code-card:hover,
.code-card.on { border-color: #2f6fed; box-shadow: 0 0 0 1px #2f6fed33; }
.code-card small {
  display: block;
  margin-top: 4px;
  color: #667085;
  font-size: 11px;
  font-family: inherit;
}
@media (max-width: 900px) {
  .pack-grid { grid-template-columns: 1fr; }
}
</style>
