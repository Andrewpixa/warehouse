<template>
  <div class="intro-page" :class="{ 'is-ready': ready }">
    <div class="progress" :style="{ transform: `scaleX(${progress})` }" />

    <div v-if="booting" class="boot" aria-hidden="true">
      <div class="boot-mark">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M20 7L12 3L4 7V17L12 21L20 17V7Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
          <path d="M12 12L20 7M12 12V21M12 12L4 7" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
        </svg>
      </div>
      <p class="boot-word">PHARMA IMS</p>
      <div class="boot-track"><i /></div>
    </div>

    <header class="nav" :class="{ 'is-scrolled': scrolled }">
      <a class="nav-brand" href="#top" @click.prevent="scrollTo('top')">
        <span class="nav-logo">
          <svg viewBox="0 0 24 24" fill="none">
            <path d="M20 7L12 3L4 7V17L12 21L20 17V7Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M12 12L20 7M12 12V21M12 12L4 7" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="nav-name">药企进销存</span>
      </a>
      <nav class="nav-links" aria-label="页面导航">
        <a href="#capabilities" @click.prevent="scrollTo('capabilities')">能力</a>
        <a href="#trace" @click.prevent="scrollTo('trace')">追溯</a>
        <a href="#trust" @click.prevent="scrollTo('trust')">信任</a>
      </nav>
      <button class="nav-cta" type="button" @click="enter">{{ ctaLabel }}</button>
    </header>

    <main id="top">
      <section class="hero">
        <div class="hero-bg" aria-hidden="true">
          <div class="grid" />
          <div class="noise" />
          <div class="orb orb-a" />
          <div class="orb orb-b" />
          <div class="orb orb-c" />
          <span v-for="n in 18" :key="n" class="speck" :style="speckStyle(n)" />
        </div>

        <div class="hero-copy">
          <p class="eyebrow reveal">
            <span class="pulse-dot" />
            GSP · 批号库存 · 追溯码
          </p>
          <h1 class="hero-title">
            <span class="reveal d1">让每一粒药</span>
            <span class="reveal d2 gradient-line">都有据可循</span>
          </h1>
          <p class="hero-sub reveal d3">
            为医药流通企业打造的进销存系统。采购入库、批号库存、销售出库、医院收货与日清月结，一条链路贯穿药品流通。
          </p>
          <div class="hero-actions reveal d4">
            <button class="btn-primary" type="button" @click="enter">{{ ctaLabel }}</button>
            <button class="btn-ghost" type="button" @click="scrollTo('capabilities')">
              了解产品能力
              <span class="chev">↓</span>
            </button>
          </div>
          <ul class="hero-meta reveal d5">
            <li>批号级精度</li>
            <li>全链路追溯</li>
            <li>日清 / 月结闭环</li>
          </ul>
        </div>

        <div
          class="stage reveal d3"
          @mousemove="onTilt"
          @mouseleave="resetTilt"
        >
          <div class="stage-glow" />
          <div class="device" :style="deviceStyle">
            <div class="device-chrome">
              <span class="chrome-dots"><i /><i /><i /></span>
              <span class="chrome-title">批号库存 · LIVE</span>
              <span class="chrome-badge">TRACE ON</span>
            </div>
            <div class="device-body">
              <aside class="side">
                <div class="side-item on">总览</div>
                <div class="side-item">采购入库</div>
                <div class="side-item">批号库存</div>
                <div class="side-item">追溯码</div>
                <div class="side-item">日清月结</div>
              </aside>
              <div class="panel">
                <div class="mini-stats">
                  <div class="mini-stat" v-for="s in miniStats" :key="s.k">
                    <em>{{ s.v }}</em>
                    <span>{{ s.k }}</span>
                  </div>
                </div>
                <div class="scan-box">
                  <div class="barcode" aria-hidden="true">
                    <i v-for="w in bars" :key="w.i" :style="{ width: w.w + 'px', opacity: w.o }" />
                    <div class="scan-line" />
                  </div>
                  <div class="scan-meta">
                    <span class="code">(01)06901234567890</span>
                    <span class="ok">追溯码已关联 · 爱创解析通过</span>
                  </div>
                </div>
                <div class="rows">
                  <div class="row" v-for="row in batchRows" :key="row.lot">
                    <span class="row-name">{{ row.name }}</span>
                    <span class="row-lot">{{ row.lot }}</span>
                    <span class="row-tag" :class="row.tone">{{ row.tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="float-card fa">
            <b>近效期预警</b>
            <span>3 个批号不足 90 天</span>
          </div>
          <div class="float-card fb">
            <b>医院收货</b>
            <span>今日已确认 12 单</span>
          </div>
        </div>
      </section>

      <div class="marquee" aria-hidden="true">
        <div class="marquee-track">
          <span v-for="(item, i) in marqueeItems" :key="'a' + i">{{ item }}</span>
          <span v-for="(item, i) in marqueeItems" :key="'b' + i">{{ item }}</span>
        </div>
      </div>

      <section id="capabilities" class="section">
        <div class="section-head">
          <p class="eyebrow reveal">PRODUCT CAPABILITIES</p>
          <h2 class="section-title reveal d1">一套系统，覆盖流通全链路</h2>
          <p class="section-sub reveal d2">从入库到出库，从批号到追溯码，把日常作业做成可审计、可回溯的闭环。</p>
        </div>
        <div class="bento">
          <article class="card c-lg reveal">
            <p class="card-kicker">01 · 批号库存</p>
            <h3>每一批货，效期与数量都在掌握中</h3>
            <p>按批号管理库存，近效期自动预警，升益、盘点、分仓一目了然。</p>
            <div class="lot-visual">
              <div class="lot-bar" v-for="b in lotBars" :key="b.n">
                <span>{{ b.n }}</span>
                <i :style="{ width: b.w }"><em /></i>
              </div>
            </div>
          </article>
          <article class="card reveal d1">
            <p class="card-kicker">02 · 追溯码</p>
            <h3>GS1 / 01 码分流查询</h3>
            <p>扫码即见来源与流向，关联入库、出库与医院收货。</p>
            <div class="mini-code">
              <div class="mini-bars"><i v-for="n in 16" :key="n" /></div>
              <span>码上放心对接</span>
            </div>
          </article>
          <article class="card reveal d2">
            <p class="card-kicker">03 · 入出库闭环</p>
            <h3>采购进、销售出，单据不断档</h3>
            <p>进货订单、出库单、退加货记录串成一条作业流。</p>
            <div class="loop">
              <span>入库</span>
              <i />
              <span>在库</span>
              <i />
              <span>出库</span>
            </div>
          </article>
          <article class="card reveal">
            <p class="card-kicker">04 · 医院收货</p>
            <h3>下游确认，流向落袋为安</h3>
            <p>医院收货确认回写单据，减少对账扯皮与流向争议。</p>
          </article>
          <article class="card reveal d1">
            <p class="card-kicker">05 · 日清月结</p>
            <h3>每天收口，每月对账</h3>
            <p>日清检查与月结对账把库存、金额、追溯一次对齐。</p>
          </article>
          <article class="card reveal d2">
            <p class="card-kicker">06 · 多仓协同</p>
            <h3>调拨、分仓、盘点一体</h3>
            <p>仓库之间调拨可追踪，分仓库存独立核算。</p>
          </article>
        </div>
      </section>

      <section id="trace" class="section trace-section">
        <div class="section-head">
          <p class="eyebrow reveal">TRACEABILITY</p>
          <h2 class="section-title reveal d1">药品流向，像时间线一样可读</h2>
          <p class="section-sub reveal d2">从供应商入库到医院收货，每一步都留下可核验的痕迹。</p>
        </div>
        <div class="flow reveal d2">
          <div class="flow-line" aria-hidden="true">
            <span class="flow-pulse" />
          </div>
          <div
            v-for="(step, i) in flowSteps"
            :key="step.t"
            class="flow-node"
            :style="{ animationDelay: `${i * 0.12}s` }"
          >
            <span class="flow-idx">0{{ i + 1 }}</span>
            <strong>{{ step.t }}</strong>
            <em>{{ step.d }}</em>
          </div>
        </div>
      </section>

      <section id="trust" class="section trust-section">
        <div class="trust-grid">
          <article class="trust-item reveal" v-for="t in trusts" :key="t.n">
            <span class="trust-num">{{ t.n }}</span>
            <h3>{{ t.t }}</h3>
            <p>{{ t.d }}</p>
          </article>
        </div>
      </section>

      <section class="cta-band">
        <div class="cta-inner reveal">
          <p class="eyebrow">READY</p>
          <h2>把流通作业，做成可信任的系统</h2>
          <p>登录后即可进入工作台，查看库存、单据与追溯全貌。</p>
          <button class="btn-primary lg" type="button" @click="enter">{{ ctaLabel }}</button>
        </div>
      </section>
    </main>

    <footer class="foot">
      <span>药企进销存 · Pharma IMS</span>
      <span>药品流通 · 批号库存 · 追溯码 © {{ year }}</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const year = new Date().getFullYear()
const booting = ref(true)
const ready = ref(false)
const scrolled = ref(false)
const progress = ref(0)
const tilt = ref({ x: 4, y: -8 })

const ctaLabel = computed(() => (authStore.isLoggedIn ? '进入工作台' : '进入系统'))

const deviceStyle = computed(() => ({
  transform: `rotateX(${tilt.value.x}deg) rotateY(${tilt.value.y}deg)`
}))

const miniStats = [
  { k: '在库批号', v: '1,284' },
  { k: '近效期', v: '03' },
  { k: '今日出库', v: '46' }
]

const batchRows = [
  { name: '阿莫西林胶囊 0.25g', lot: '240918A', tag: '近效 86 天', tone: 'warn' },
  { name: '头孢克肟分散片', lot: '240722B', tag: '库存充足', tone: 'ok' },
  { name: '注射用奥美拉唑', lot: '241103C', tag: '已关联码', tone: 'info' }
]

const bars = Array.from({ length: 28 }, (_, i) => ({
  i,
  w: 1 + ((i * 17) % 5),
  o: 0.35 + ((i * 13) % 7) / 10
}))

const marqueeItems = [
  '采购入库', '批号库存', '追溯码查询', '销售出库', '医院收货',
  '日清检查', '月结对账', '多仓调拨', '利润分析', '盘点管理'
]

const lotBars = [
  { n: '240918A', w: '86%' },
  { n: '240722B', w: '64%' },
  { n: '241103C', w: '42%' }
]

const flowSteps = [
  { t: '采购入库', d: '供应商到货，批号入账' },
  { t: '在库管理', d: '效期、升益、盘点' },
  { t: '销售出库', d: '开单出库，码随货走' },
  { t: '医院收货', d: '下游确认流向' },
  { t: '码上放心', d: '追溯链路闭环' }
]

const trusts = [
  { n: '01', t: '批号级精度', d: '库存不再只到品种。每一批的数量、效期、位置都可核对。' },
  { n: '02', t: '全链路追溯', d: 'GS1 / 01 码可查询来源与去向，对接码上放心业务节奏。' },
  { n: '03', t: '作业可审计', d: '入出库、收货、日清月结留下操作痕迹，对账与检查有据可依。' }
]

let io: IntersectionObserver | null = null
let bootTimer = 0
let ticking = false

const speckStyle = (n: number) => {
  const left = ((n * 37) % 100)
  const top = ((n * 53) % 90)
  const delay = ((n * 0.37) % 6).toFixed(2)
  const dur = (6 + (n % 5)).toFixed(1)
  return {
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${dur}s`
  }
}

const enter = () => {
  router.push(authStore.isLoggedIn ? '/dashboard' : '/login')
}

const scrollTo = (id: string) => {
  if (id === 'top') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const onTilt = (e: MouseEvent) => {
  const el = e.currentTarget as HTMLElement
  const r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width - 0.5
  const py = (e.clientY - r.top) / r.height - 0.5
  tilt.value = { x: 6 - py * 8, y: -10 + px * 12 }
}

const resetTilt = () => {
  tilt.value = { x: 4, y: -8 }
}

const onScroll = () => {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const y = window.scrollY
    const max = Math.max(1, document.documentElement.scrollHeight - window.innerHeight)
    scrolled.value = y > 20
    progress.value = Math.min(1, y / max)
    ticking = false
  })
}

onMounted(() => {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduce) {
    booting.value = false
    ready.value = true
  } else {
    bootTimer = window.setTimeout(() => {
      booting.value = false
      ready.value = true
    }, 1100)
  }

  io = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-in')
          io?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.14, rootMargin: '0px 0px -8% 0px' }
  )
  document.querySelectorAll('.intro-page .reveal').forEach((el) => io?.observe(el))

  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  io?.disconnect()
  window.clearTimeout(bootTimer)
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.intro-page {
  --ink: #f6f1e8;
  --muted: rgba(246, 241, 232, 0.64);
  --faint: rgba(246, 241, 232, 0.38);
  --line: rgba(246, 241, 232, 0.1);
  --rose: #fb7185;
  --rose-deep: #e11d48;
  --gold: #e8c39a;
  --bg: #07070a;
  --panel: rgba(255, 255, 255, 0.035);
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  font-size: 16px;
  overflow-x: clip;
  isolation: isolate;
}

:global(html:has(.intro-page)),
:global(body:has(.intro-page)) {
  background: #07070a;
}

.progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--rose-deep), var(--gold));
  transform-origin: left center;
  z-index: 80;
  pointer-events: none;
}

.boot {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: #050506;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  animation: bootOut 0.55s ease 0.9s forwards;
}

.boot-mark {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #e11d48, #fb7185);
  display: grid;
  place-items: center;
  color: #fff;
  animation: pop 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.boot-mark svg { width: 22px; height: 22px; }

.boot-word {
  letter-spacing: 0.42em;
  font-size: 11px;
  color: var(--faint);
}

.boot-track {
  width: 120px;
  height: 2px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
  border-radius: 2px;
}

.boot-track i {
  display: block;
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, var(--rose-deep), var(--gold));
  animation: load 0.9s ease forwards;
}

.nav {
  position: sticky;
  top: 0;
  z-index: 40;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid transparent;
  transition: background 0.35s ease, border-color 0.35s ease, backdrop-filter 0.35s ease;
}

.nav.is-scrolled {
  background: rgba(7, 7, 10, 0.72);
  border-bottom-color: var(--line);
  backdrop-filter: blur(18px) saturate(140%);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink) !important;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.nav-logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #e11d48, #fb7185);
  display: grid;
  place-items: center;
  color: #fff;
}

.nav-logo svg { width: 16px; height: 16px; }

.nav-name { font-size: 13px; }

.nav-links {
  display: flex;
  gap: 28px;
}

.nav-links a {
  color: var(--muted) !important;
  font-size: 13px;
  letter-spacing: 0.12em;
}

.nav-links a:hover { color: var(--ink) !important; }

.nav-cta,
.btn-primary,
.btn-ghost {
  cursor: pointer;
  border: 0;
  font: inherit;
}

.nav-cta,
.btn-primary {
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #e11d48, #fb7185);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.12em;
  box-shadow: 0 8px 28px rgba(225, 29, 72, 0.28);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.nav-cta:hover,
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 36px rgba(225, 29, 72, 0.4);
}

.btn-primary.lg {
  height: 48px;
  padding: 0 28px;
  font-size: 14px;
}

.hero {
  position: relative;
  min-height: calc(100vh - 72px);
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 40px;
  align-items: center;
  padding: 48px 64px 80px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: radial-gradient(ellipse at 50% 30%, #000 30%, transparent 75%);
}

.noise {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  animation: float 12s ease-in-out infinite;
}

.orb-a {
  width: 420px;
  height: 420px;
  left: -80px;
  top: -40px;
  background: radial-gradient(circle, rgba(225, 29, 72, 0.55), transparent 68%);
}

.orb-b {
  width: 360px;
  height: 360px;
  right: 8%;
  top: 12%;
  background: radial-gradient(circle, rgba(232, 195, 154, 0.28), transparent 70%);
  animation-delay: -4s;
}

.orb-c {
  width: 280px;
  height: 280px;
  right: 22%;
  bottom: 0;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.2), transparent 70%);
  animation-delay: -7s;
}

.speck {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.55);
  animation: twinkle 7s ease-in-out infinite;
}

.hero-copy,
.stage {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--gold);
  margin-bottom: 20px;
}

.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.6);
  animation: ping 2s infinite;
}

.hero-title {
  font-size: clamp(44px, 6vw, 76px);
  line-height: 1.05;
  font-weight: 600;
  letter-spacing: -0.045em;
  margin: 0 0 20px;
}

.hero-title span { display: block; }

.gradient-line {
  background: linear-gradient(105deg, #fff 10%, #fb7185 48%, #e8c39a 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-sub {
  max-width: 520px;
  font-size: 16px;
  line-height: 1.75;
  color: var(--muted);
  margin-bottom: 28px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}

.btn-ghost {
  height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  background: transparent;
  color: var(--ink);
  border: 1px solid var(--line);
  letter-spacing: 0.08em;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.btn-ghost:hover {
  border-color: rgba(251, 113, 133, 0.45);
  background: rgba(255, 255, 255, 0.03);
}

.btn-primary { height: 44px; }

.chev { opacity: 0.7; animation: bounce 1.8s infinite; }

.hero-meta {
  display: flex;
  gap: 18px;
  list-style: none;
  color: var(--faint);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-meta li {
  padding-left: 12px;
  border-left: 1px solid var(--line);
}

.stage {
  perspective: 1600px;
  min-height: 460px;
  padding-bottom: 40px;
}

.stage-glow {
  position: absolute;
  inset: 18% 8% 8%;
  background: radial-gradient(circle, rgba(225, 29, 72, 0.22), transparent 70%);
  filter: blur(18px);
}

.device {
  position: relative;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(180deg, rgba(28, 24, 22, 0.92), rgba(12, 11, 14, 0.96));
  box-shadow:
    0 40px 80px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  overflow: hidden;
  transform-style: preserve-3d;
  transition: transform 0.35s ease;
}

.device-chrome {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}

.chrome-dots { display: flex; gap: 6px; }
.chrome-dots i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3f3f46;
}
.chrome-dots i:nth-child(1) { background: #fb7185; }
.chrome-dots i:nth-child(2) { background: #e8c39a; }
.chrome-dots i:nth-child(3) { background: #34d399; }

.chrome-title {
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--faint);
}

.chrome-badge {
  margin-left: auto;
  font-size: 10px;
  letter-spacing: 0.16em;
  color: #34d399;
}

.device-body {
  display: grid;
  grid-template-columns: 118px 1fr;
  min-height: 340px;
}

.side {
  border-right: 1px solid var(--line);
  padding: 16px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-item {
  font-size: 12px;
  color: var(--faint);
  padding: 8px 10px;
  border-radius: 8px;
}

.side-item.on {
  color: var(--ink);
  background: rgba(225, 29, 72, 0.16);
}

.panel { padding: 16px; }

.mini-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}

.mini-stat {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-stat em {
  font-style: normal;
  font-size: 18px;
  letter-spacing: -0.03em;
}

.mini-stat span {
  font-size: 10px;
  color: var(--faint);
  letter-spacing: 0.08em;
}

.scan-box {
  position: relative;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.02);
}

.barcode {
  display: flex;
  align-items: stretch;
  height: 36px;
  gap: 2px;
  position: relative;
  margin-bottom: 8px;
}

.barcode i {
  display: block;
  height: 100%;
  background: var(--ink);
}

.scan-line {
  position: absolute;
  top: -8px;
  bottom: -8px;
  width: 2px;
  background: linear-gradient(180deg, transparent, #fb7185, transparent);
  box-shadow: 0 0 12px #fb7185;
  animation: scan 2.8s ease-in-out infinite;
}

.scan-meta { display: flex; flex-direction: column; gap: 2px; }
.code { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; color: var(--gold); }
.ok { font-size: 11px; color: #34d399; }

.rows { display: flex; flex-direction: column; gap: 6px; }
.row {
  display: grid;
  grid-template-columns: 1.4fr 0.7fr auto;
  gap: 8px;
  align-items: center;
  font-size: 11px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.row-name { color: var(--ink); }
.row-lot { color: var(--faint); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.row-tag { font-size: 10px; letter-spacing: 0.04em; }
.row-tag.warn { color: #fbbf24; }
.row-tag.ok { color: #34d399; }
.row-tag.info { color: #fb7185; }

.float-card {
  position: absolute;
  min-width: 168px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(18, 16, 20, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
  display: flex;
  flex-direction: column;
  gap: 4px;
  animation: float 7s ease-in-out infinite;
}

.float-card b { font-size: 13px; }
.float-card span { font-size: 12px; color: var(--muted); }
.fa { left: 10%; bottom: -18px; top: auto; z-index: 2; animation-delay: -2s; }
.fb { right: 6%; bottom: -18px; top: auto; z-index: 2; }

.marquee {
  border-block: 1px solid var(--line);
  overflow: hidden;
  padding: 16px 0;
  mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent);
}

.marquee-track {
  display: flex;
  width: max-content;
  gap: 40px;
  animation: marquee 28s linear infinite;
}

.marquee-track span {
  font-size: 13px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--faint);
  white-space: nowrap;
}

.marquee-track span::before {
  content: '✦';
  margin-right: 40px;
  color: var(--rose);
}

.section {
  padding: 112px 64px;
  scroll-margin-top: 72px;
}

.section-head { max-width: 720px; margin-bottom: 48px; }

.section-title {
  font-size: clamp(32px, 4vw, 48px);
  letter-spacing: -0.04em;
  line-height: 1.15;
  margin: 0 0 12px;
}

.section-sub {
  color: var(--muted);
  font-size: 16px;
  line-height: 1.7;
}

.bento {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 28px;
  min-height: 220px;
  transition: transform 0.35s ease, border-color 0.35s ease, background 0.35s ease;
}

.card:hover {
  transform: translateY(-6px);
  border-color: rgba(251, 113, 133, 0.35);
  background: rgba(255, 255, 255, 0.05);
}

.c-lg {
  grid-column: span 2;
  grid-row: span 2;
}

.card-kicker {
  color: var(--gold);
  font-size: 11px;
  letter-spacing: 0.22em;
  margin-bottom: 12px;
}

.card h3 {
  font-size: 22px;
  letter-spacing: -0.03em;
  margin-bottom: 10px;
  font-weight: 600;
}

.card p { color: var(--muted); line-height: 1.7; font-size: 14px; }

.lot-visual { margin-top: 28px; display: flex; flex-direction: column; gap: 14px; }
.lot-bar { display: grid; grid-template-columns: 72px 1fr; gap: 10px; align-items: center; }
.lot-bar span { font-size: 11px; color: var(--faint); font-family: ui-monospace, Menlo, monospace; }
.lot-bar i {
  display: block;
  height: 8px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.lot-bar em {
  display: block;
  height: 100%;
  width: 100%;
  background: linear-gradient(90deg, #e11d48, #e8c39a);
  transform-origin: left;
  animation: fill 1.6s ease forwards;
}

.mini-code { margin-top: 22px; }
.mini-bars { display: flex; gap: 3px; height: 28px; margin-bottom: 8px; }
.mini-bars i {
  flex: 1;
  background: var(--ink);
  opacity: 0.75;
  animation: pulseBar 1.8s ease-in-out infinite;
}
.mini-bars i:nth-child(odd) { opacity: 0.35; height: 70%; align-self: end; }
.mini-code span { font-size: 12px; color: var(--faint); letter-spacing: 0.12em; }

.loop {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: var(--gold);
}

.loop i {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--rose), transparent);
  position: relative;
  overflow: hidden;
}

.loop i::after {
  content: '';
  position: absolute;
  width: 18px;
  height: 1px;
  background: #fff;
  animation: dash 1.6s linear infinite;
}

.trace-section {
  background:
    radial-gradient(ellipse at 50% 0%, rgba(225, 29, 72, 0.12), transparent 55%);
}

.flow {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.flow-line {
  position: absolute;
  top: 22px;
  left: 8%;
  right: 8%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(251, 113, 133, 0.6), transparent);
}

.flow-pulse {
  position: absolute;
  top: -3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 16px #fb7185;
  animation: travel 4.2s linear infinite;
}

.flow-node {
  position: relative;
  padding: 48px 8px 0;
  text-align: center;
}

.flow-idx {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid rgba(251, 113, 133, 0.5);
  background: #07070a;
  font-size: 11px;
  display: grid;
  place-items: center;
  color: var(--gold);
}

.flow-node strong { display: block; margin-bottom: 8px; font-size: 16px; }
.flow-node em { color: var(--muted); font-style: normal; font-size: 13px; line-height: 1.6; }

.trust-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 48px;
}

.trust-num {
  display: block;
  font-size: 12px;
  letter-spacing: 0.28em;
  color: var(--gold);
  margin-bottom: 16px;
}

.trust-item h3 {
  font-size: 28px;
  letter-spacing: -0.03em;
  margin-bottom: 12px;
}

.trust-item p { color: var(--muted); line-height: 1.75; }

.cta-band {
  padding: 0 64px 112px;
}

.cta-inner {
  border-radius: 28px;
  padding: 72px 48px;
  text-align: center;
  border: 1px solid var(--line);
  background:
    radial-gradient(ellipse at 50% 0%, rgba(225, 29, 72, 0.22), transparent 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.015));
}

.cta-inner h2 {
  font-size: clamp(28px, 4vw, 44px);
  letter-spacing: -0.04em;
  margin: 8px 0 12px;
}

.cta-inner p { color: var(--muted); margin-bottom: 28px; }

.foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 28px 64px 40px;
  border-top: 1px solid var(--line);
  color: var(--faint);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.reveal {
  opacity: 0;
  transform: translateY(22px);
  transition: opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1), transform 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}

.reveal.d1 { transition-delay: 0.08s; }
.reveal.d2 { transition-delay: 0.16s; }
.reveal.d3 { transition-delay: 0.24s; }
.reveal.d4 { transition-delay: 0.32s; }
.reveal.d5 { transition-delay: 0.4s; }
.reveal.is-in,
.is-ready .hero .reveal {
  opacity: 1;
  transform: none;
}

@keyframes bootOut {
  to { opacity: 0; visibility: hidden; }
}

@keyframes load {
  from { transform: translateX(-120%); }
  to { transform: translateX(250%); }
}

@keyframes pop {
  from { transform: scale(0.7); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}

@keyframes twinkle {
  0%, 100% { opacity: 0.15; transform: scale(1); }
  50% { opacity: 0.9; transform: scale(1.5); }
}

@keyframes ping {
  0% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.55); }
  70% { box-shadow: 0 0 0 8px rgba(52, 211, 153, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0); }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(4px); }
}

@keyframes scan {
  0% { left: 0; }
  100% { left: 100%; }
}

@keyframes marquee {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@keyframes fill {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes pulseBar {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.55); }
}

@keyframes dash {
  from { left: -20%; }
  to { left: 120%; }
}

@keyframes travel {
  from { left: 0; }
  to { left: 100%; }
}

@media (max-width: 1100px) {
  .hero,
  .section,
  .cta-band,
  .foot,
  .nav { padding-left: 24px; padding-right: 24px; }

  .hero {
    grid-template-columns: 1fr;
    min-height: auto;
    padding-top: 28px;
  }

  .bento,
  .flow,
  .trust-grid { grid-template-columns: 1fr; }

  .c-lg { grid-row: auto; grid-column: auto; }
  .flow-line { display: none; }
  .fa, .fb { display: none; }
  .nav-links { display: none; }
  .device-body { grid-template-columns: 1fr; }
  .side { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .boot { display: none; }
  .reveal { opacity: 1; transform: none; transition: none; }
  .orb, .speck, .float-card, .scan-line, .marquee-track, .flow-pulse, .chev, .boot-track i {
    animation: none !important;
  }
}
</style>
