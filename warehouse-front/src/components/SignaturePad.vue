<template>
  <div class="sign-pad">
    <canvas
      ref="canvasRef"
      width="520"
      height="160"
      @pointerdown="start"
      @pointermove="move"
      @pointerup="end"
      @pointerleave="end"
    />
    <div class="sign-actions">
      <el-button size="small" @click="clear">清除</el-button>
      <el-button size="small" type="primary" :disabled="!dirty" @click="submit">使用此签字</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

const emit = defineEmits<{
  (e: 'signed', path: string): void
}>()

const canvasRef = ref<HTMLCanvasElement>()
const drawing = ref(false)
const dirty = ref(false)

const ctx = () => canvasRef.value?.getContext('2d')

const setup = () => {
  const c = canvasRef.value
  const g = ctx()
  if (!c || !g) return
  g.fillStyle = '#fff'
  g.fillRect(0, 0, c.width, c.height)
  g.strokeStyle = '#111'
  g.lineWidth = 2
  g.lineCap = 'round'
  g.lineJoin = 'round'
}

onMounted(setup)

const pos = (e: PointerEvent) => {
  const c = canvasRef.value!
  const r = c.getBoundingClientRect()
  return {
    x: (e.clientX - r.left) * (c.width / r.width),
    y: (e.clientY - r.top) * (c.height / r.height)
  }
}

const start = (e: PointerEvent) => {
  const g = ctx()
  if (!g) return
  canvasRef.value?.setPointerCapture(e.pointerId)
  drawing.value = true
  const p = pos(e)
  g.beginPath()
  g.moveTo(p.x, p.y)
}

const move = (e: PointerEvent) => {
  if (!drawing.value) return
  const g = ctx()
  if (!g) return
  const p = pos(e)
  g.lineTo(p.x, p.y)
  g.stroke()
  dirty.value = true
}

const end = () => {
  drawing.value = false
}

const clear = () => {
  dirty.value = false
  setup()
}

const submit = () => {
  const c = canvasRef.value
  if (!c || !dirty.value) return
  c.toBlob(async (blob) => {
    if (!blob) {
      ElMessage.error('签字生成失败')
      return
    }
    const file = new File([blob], 'signature.png', { type: 'image/png' })
    const res: any = await uploadFile(file)
    if (res?.path) {
      emit('signed', res.path)
    } else {
      ElMessage.error(res?.msg || '签字上传失败')
    }
  }, 'image/png')
}
</script>

<style scoped>
.sign-pad canvas {
  width: 100%;
  height: 160px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  background: #fff;
  touch-action: none;
  cursor: crosshair;
}
.sign-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
</style>
