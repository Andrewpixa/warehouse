<template>
  <div class="mermaid-block">
    <p v-if="caption" class="mermaid-caption">{{ caption }}</p>
    <div ref="hostRef" class="mermaid-host" :class="{ 'is-empty': !svg }">
      <div v-if="!svg && !error" class="mermaid-wait">图正在绘制…</div>
      <p v-else-if="error" class="mermaid-error">{{ error }}</p>
      <div v-else class="mermaid-svg" v-html="svg" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { loadMermaid, renderMermaid } from '@/views/design/mermaid-loader'

const props = defineProps<{
  code: string
  caption?: string
}>()

const hostRef = ref<HTMLElement>()
const svg = ref('')
const error = ref('')

const draw = async () => {
  if (!props.code.trim()) return
  error.value = ''
  try {
    await loadMermaid()
    svg.value = await renderMermaid(props.code)
    await nextTick()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '图绘制失败'
  }
}

onMounted(() => {
  void draw()
})

watch(() => props.code, () => {
  void draw()
})
</script>

<style scoped>
.mermaid-block {
  margin: 0 0 28px;
}

.mermaid-caption {
  margin: 0 0 10px;
  color: rgba(246, 241, 232, 0.56);
  font-size: 13px;
}

.mermaid-host {
  border: 1px solid rgba(246, 241, 232, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  padding: 18px;
  overflow: auto;
}

.mermaid-wait,
.mermaid-error {
  margin: 0;
  padding: 36px 12px;
  text-align: center;
  color: rgba(246, 241, 232, 0.45);
  font-size: 14px;
}

.mermaid-error { color: #fb7185; }

.mermaid-svg :deep(svg) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
}
</style>
