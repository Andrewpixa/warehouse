<template>
  <div class="intro" :class="{ ready }">
    <div v-if="booting" class="boot" aria-hidden="true">
      <div class="boot-ring" />
      <p>内勤查询台</p>
    </div>

    <header class="nav">
      <div class="brand">
        <span class="mark">票</span>
        <div>
          <strong>药企进销存 · 桌面端</strong>
          <small>财务 / 销售内勤专用</small>
        </div>
      </div>
      <button class="cta" type="button" @click="enter">进入工作台</button>
    </header>

    <main>
      <section class="hero">
        <div class="hero-bg" aria-hidden="true">
          <div class="grid" />
          <div class="orb a" />
          <div class="orb b" />
        </div>
        <div class="hero-copy">
          <p class="eyebrow reveal">
            <i class="dot" />
            给坐在电脑前对账的人
          </p>
          <h1 class="reveal d1">这一票，<em>回了没有</em></h1>
          <p class="lead reveal d2">
            本程序不是仓库捡货，也不是全量开单。它给财务和销售内勤：用发票号查明细、对着未回款催、核对批号库存、查看红冲记录。
          </p>
          <div class="who reveal d3">
            <span class="who-on">财务人员</span>
            <span class="who-on">销售内勤</span>
            <span class="who-off">仓管作业请走网页 / 手机</span>
          </div>
          <div class="actions reveal d4">
            <button class="cta solid" type="button" @click="enter">登录查询</button>
            <button class="ghost" type="button" @click="scrollTo('jobs')">本岗能做什么</button>
          </div>
        </div>

        <div class="stage reveal d2">
          <div class="ticket">
            <div class="ticket-bar">
              <span>发票查询 · LIVE</span>
              <em>10s</em>
            </div>
            <div class="ticket-no">
              <span v-for="(ch, i) in invoiceDigits" :key="i" class="digit" :style="{ animationDelay: i * 40 + 'ms' }">{{ ch }}</span>
            </div>
            <div class="scan"><i /></div>
            <dl>
              <div><dt>客户</dt><dd>市一医院</dd></div>
              <div><dt>出库金额</dt><dd>¥ 86,420.00</dd></div>
              <div><dt>已回</dt><dd class="warn">¥ 30,000.00</dd></div>
            </dl>
            <div class="pay">
              <span>部分回款</span>
              <b>{{ paidPct }}%</b>
            </div>
            <div class="bar"><i :style="{ width: paidPct + '%' }" /></div>
          </div>
          <aside class="float f1">
            <b>未回 12 票</b>
            <span>按客户筛，点开就能催</span>
          </aside>
          <aside class="float f2">
            <b>红冲只读</b>
            <span>开红冲仍在网页端</span>
          </aside>
        </div>
      </section>

      <section id="jobs" class="jobs">
        <p class="eyebrow reveal">THIS DESK</p>
        <h2 class="reveal d1">四件事，覆盖你每天的对账</h2>
        <div class="cards">
          <article v-for="(job, i) in jobs" :key="job.k" class="card reveal" :style="{ animationDelay: 120 + i * 80 + 'ms' }">
            <p class="kicker">{{ job.k }}</p>
            <h3>{{ job.t }}</h3>
            <p>{{ job.d }}</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const emit = defineEmits<{ enter: [] }>()

const booting = ref(true)
const ready = ref(false)
const paidPct = ref(0)
const invoiceDigits = '26957000000121085238'.split('')

const jobs = [
  {
    k: '01 · 发票号',
    t: '10 秒判断这一票',
    d: '支持 20 位全电号、后 10 位顺序号或出库单号。打开就能看到金额、已回、状态、明细和追溯码。'
  },
  {
    k: '02 · 未回款',
    t: '对着列表催，不翻本子',
    d: '只列已确认、尚未收齐的正常出库。按客户、发票号筛选，点一行进入该票明细。'
  },
  {
    k: '03 · 批号库存',
    t: '核对用，不是改库存',
    d: '按品种、批号、仓库看数量、质量状态和效期。对账发现对不上时，先在这里核对实物账。'
  },
  {
    k: '04 · 红冲记录',
    t: '查关联冲减，不开单',
    d: '输入原蓝字发票号，查看红冲单、冲减数量和日期。开红冲、确认加回库存请到网页端。'
  }
]

let bootTimer: number | undefined
let pctTimer: number | undefined

const enter = () => emit('enter')

const scrollTo = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(() => {
  bootTimer = window.setTimeout(() => {
    booting.value = false
    ready.value = true
    let n = 0
    pctTimer = window.setInterval(() => {
      n += 3
      paidPct.value = Math.min(35, n)
      if (n >= 35 && pctTimer) window.clearInterval(pctTimer)
    }, 40)
  }, 720)
})

onUnmounted(() => {
  if (bootTimer) window.clearTimeout(bootTimer)
  if (pctTimer) window.clearInterval(pctTimer)
})
</script>

<style scoped>
.intro {
  min-height: 100%;
  color: #e8eef6;
  background: #071018;
  overflow: auto;
}
.boot {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #050b10;
  animation: fadeOut 0.45s ease 0.55s forwards;
}
.boot p {
  margin: 0;
  letter-spacing: 0.28em;
  font-size: 12px;
  color: #8fb4d4;
}
.boot-ring {
  width: 48px;
  height: 48px;
  border: 1px solid rgba(110, 184, 255, 0.25);
  border-top-color: #7ec8ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.nav {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  backdrop-filter: blur(16px);
  background: rgba(7, 16, 24, 0.72);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.brand {
  display: flex;
  gap: 12px;
  align-items: center;
}
.mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3aa0e8, #0c5f96);
  font-weight: 700;
}
.brand strong {
  display: block;
  font-size: 14px;
}
.brand small {
  color: #8aa3b8;
}
.cta {
  border: 0;
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
  color: #0b1a26;
  background: #d7ecff;
  font-weight: 600;
}
.cta.solid {
  padding: 12px 22px;
  font-size: 15px;
}
.ghost {
  margin-left: 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: transparent;
  color: #d5e6f5;
  border-radius: 999px;
  padding: 12px 18px;
  cursor: pointer;
}
.hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 40px;
  padding: 48px 48px 24px;
  min-height: 520px;
  align-items: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(126, 200, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(126, 200, 255, 0.05) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(circle at 30% 30%, #000 20%, transparent 70%);
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.45;
  animation: drift 8s ease-in-out infinite;
}
.orb.a {
  width: 280px;
  height: 280px;
  background: #1b6ca8;
  top: -40px;
  right: 8%;
}
.orb.b {
  width: 220px;
  height: 220px;
  background: #0f3d5c;
  bottom: 10%;
  left: 40%;
  animation-delay: -3s;
}
.eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8ec7ef;
  letter-spacing: 0.08em;
  font-size: 12px;
  text-transform: uppercase;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 12px #4ade80;
  animation: pulse 1.6s ease-in-out infinite;
}
h1 {
  margin: 12px 0 16px;
  font-size: 44px;
  line-height: 1.15;
  font-weight: 650;
}
h1 em {
  font-style: normal;
  background: linear-gradient(90deg, #9ad7ff, #f4fbff 55%, #6ee7b7);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.lead {
  max-width: 520px;
  color: #b7c9d8;
  line-height: 1.7;
  font-size: 15px;
}
.who {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 22px 0;
}
.who-on,
.who-off {
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
}
.who-on {
  background: rgba(74, 222, 128, 0.12);
  color: #86efac;
  border: 1px solid rgba(74, 222, 128, 0.28);
}
.who-off {
  background: rgba(255, 255, 255, 0.04);
  color: #8aa3b8;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.stage {
  position: relative;
  min-height: 360px;
}
.ticket {
  position: relative;
  margin: 24px 24px 0 40px;
  padding: 22px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(18, 36, 52, 0.92), rgba(10, 20, 30, 0.92));
  border: 1px solid rgba(158, 206, 240, 0.18);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
  overflow: hidden;
}
.ticket-bar {
  display: flex;
  justify-content: space-between;
  color: #8fb4d4;
  font-size: 12px;
  margin-bottom: 12px;
}
.ticket-bar em {
  font-style: normal;
  color: #86efac;
}
.ticket-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 18px;
  letter-spacing: 0.08em;
}
.digit {
  display: inline-block;
  opacity: 0;
  animation: pop 0.4s ease forwards;
}
.scan {
  height: 2px;
  margin: 14px 0 18px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.scan i {
  display: block;
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, transparent, #7dd3fc, transparent);
  animation: scan 2.2s ease-in-out infinite;
}
dl {
  display: grid;
  gap: 8px;
  margin: 0 0 16px;
}
dl div {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
dt {
  color: #8aa3b8;
}
dd {
  margin: 0;
}
.warn {
  color: #fbbf24;
}
.pay {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}
.bar {
  height: 6px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #fbbf24, #86efac);
  transition: width 0.2s linear;
}
.float {
  position: absolute;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(12, 24, 36, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  animation: float 4.5s ease-in-out infinite;
}
.float b {
  display: block;
  font-size: 13px;
}
.float span {
  color: #9bb3c6;
  font-size: 12px;
}
.f1 {
  top: 8px;
  left: 0;
}
.f2 {
  right: 0;
  bottom: 28px;
  animation-delay: -1.6s;
}
.jobs {
  padding: 24px 48px 64px;
}
.jobs h2 {
  margin: 8px 0 28px;
  font-size: 28px;
  font-weight: 600;
}
.cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.card {
  padding: 22px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.kicker {
  margin: 0 0 8px;
  color: #7dd3fc;
  font-size: 12px;
  letter-spacing: 0.06em;
}
.card h3 {
  margin: 0 0 8px;
  font-size: 18px;
}
.card p:last-child {
  margin: 0;
  color: #adc0d0;
  line-height: 1.65;
  font-size: 14px;
}
.ready .reveal {
  animation: rise 0.7s ease both;
}
.d1 { animation-delay: 0.08s; }
.d2 { animation-delay: 0.16s; }
.d3 { animation-delay: 0.24s; }
.d4 { animation-delay: 0.32s; }

@keyframes spin { to { transform: rotate(360deg); } }
@keyframes fadeOut { to { opacity: 0; visibility: hidden; } }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
@keyframes drift {
  50% { transform: translateY(18px) scale(1.06); }
}
@keyframes pop {
  to { opacity: 1; transform: none; }
  from { opacity: 0; transform: translateY(6px); }
}
@keyframes scan {
  0% { transform: translateX(-120%); }
  100% { transform: translateX(280%); }
}
@keyframes float {
  50% { transform: translateY(-8px); }
}
@keyframes rise {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: none; }
}

@media (max-width: 980px) {
  .hero, .cards { grid-template-columns: 1fr; }
  .hero { padding: 32px 24px; }
  .jobs { padding: 12px 24px 48px; }
  h1 { font-size: 34px; }
}
</style>
