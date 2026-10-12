<template>
  <div class="download-page">
    <header class="nav">
      <router-link class="brand" to="/welcome">
        <span class="logo" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <path d="M20 7L12 3L4 7V17L12 21L20 17V7Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M12 12L20 7M12 12V21M12 12L4 7" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
          </svg>
        </span>
        <span>药企进销存</span>
      </router-link>
      <nav class="nav-links">
        <a href="#pc">PC 端</a>
        <a href="#android">安卓</a>
        <router-link to="/design">课程设计</router-link>
      </nav>
      <router-link class="nav-cta" to="/login">登录</router-link>
    </header>

    <main>
      <section class="hero">
        <div>
          <p class="eyebrow">CLIENT  ·  1.0.0</p>
          <h1>下载客户端</h1>
          <p class="lead">PC 端和安卓包分开。桌面端打开后就是线上同一套进销存；安卓包给现场查批号、扫追溯码。</p>
        </div>
        <aside class="recommend" v-if="recommended">
          <p class="recommend-kicker">当前设备</p>
          <p class="recommend-name">{{ recommended.name }}</p>
          <p class="recommend-meta">{{ recommended.size }} · {{ recommended.arch }} · {{ recommended.updated }}</p>
          <a class="btn" :href="recommended.href" :download="recommended.file">下载 {{ recommended.short }}</a>
        </aside>
      </section>

      <section id="pc" class="block">
        <div class="block-head">
          <div>
            <p class="index">01</p>
            <h2>PC 端</h2>
          </div>
          <p>Electron 桌面窗口。解压即用，登录的是 <span class="host">yaoheng.cloud</span>。</p>
        </div>

        <div class="switch" role="tablist" aria-label="选择电脑系统">
          <button
            v-for="item in orderedPc"
            :key="item.id"
            type="button"
            role="tab"
            :aria-selected="pcId === item.id"
            :class="{ on: pcId === item.id }"
            @click="pcId = item.id"
          >
            <span>{{ item.platform }}</span>
            <small>{{ item.id === detectedDesktop ? '本机' : item.arch }}</small>
          </button>
        </div>

        <article class="sheet">
          <div class="sheet-main">
            <p class="file">{{ activePc.file }}</p>
            <h3>{{ activePc.name }}</h3>
            <dl>
              <div><dt>体积</dt><dd>{{ activePc.size }}</dd></div>
              <div><dt>架构</dt><dd>{{ activePc.arch }}</dd></div>
              <div><dt>格式</dt><dd>{{ activePc.format }}</dd></div>
              <div><dt>日期</dt><dd>{{ activePc.updated }}</dd></div>
            </dl>
            <a class="btn" :href="activePc.href" :download="activePc.file">下载 {{ activePc.platform }} 版</a>
          </div>
          <ol class="steps">
            <li v-for="(step, i) in activePc.steps" :key="step">
              <span>0{{ i + 1 }}</span>
              <p>{{ step }}</p>
            </li>
          </ol>
        </article>
      </section>

      <section id="android" class="block">
        <div class="block-head">
          <div>
            <p class="index">02</p>
            <h2>安卓</h2>
          </div>
          <p>手机安装包。iOS 与鸿蒙没有此版本。</p>
        </div>

        <article class="sheet android">
          <div class="sheet-main">
            <p class="file">{{ android.file }}</p>
            <h3>{{ android.name }}</h3>
            <dl>
              <div><dt>体积</dt><dd>{{ android.size }}</dd></div>
              <div><dt>系统</dt><dd>Android</dd></div>
              <div><dt>格式</dt><dd>APK</dd></div>
              <div><dt>日期</dt><dd>{{ android.updated }}</dd></div>
            </dl>
            <a class="btn" :href="android.href" :download="android.file">下载安卓包</a>
          </div>
          <ol class="steps">
            <li v-for="(step, i) in android.steps" :key="step">
              <span>0{{ i + 1 }}</span>
              <p>{{ step }}</p>
            </li>
          </ol>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

type Pack = {
  id: string
  platform: string
  short: string
  name: string
  size: string
  arch: string
  format: string
  updated: string
  file: string
  href: string
  steps: string[]
}

const releaseBase = 'https://github.com/Andrewpixa/warehouse/releases/download/client-1.0.0'

const pcItems: Pack[] = [
  {
    id: 'windows',
    platform: 'Windows',
    short: 'Windows 版',
    name: 'Windows 64 位',
    size: '106 MB',
    arch: 'x64',
    format: 'ZIP',
    updated: '2026-10-06',
    file: 'pharma-pc-windows-x64.zip',
    href: `${releaseBase}/pharma-pc-windows-x64.zip`,
    steps: [
      '下载 zip 到电脑。',
      '解压整个文件夹，不要只抽出 exe。',
      '运行「药企进销存.exe」，用岗位账号登录。'
    ]
  },
  {
    id: 'mac',
    platform: 'macOS',
    short: 'macOS 版',
    name: 'macOS 64 位',
    size: '95 MB',
    arch: 'x64',
    format: 'ZIP',
    updated: '2026-10-06',
    file: 'pharma-pc-macos-x64.zip',
    href: `${releaseBase}/pharma-pc-macos-x64.zip`,
    steps: [
      '下载 zip 并解压。',
      '打开「药企进销存」。',
      '若系统拦截，到「隐私与安全性」里选择仍要打开。'
    ]
  }
]

const android: Pack = {
  id: 'android',
  platform: 'Android',
  short: '安卓包',
  name: '安卓安装包',
  size: '65 MB',
  arch: 'APK',
  format: 'APK',
  updated: '2026-10-06',
  file: 'pharma-android.apk',
  href: `${releaseBase}/pharma-android.apk`,
  steps: [
    '把 apk 传到安卓手机。',
    '安装时若提示未知来源，允许这一次。',
    '打开后登录，查批号库存或扫追溯码。'
  ]
}

function detectId() {
  const nav = navigator as Navigator & { userAgentData?: { platform?: string } }
  const hints = `${nav.userAgentData?.platform || ''} ${navigator.platform || ''} ${navigator.userAgent || ''}`
  if (/Android/i.test(hints)) return 'android'
  if (/Win/i.test(hints)) return 'windows'
  if (/Mac/i.test(hints)) return 'mac'
  return 'windows'
}

const detectedId = detectId()
const detectedDesktop = detectedId === 'mac' ? 'mac' : 'windows'
const pcId = ref(detectedDesktop)
const orderedPc = computed(() => {
  const current = pcItems.find((item) => item.id === detectedDesktop)
  return current ? [current, ...pcItems.filter((item) => item.id !== detectedDesktop)] : pcItems
})
const activePc = computed(() => pcItems.find((item) => item.id === pcId.value) || pcItems[0])
const recommended = detectedId === 'android'
  ? android
  : pcItems.find((item) => item.id === detectedDesktop) || pcItems[0]
</script>

<style scoped>
.download-page {
  --ink: #f6f1e8;
  --muted: rgba(246, 241, 232, 0.62);
  --faint: rgba(246, 241, 232, 0.38);
  --line: rgba(246, 241, 232, 0.12);
  --gold: #e8c39a;
  --bg: #07070a;
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
}

.nav {
  position: sticky;
  top: 0;
  z-index: 5;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: rgba(7, 7, 10, 0.78);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(18px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #e11d48, #fb7185);
}

.logo svg { width: 16px; height: 16px; }

.nav-links {
  display: flex;
  gap: 28px;
}

.nav-links a {
  color: var(--muted);
  text-decoration: none;
  font-size: 13px;
  letter-spacing: 0.12em;
}

.nav-links a:hover { color: var(--ink); }

.nav-cta {
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  color: #fff;
  font-size: 13px;
  letter-spacing: 0.12em;
  background: linear-gradient(135deg, #e11d48, #fb7185);
}

main {
  width: min(1120px, calc(100% - 64px));
  margin: 0 auto;
  padding: 56px 0 96px;
}

.hero {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 32px;
  align-items: end;
  padding-bottom: 48px;
  border-bottom: 1px solid var(--line);
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--gold);
  letter-spacing: 0.18em;
  font-size: 12px;
}

h1 {
  margin: 0;
  font-size: 56px;
  line-height: 1;
  font-weight: 620;
  letter-spacing: -0.04em;
}

.lead {
  max-width: 520px;
  margin: 18px 0 0;
  color: var(--muted);
  line-height: 1.75;
}

.recommend {
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 22px;
  background: linear-gradient(180deg, rgba(225, 29, 72, 0.14), rgba(255, 255, 255, 0.02));
}

.recommend-kicker {
  margin: 0;
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 0.16em;
}

.recommend-name {
  margin: 10px 0 0;
  font-size: 28px;
  letter-spacing: -0.03em;
}

.recommend-meta {
  margin: 8px 0 18px;
  color: var(--muted);
  font-size: 13px;
}

.block { padding-top: 48px; }

.block-head {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
  margin-bottom: 20px;
}

.index {
  margin: 0 0 6px;
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 0.16em;
}

h2 {
  margin: 0;
  font-size: 32px;
  letter-spacing: -0.03em;
}

.block-head p {
  margin: 0;
  max-width: 420px;
  text-align: right;
  color: var(--muted);
  line-height: 1.6;
}

.host { color: var(--ink); }

.switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.switch button {
  height: 64px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--muted);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 0 18px;
  cursor: pointer;
}

.switch button span {
  font-size: 16px;
  color: var(--ink);
}

.switch button small {
  margin-top: 2px;
  letter-spacing: 0.08em;
}

.switch button.on {
  border-color: rgba(251, 113, 133, 0.85);
  background: rgba(225, 29, 72, 0.16);
  box-shadow: inset 0 0 0 1px rgba(251, 113, 133, 0.35);
}

.sheet {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  border: 1px solid var(--line);
  border-radius: 18px;
  overflow: hidden;
}

.sheet-main { padding: 28px; }

.file {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--faint);
}

h3 {
  margin: 10px 0 18px;
  font-size: 28px;
  letter-spacing: -0.03em;
}

dl {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
  margin: 0 0 22px;
}

dt {
  color: var(--faint);
  font-size: 12px;
  letter-spacing: 0.12em;
}

dd {
  margin: 4px 0 0;
  font-size: 15px;
}

.btn {
  height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #e11d48, #fb7185);
  box-shadow: 0 8px 28px rgba(225, 29, 72, 0.28);
}

.steps {
  margin: 0;
  padding: 8px 8px 8px 0;
  list-style: none;
  background: rgba(255, 255, 255, 0.025);
  border-left: 1px solid var(--line);
}

.steps li {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 8px;
  padding: 18px 22px;
}

.steps li + li { border-top: 1px solid var(--line); }

.steps span {
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 0.08em;
  padding-top: 3px;
}

.steps p {
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .nav-links { display: none; }
  main { width: min(100% - 40px, 1120px); }
  .hero,
  .sheet,
  .block-head { grid-template-columns: 1fr; display: grid; }
  .block-head p { text-align: left; }
  h1 { font-size: 40px; }
  .steps { border-left: 0; border-top: 1px solid var(--line); }
}
</style>
