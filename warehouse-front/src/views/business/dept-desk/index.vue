<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-header-title">{{ current.title }}作业台</h1>
      <p class="page-header-desc">{{ current.desc }}</p>
    </div>

    <el-tabs v-model="active" @tab-change="onTab">
      <el-tab-pane v-for="d in desks" :key="d.type" :label="d.title" :name="d.type" />
    </el-tabs>

    <div class="todo-grid">
      <router-link v-for="t in current.todos" :key="t.to" class="todo-card" :to="t.to">
        <strong>{{ t.value }}</strong>
        <span>{{ t.label }}</span>
        <em>{{ t.hint }}</em>
      </router-link>
    </div>

    <el-card class="links-card">
      <template #header>本岗入口</template>
      <div class="links">
        <el-button v-for="l in current.links" :key="l.to" @click="$router.push(l.to)">{{ l.label }}</el-button>
      </div>
    </el-card>

    <el-card v-if="active === '质量'" class="work-card">
      <template #header>待首营供应商</template>
      <el-table :data="suppliers" size="small">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column prop="name" label="供应商" min-width="160" />
        <el-table-column prop="licenseNo" label="许可证" min-width="140" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="passCamp(row)">审核通过</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="active === '质量'" class="work-card">
      <template #header>非合格批号（放行 / 停售 / 不合格）</template>
      <el-table :data="batches" size="small">
        <el-table-column prop="drugName" label="品种" min-width="140" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="qualityStatus" label="状态" width="90" />
        <el-table-column label="判定" width="280">
          <template #default="{ row }">
            <el-button link type="success" @click="setQ(row, '合格')">放行合格</el-button>
            <el-button link type="warning" @click="setQ(row, '停售')">停售</el-button>
            <el-button link type="danger" @click="setQ(row, '不合格')">不合格</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="active === '财务'" class="work-card">
      <template #header>未回款出库单</template>
      <el-table :data="unpaid" size="small">
        <el-table-column prop="invoiceNo" label="发票号" min-width="180" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column prop="paidAmount" label="已回" width="90" />
        <el-table-column prop="paidStatus" label="状态" width="100" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push('/business/offset')">去冲账</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loadDeskSummary, loadPendingSuppliers, approveFirstCamp, loadPendingBatches, setBatchQuality } from '@/api/desk'
import { loadUnpaidOutbound } from '@/api/outbound'

const route = useRoute()
const router = useRouter()
const summary = ref<any>({})
const suppliers = ref<any[]>([])
const batches = ref<any[]>([])
const unpaid = ref<any[]>([])
const active = ref(String(route.query.type || '采购'))

const desks = [
  { type: '采购', title: '采购', desc: '开入库草稿、按缺货补货、看供应商。确认入库由仓管做。' },
  { type: '销售', title: '销售', desc: '开出库、医院收货、缺货登记、销退通知。' },
  { type: '仓储', title: '仓储', desc: '确认入库、批号库存、到货异常、日清。' },
  { type: '质量', title: '质量', desc: '供应商首营、到货异常判定、批号放行/停售。' },
  { type: '财务', title: '财务', desc: '未回款、冲账、进销统计、月结。不改库存。' },
  { type: '信息', title: '信息·IT', desc: '客户要货进 BMS → 预处理 → 开票员开票下载。网单不能直接发货。' },
  { type: '物流', title: '信息·物流', desc: '物流联系单、出库打印包、分货协同。开票完成后配合发运。' }
]

const current = computed(() => {
  const ops = summary.value.ops || {}
  const d = desks.find((x) => x.type === active.value) || desks[0]
  const packs: Record<string, { todos: any[]; links: { label: string; to: string }[] }> = {
    采购: {
      todos: [
        { value: summary.value.draftPurchase || 0, label: '采购草稿', hint: '待确认入库', to: '/business/purchase' },
        { value: ops.stockoutOpen || 0, label: '缺货待补', hint: '按登记补货', to: '/business/stockout' },
        { value: summary.value.pendingFirstCamp || 0, label: '未过首营', hint: '供应商', to: '/business/supplier' }
      ],
      links: [
        { label: '采购入库单', to: '/business/purchase' },
        { label: '供应商发票查询', to: '/business/purchase-invoice' },
        { label: '供应商', to: '/business/supplier' },
        { label: '缺货补货', to: '/business/stockout' },
        { label: '统筹值', to: '/business/quota' }
      ]
    },
    销售: {
      todos: [
        { value: summary.value.draftOutbound || 0, label: '出库草稿', hint: '待确认', to: '/business/outbound' },
        { value: ops.pendingReceipt || 0, label: '待收货', hint: '医院签收', to: '/business/receipt' },
        { value: ops.stockoutOpen || 0, label: '开票缺货', hint: '登记', to: '/business/stockout' },
        { value: ops.returnOpen || 0, label: '销退在途', hint: '通知', to: '/business/return-notice' }
      ],
      links: [
        { label: '销售出库单', to: '/business/outbound' },
        { label: '客户', to: '/business/customer' },
        { label: '医院收货', to: '/business/receipt' },
        { label: '追溯查询', to: '/business/trace' },
        { label: '大码解析', to: '/business/trace-pack' },
        { label: '收款冲账', to: '/business/offset' },
        { label: '单位欠款', to: '/business/customer-debt' }
      ]
    },
    仓储: {
      todos: [
        { value: summary.value.draftPurchase || 0, label: '待确认入库', hint: '保管过账', to: '/business/purchase' },
        { value: ops.inboundExOpen || 0, label: '到货异常', hint: '待处理', to: '/business/inbound-ex' },
        { value: ops.allocateOpen || 0, label: '待分货', hint: '拣货', to: '/business/allocate' },
        { value: summary.value.abnormalBatch || 0, label: '非合格库存', hint: '待质量', to: '/business/batch-stock' }
      ],
      links: [
        { label: '批号库存', to: '/business/batch-stock' },
        { label: '采购确认入库', to: '/business/purchase' },
        { label: '日清检查', to: '/business/daily-close' },
        { label: '到货异常', to: '/business/inbound-ex' },
        { label: '追溯查询', to: '/business/trace' },
        { label: '大码解析', to: '/business/trace-pack' }
      ]
    },
    质量: {
      todos: [
        { value: summary.value.pendingFirstCamp || 0, label: '待首营', hint: '供应商', to: '/business/supplier' },
        { value: ops.inboundExOpen || 0, label: '到货异常', hint: '合格/拒收', to: '/business/inbound-ex' },
        { value: summary.value.abnormalBatch || 0, label: '非合格批号', hint: '放行或停售', to: '/business/batch-stock' }
      ],
      links: [
        { label: '供应商首营', to: '/business/supplier' },
        { label: '到货异常', to: '/business/inbound-ex' },
        { label: '批号库存', to: '/business/batch-stock' }
      ]
    },
    财务: {
      todos: [
        { value: ops.unpaidOrders || 0, label: '未回款', hint: '出库单', to: '/business/customer-debt' },
        { value: ops.receiptOpen || 0, label: '未认领到账', hint: '银行流水', to: '/business/bank-receipt' },
        { value: ops.offsetOpen || 0, label: '待冲账', hint: '勾兑发票', to: '/business/offset' }
      ],
      links: [
        { label: '银行到账', to: '/business/bank-receipt' },
        { label: '单位欠款', to: '/business/customer-debt' },
        { label: '收款冲账', to: '/business/offset' },
        { label: '进货统计', to: '/business/purchase-stats' },
        { label: '出货统计', to: '/business/outbound-stats' },
        { label: '月结对账', to: '/business/monthly-close' }
      ]
    },
    信息: {
      todos: [
        { value: summary.value.draftOutbound || 0, label: '待开票草稿', hint: '模拟开具电子发票', to: '/business/invoice' },
        { value: summary.value.userCount || 0, label: '启用账号', hint: '人员', to: '/system/user' },
        { value: summary.value.todayLogin || 0, label: '今日登录', hint: '日志', to: '/system/loginfo' }
      ],
      links: [
        { label: '模拟开票', to: '/business/invoice' },
        { label: '人员管理', to: '/system/user' },
        { label: '角色管理', to: '/system/role' },
        { label: '菜单管理', to: '/system/menu' },
        { label: '登录日志', to: '/system/loginfo' }
      ]
    },
    物流: {
      todos: [
        { value: ops.logisticsOpen || 0, label: '物流联系单', hint: '预约/特殊送货', to: '/business/logistics' },
        { value: ops.allocateOpen || 0, label: '待分货', hint: '拣货协同', to: '/business/allocate' },
        { value: ops.pendingReceipt || 0, label: '待收货', hint: '在途', to: '/business/receipt' }
      ],
      links: [
        { label: '物流联系单', to: '/business/logistics' },
        { label: '出库打印包', to: '/business/print-pack' },
        { label: '分货', to: '/business/allocate' },
        { label: '销售出库（发运）', to: '/business/outbound' }
      ]
    }
  }
  const pack = packs[d.type]
  return { ...d, todos: pack.todos, links: pack.links }
})

async function load() {
  const res: any = await loadDeskSummary()
  summary.value = res.data || {}
  if (active.value === '质量') {
    const s: any = await loadPendingSuppliers({ page: 1, limit: 20 })
    suppliers.value = s.data || []
    const b: any = await loadPendingBatches({ page: 1, limit: 20 })
    batches.value = b.data || []
  }
  if (active.value === '财务') {
    const u: any = await loadUnpaidOutbound({ page: 1, limit: 20 })
    unpaid.value = u.data || []
  }
}

function onTab(name: string | number) {
  router.replace({ path: '/business/dept-desk', query: { type: String(name) } })
}

async function passCamp(row: any) {
  const res: any = await approveFirstCamp(row.id)
  if (res.code === 200) {
    ElMessage.success(res.msg || '已通过')
    load()
  }
}

async function setQ(row: any, status: string) {
  const res: any = await setBatchQuality(row.id, status)
  if (res.code === 200) {
    ElMessage.success(res.msg || '已更新')
    load()
  }
}

watch(() => route.query.type, (t) => {
  if (t) active.value = String(t)
  load()
})

onMounted(load)
</script>

<style scoped>
.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.todo-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
}
.todo-card strong { font-size: 28px; color: var(--primary-color); }
.todo-card span { font-weight: 600; }
.todo-card em { font-style: normal; font-size: 12px; color: var(--text-secondary); }
.links { display: flex; flex-wrap: wrap; gap: 8px; }
.work-card, .links-card { margin-top: 16px; }
</style>
