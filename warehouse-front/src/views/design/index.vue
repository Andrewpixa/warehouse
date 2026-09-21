<template>
  <div class="design-page">
    <header class="bar">
      <router-link class="brand" to="/welcome">
        <span class="logo">课</span>
        <span>课程设计图册</span>
      </router-link>
      <nav class="links">
        <a v-for="item in nav" :key="item.id" href="#" @click.prevent="go(item.id)">{{ item.label }}</a>
      </nav>
      <router-link class="cta" to="/login">进入系统</router-link>
    </header>

    <div class="layout">
      <aside class="toc">
        <p class="toc-kicker">CONTENTS</p>
        <button
          v-for="item in nav"
          :key="item.id"
          type="button"
          :class="{ on: active === item.id }"
          @click="go(item.id)"
        >{{ item.label }}</button>
        <p class="toc-note">{{ useCases.length }} 个用例 · {{ queries.length }} 个查询 · {{ reports.length }} 张报表</p>
      </aside>

      <main class="paper">
        <section id="overview" class="hero">
          <p class="eyebrow">COURSE DESIGN</p>
          <h1>药企进销存 · 老师要看的图</h1>
          <p class="lead">
            行业选药品流通。下面按课堂要求放齐：架构、用例、ER、类图、时序、状态、查询统计、报表和技术选型。
            短文见 <code>docs/course/技术选型.md</code>、<code>docs/course/架构设计.md</code>；图与 <code>docs/pharma-er-uml.md</code> 同一套模型。
          </p>
        </section>

        <section id="arch">
          <h2>1. 软件基本架构</h2>
          <p>浏览器和手机走 Nginx。静态页是 Vue，接口进 Spring Boot，库存与单据落 MySQL。权限和操作日志走 AOP，不写进每张业务表。</p>
          <MermaidBlock :code="diagrams.architecture" caption="图 1-1 软件分层：客户端 → Nginx → Controller → Service → MyBatis-Plus → MySQL" />
          <MermaidBlock :code="diagrams.deploy" caption="图 1-2 部署：dist 静态资源 + /warehouse 反代到 8899" />
        </section>

        <section id="usecase">
          <h2>2. 用例图 · {{ useCases.length }} 个</h2>
          <p>角色：管理员、采购、仓管、销售、财务、医院收货。一张图画不下，按模块拆三张，下面表是完整清单。</p>
          <MermaidBlock :code="diagrams.usecaseSystem" caption="图 2-1 系统、权限、档案 UC01–UC14" />
          <MermaidBlock :code="diagrams.usecaseTrade" caption="图 2-2 进货、出库、红冲、回款、收货 UC15–UC24" />
          <MermaidBlock :code="diagrams.usecaseStock" caption="图 2-3 仓储、盘点、调拨、追溯、日清月结、报表 UC25–UC36" />

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编号</th>
                  <th>用例</th>
                  <th>角色</th>
                  <th>模块</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in useCases" :key="item.id">
                  <td>{{ item.id }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.actor }}</td>
                  <td>{{ item.module }}</td>
                  <td>{{ item.summary }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="er">
          <h2>3. ER 图</h2>
          <p>药企账粒度是药品 × 仓库 × 批号 × 质量状态。旧仓管 bus_* 按 SKU 扣库存，不画进这组图。</p>
          <MermaidBlock :code="diagrams.erOverview" caption="图 3-1 总览，只画关系" />
          <MermaidBlock :code="diagrams.erMaster" caption="图 3-2 档案层" />
          <MermaidBlock :code="diagrams.erIn" caption="图 3-3 进货层" />
          <MermaidBlock :code="diagrams.erOut" caption="图 3-4 出库层：红冲、回款、收货同表不同字段" />
          <MermaidBlock :code="diagrams.erStock" caption="图 3-5 批号库存与升益，唯一键为药品+仓库+批号+质量状态" />
          <MermaidBlock :code="diagrams.erTrace" caption="图 3-6 追溯码靠 SPDID 对齐明细，包装树自关联" />
          <MermaidBlock :code="diagrams.erClose" caption="图 3-7 日清 / 月结是汇总快照，不挂单据外键" />
        </section>

        <section id="class">
          <h2>4. 类图</h2>
          <p>类名与后端实体一致。exist=false 的展示字段不进图。</p>
          <MermaidBlock :code="diagrams.classMaster" caption="图 4-1 档案" />
          <MermaidBlock :code="diagrams.classOrder" caption="图 4-2 进销存单据" />
          <MermaidBlock :code="diagrams.classStock" caption="图 4-3 库存、升益、追溯、结账" />
        </section>

        <section id="seq">
          <h2>5. 时序图</h2>
          <MermaidBlock :code="diagrams.seqLogin" caption="图 5-1 登录：验证码一次性使用" />
          <MermaidBlock :code="diagrams.seqPurchase" caption="图 5-2 进货草稿 → 确认入库 → 批号库存增加" />
          <MermaidBlock :code="diagrams.seqOutbound" caption="图 5-3 出库确认扣库存，再按发票扫码挂追溯码" />
          <MermaidBlock :code="diagrams.seqReversal" caption="图 5-4 红冲另开新单，确认后加回库存" />
          <MermaidBlock :code="diagrams.seqReceipt" caption="图 5-5 医院收货，重复提交拒绝" />
        </section>

        <section id="state">
          <h2>6. 状态图</h2>
          <MermaidBlock :code="diagrams.stateOrder" caption="图 6-1 进货 / 出库 / 升益：草稿或已确认" />
          <MermaidBlock :code="diagrams.statePaid" caption="图 6-2 回款三态" />
          <MermaidBlock :code="diagrams.stateReceipt" caption="图 6-3 医院收货" />
          <MermaidBlock :code="diagrams.stateTrace" caption="图 6-4 追溯码正常或作废" />
        </section>

        <section id="query">
          <h2>7. 查询统计 · {{ queries.length }} 个</h2>
          <p>课堂要求不少于 20 个查询统计。下列均可在 Web 对应页筛选或汇总。</p>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编号</th>
                  <th>名称</th>
                  <th>系统入口</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in queries" :key="item.id">
                  <td>{{ item.id }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.entry }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="report">
          <h2>8. 报表 · {{ reports.length }} 张</h2>
          <p>课堂要求不少于 15 张报表。金额/商品/利润、进货出货、日清月结、欠款、流向、库存、收货红冲与看板如下。</p>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编号</th>
                  <th>名称</th>
                  <th>系统入口</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in reports" :key="item.id">
                  <td>{{ item.id }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.entry }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="tech">
          <h2>9. 技术选型</h2>
          <p>短文：<code>docs/course/技术选型.md</code>。继续用开源仓管底座，不另换 SQL Server。答辩时可以说：模型按药企设计，库可迁，应用层换数据源即可。</p>
          <MermaidBlock :code="diagrams.techSelect" caption="图 9-1 选型结论" />
          <div class="table-wrap">
            <table>
              <thead>
                <tr><th>层</th><th>选定</th><th>为什么</th></tr>
              </thead>
              <tbody>
                <tr><td>后端</td><td>Spring Boot 3.5 + Java 21</td><td>与开源底座一致，REST + 事务好写进销存确认。</td></tr>
                <tr><td>持久化</td><td>MyBatis-Plus + MySQL 8</td><td>本机和演示机都好装；批号库存用条件 UPDATE 防超卖。</td></tr>
                <tr><td>权限</td><td>SA-Token</td><td>会话 Cookie，菜单和按钮权限能对上岗位。</td></tr>
                <tr><td>Web</td><td>Vue 3 + TypeScript + Element Plus</td><td>B/S 作业台、表格和打印够用。</td></tr>
                <tr><td>移动</td><td>uni-app X / Flutter</td><td>盘点、查询、扫码分端做，不和 Web 抢同一套页面。</td></tr>
                <tr><td>图与文档</td><td>Mermaid</td><td>ER / UML 写在仓库里，网页直接渲染，改一处两边一致。</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="ci">
          <h2>10. 持续集成是什么</h2>
          <p>
            持续集成（Continuous Integration，CI）是：多人改代码后，<strong>自动</strong>把最新代码拉下来、编译、跑测试，尽快发现谁把主分支弄坏了。
            它不是“已经部署到公网”，也不是手工在服务器上 <code>mvn package</code>。
          </p>
          <ul class="plain">
            <li><strong>集成</strong>：把各自分支的改动合到一条主线。</li>
            <li><strong>持续</strong>：每次推送或每天多次，而不是期末才合一次。</li>
            <li><strong>自动</strong>：用 GitHub Actions、GitLab CI、Jenkins 等流水线执行，不靠人记步骤。</li>
          </ul>
          <p>
            常见流水线：<code>git push</code> → 装依赖 → <code>mvn test</code> / <code>npm run build</code> → 通过才允许合并。
            再往后把产物发到服务器，叫持续交付或持续部署（CD）。
          </p>
          <p>
            本仓库：<code>.github/workflows/ci.yml</code> 在 push / PR 时跑后端 <code>mvn test</code> 与前端 <code>npm run build</code>。
            公网演示仍是手工发布；<code>deploy/</code> 里有备份、巡检、回滚。CI 负责发现构建被破坏，不自动上线。
          </p>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import MermaidBlock from '@/components/MermaidBlock.vue'
import * as diagrams from './diagrams'
import { queries, reports, useCases } from './use-cases'

const nav = [
  { id: 'overview', label: '总览' },
  { id: 'arch', label: '架构' },
  { id: 'usecase', label: '用例' },
  { id: 'er', label: 'ER' },
  { id: 'class', label: '类图' },
  { id: 'seq', label: '时序' },
  { id: 'state', label: '状态' },
  { id: 'query', label: '查询' },
  { id: 'report', label: '报表' },
  { id: 'tech', label: '选型' },
  { id: 'ci', label: '持续集成' }
]

const active = ref('overview')

const go = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const onScroll = () => {
  const y = window.scrollY + 120
  for (const item of [...nav].reverse()) {
    const el = document.getElementById(item.id)
    if (el && el.offsetTop <= y) {
      active.value = item.id
      break
    }
  }
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.design-page {
  --ink: #f6f1e8;
  --muted: rgba(246, 241, 232, 0.64);
  --line: rgba(246, 241, 232, 0.1);
  --gold: #e8c39a;
  --rose: #fb7185;
  min-height: 100vh;
  background: #07070a;
  color: var(--ink);
  font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

:global(html:has(.design-page)),
:global(body:has(.design-page)) {
  background: #07070a;
}

.bar {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--line);
  background: rgba(7, 7, 10, 0.82);
  backdrop-filter: blur(16px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink) !important;
  font-weight: 600;
  font-size: 14px;
}

.logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #e11d48, #fb7185);
  font-size: 13px;
}

.links {
  display: flex;
  gap: 18px;
}

.links a {
  color: var(--muted) !important;
  font-size: 13px;
}

.cta {
  color: #07070a !important;
  background: var(--gold);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
}

.layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 8px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 20px 80px;
}

.toc {
  position: sticky;
  top: 84px;
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.toc-kicker {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.16em;
  color: var(--gold);
}

.toc button {
  text-align: left;
  border: 0;
  background: transparent;
  color: var(--muted);
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.toc button.on {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ink);
}

.toc-note {
  margin: 12px 10px 0;
  font-size: 12px;
  color: rgba(246, 241, 232, 0.4);
  line-height: 1.5;
}

.paper {
  padding: 8px 12px 40px;
}

.hero {
  padding: 28px 0 12px;
}

.eyebrow {
  letter-spacing: 0.2em;
  font-size: 12px;
  color: var(--gold);
}

h1 {
  margin: 8px 0 12px;
  font-size: 36px;
  font-weight: 650;
  letter-spacing: -0.03em;
}

.lead, section p, .plain {
  color: var(--muted);
  line-height: 1.75;
  font-size: 15px;
}

h2 {
  margin: 48px 0 12px;
  font-size: 22px;
}

code {
  font-size: 13px;
  color: var(--gold);
}

.table-wrap {
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 14px;
  margin: 18px 0 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th, td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  vertical-align: top;
}

th { color: var(--gold); font-weight: 600; }
td:first-child { color: var(--rose); white-space: nowrap; }

.chip-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
  padding: 0;
  margin: 16px 0 0;
  list-style: none;
  counter-reset: q;
}

.chip-list li {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--ink);
  counter-increment: q;
}

.chip-list li::before {
  content: counter(q) ". ";
  color: var(--gold);
}

.plain {
  padding-left: 20px;
}

.plain li { margin: 8px 0; }

@media (max-width: 900px) {
  .links { display: none; }
  .layout { grid-template-columns: 1fr; }
  .toc { display: none; }
  h1 { font-size: 28px; }
}

@media print {
  .bar, .toc, .cta { display: none !important; }
  .layout { display: block; max-width: none; }
  .design-page { background: #fff; color: #111; }
  .lead, section p, td, .chip-list li { color: #333; }
}
</style>
