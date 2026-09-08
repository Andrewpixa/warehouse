import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '后台首页' }
        },
        {
          path: 'system/user',
          name: 'UserManager',
          component: () => import('@/views/system/user/index.vue'),
          meta: { title: '人员管理' }
        },
        {
          path: 'system/user/profile',
          name: 'UserProfile',
          component: () => import('@/views/system/user/profile.vue'),
          meta: { title: '个人信息' }
        },
        {
          path: 'system/user/changePassword',
          name: 'ChangePassword',
          component: () => import('@/views/system/user/changePassword.vue'),
          meta: { title: '修改密码' }
        },
        {
          path: 'system/role',
          name: 'RoleManager',
          component: () => import('@/views/system/role/index.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'system/dept',
          name: 'DeptManager',
          component: () => import('@/views/system/dept/index.vue'),
          meta: { title: '部门管理' }
        },
        {
          path: 'system/menu',
          name: 'MenuManager',
          component: () => import('@/views/system/menu/index.vue'),
          meta: { title: '菜单管理' }
        },
        {
          path: 'system/permission',
          name: 'PermissionManager',
          component: () => import('@/views/system/permission/index.vue'),
          meta: { title: '权限管理' }
        },
        {
          path: 'system/notice',
          name: 'NoticeManager',
          component: () => import('@/views/system/notice/index.vue'),
          meta: { title: '公告管理' }
        },
        {
          path: 'system/loginfo',
          name: 'LoginfoManager',
          component: () => import('@/views/system/loginfo/index.vue'),
          meta: { title: '登录日志' }
        },
        {
          path: 'system/operation-log',
          name: 'OperationLogManager',
          component: () => import('@/views/system/operation-log/index.vue'),
          meta: { title: '操作日志' }
        },
        {
          path: 'system/icon',
          name: 'IconManager',
          component: () => import('@/views/system/icon/index.vue'),
          meta: { title: '图标管理' }
        },
        {
          path: 'business/customer',
          name: 'CustomerManager',
          component: () => import('@/views/business/customer/index.vue'),
          meta: { title: '客户管理' }
        },
        {
          path: 'business/provider',
          name: 'ProviderManager',
          component: () => import('@/views/business/provider/index.vue'),
          meta: { title: '供应商管理(旧仓管)' }
        },
        {
          path: 'business/supplier',
          name: 'SupplierManager',
          component: () => import('@/views/business/supplier/index.vue'),
          meta: { title: '供应商管理' }
        },
        {
          path: 'business/category',
          name: 'CategoryManager',
          component: () => import('@/views/business/category/index.vue'),
          meta: { title: '药品分类(旧)' }
        },
        {
          path: 'business/goods',
          name: 'GoodsManager',
          component: () => import('@/views/business/goods/index.vue'),
          meta: { title: '商品管理(旧)' }
        },
        {
          path: 'business/drug',
          name: 'DrugManager',
          component: () => import('@/views/business/drug/index.vue'),
          meta: { title: '品种管理' }
        },
        {
          path: 'business/purchase',
          name: 'PurchaseManager',
          component: () => import('@/views/business/purchase/index.vue'),
          meta: { title: '采购入库单' }
        },
        {
          path: 'business/outbound',
          name: 'OutboundManager',
          component: () => import('@/views/business/outbound/index.vue'),
          meta: { title: '销售出库单' }
        },
        {
          path: 'business/receipt',
          name: 'ReceiptManager',
          component: () => import('@/views/business/receipt/index.vue'),
          meta: { title: '医院收货确认' }
        },
        {
          path: 'business/batch-stock',
          name: 'BatchStockManager',
          component: () => import('@/views/business/batch-stock/index.vue'),
          meta: { title: '批号库存' }
        },
        {
          path: 'business/surplus',
          name: 'SurplusManager',
          component: () => import('@/views/business/surplus/index.vue'),
          meta: { title: '批号升益' }
        },
        {
          path: 'business/daily-close',
          name: 'DailyClose',
          component: () => import('@/views/business/daily-close/index.vue'),
          meta: { title: '日清检查' }
        },
        {
          path: 'business/monthly-close',
          name: 'MonthlyClose',
          component: () => import('@/views/business/monthly-close/index.vue'),
          meta: { title: '月结对账' }
        },
        {
          path: 'business/purchase-stats',
          name: 'PurchaseStats',
          component: () => import('@/views/business/purchase-stats/index.vue'),
          meta: { title: '进货统计' }
        },
        {
          path: 'business/outbound-stats',
          name: 'OutboundStats',
          component: () => import('@/views/business/outbound-stats/index.vue'),
          meta: { title: '出货统计' }
        },
        {
          path: 'business/trace',
          name: 'TraceManager',
          component: () => import('@/views/business/trace/index.vue'),
          meta: { title: '追溯码查询' }
        },
        {
          path: 'business/inport-pos',
          name: 'InportPOS',
          component: () => import('@/views/business/inport-pos/index.vue'),
          meta: { title: '商品进货' }
        },
        {
          path: 'business/inport-order',
          name: 'InportOrder',
          component: () => import('@/views/business/inport-order/index.vue'),
          meta: { title: '进货订单' }
        },
        {
          path: 'business/inport-record',
          name: 'InportRecord',
          component: () => import('@/views/business/inport-record/index.vue'),
          meta: { title: '进货退加货记录' }
        },
        {
          path: 'business/sales-pos',
          name: 'SalesPOS',
          component: () => import('@/views/business/sales-pos/index.vue'),
          meta: { title: '商品销售' }
        },
        {
          path: 'business/sales-order',
          name: 'SalesOrder',
          component: () => import('@/views/business/sales-order/index.vue'),
          meta: { title: '销售订单' }
        },
        {
          path: 'business/sales-record',
          name: 'SalesRecord',
          component: () => import('@/views/business/sales-record/index.vue'),
          meta: { title: '销售退加货记录' }
        },
        {
          path: 'business/retail',
          name: 'RetailPOS',
          component: () => import('@/views/business/retail/index.vue'),
          meta: { title: '散客零售' }
        },
        {
          path: 'business/retail-order',
          name: 'RetailOrder',
          component: () => import('@/views/business/retail-order/index.vue'),
          meta: { title: '零售订单' }
        },
        {
          path: 'business/retail-record',
          name: 'RetailRecord',
          component: () => import('@/views/business/retail-record/index.vue'),
          meta: { title: '零售退回记录' }
        },
        {
          path: 'business/report',
          name: 'ReportManager',
          component: () => import('@/views/business/report/index.vue'),
          meta: { title: '报表管理' }
        },
        {
          path: 'business/inport-analysis',
          name: 'InportAnalysis',
          component: () => import('@/views/business/inport-analysis/index.vue'),
          meta: { title: '进销金额分析' }
        },
        {
          path: 'business/goods-analysis',
          name: 'GoodsAnalysis',
          component: () => import('@/views/business/goods-analysis/index.vue'),
          meta: { title: '进销商品分析' }
        },
        {
          path: 'business/profit-analysis',
          name: 'ProfitAnalysis',
          component: () => import('@/views/business/profit-analysis/index.vue'),
          meta: { title: '利润分析' }
        },
        {
          path: 'business/stocktake',
          name: 'StocktakeManager',
          component: () => import('@/views/business/stocktake/index.vue'),
          meta: { title: '盘点管理' }
        },
        {
          path: 'business/member',
          name: 'MemberManager',
          component: () => import('@/views/business/member/index.vue'),
          meta: { title: '会员列表' }
        },
        {
          path: 'business/member-level',
          name: 'MemberLevelManager',
          component: () => import('@/views/business/member/level.vue'),
          meta: { title: '等级规则' }
        },
        {
          path: 'business/performance',
          name: 'PerformanceRanking',
          component: () => import('@/views/business/performance/index.vue'),
          meta: { title: '业绩排名' }
        },
        {
          path: 'business/commission',
          name: 'CommissionManager',
          component: () => import('@/views/business/commission/index.vue'),
          meta: { title: '店员提成' }
        },
        {
          path: 'business/serial-number',
          name: 'SerialNumberManager',
          component: () => import('@/views/business/serial-number/index.vue'),
          meta: { title: '序列号管理' }
        },
        {
          path: 'business/warehouse',
          name: 'WarehouseManager',
          component: () => import('@/views/business/warehouse/index.vue'),
          meta: { title: '仓库管理' }
        },
        {
          path: 'business/transfer',
          name: 'TransferManager',
          component: () => import('@/views/business/transfer/index.vue'),
          meta: { title: '库存调拨' }
        },
        {
          path: 'business/goods-stock',
          name: 'GoodsStockManager',
          component: () => import('@/views/business/goods-stock/index.vue'),
          meta: { title: '分仓库存' }
        },
        {
          path: 'business/my-commission',
          name: 'MyCommission',
          component: () => import('@/views/business/my-commission/index.vue'),
          meta: { title: '我的提成' }
        }
      ]
    }
  ]
})

NProgress.configure({ showSpinner: false })

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = ((to.meta.title as string) || '药品进销存') + ' - 药品进销存'

  if (to.path === '/login') {
    next()
    return
  }

  const authStore = useAuthStore()

  if (!authStore.isLoggedIn) {
    try {
      await authStore.fetchCurrentUser()
      next()
    } catch (e) {
      console.error('路由守卫认证失败:', e)
      next('/login')
    }
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
