<template>
  <div class="voucher-panel">
    <el-alert
      v-if="!bizId"
      type="info"
      :closable="false"
      title="请先保存草稿，再上传发票/随货同行单并完成签字。"
      style="margin-bottom: 12px"
    />
    <template v-else>
      <div class="voucher-block" v-for="group in attachTypes" :key="group.type">
        <div class="voucher-title">{{ group.label }} <span class="req">*</span></div>
        <div class="file-list">
          <a
            v-for="item in filesOf(group.type)"
            :key="item.id"
            class="file-card"
            :href="fileUrl(item.filePath)"
            target="_blank"
            rel="noopener"
          >
            <img v-if="isImage(item.filePath)" :src="fileUrl(item.filePath)" alt="" />
            <span v-else>{{ item.fileName || '附件' }}</span>
            <el-button
              v-if="!readonly"
              class="file-del"
              type="danger"
              link
              @click.prevent="removeAttach(item.id)"
            >删除</el-button>
          </a>
          <el-upload
            v-if="!readonly"
            :action="uploadUrl"
            name="mf"
            :show-file-list="false"
            :on-success="(res: any, file: any) => onUpload(group.type, res, file)"
            :before-upload="beforeUpload"
            :with-credentials="true"
            accept=".pdf,.html,image/*"
          >
            <el-button type="primary" plain>上传 {{ group.label }}</el-button>
          </el-upload>
        </div>
      </div>

      <div class="voucher-block" v-for="role in signRoles" :key="role.role">
        <div class="voucher-title">{{ role.label }} <span class="req">*</span></div>
        <div v-if="signOf(role.role)" class="sign-preview">
          <img :src="fileUrl(signOf(role.role)!.imagePath)" alt="签字" />
          <div class="sign-meta">
            <div>签字人：{{ signOf(role.role)!.signerName }}</div>
            <div>方式：{{ signOf(role.role)!.signSource === 'pad' ? '手写板' : '拍照' }}</div>
          </div>
          <el-button v-if="!readonly" type="danger" link @click="removeSign(signOf(role.role)!.id)">重签</el-button>
        </div>
        <div v-else-if="!readonly" class="sign-editor">
          <el-input v-model="signerNames[role.role]" placeholder="签字人姓名" style="max-width: 220px; margin-bottom: 8px" />
          <el-tabs v-model="signTabs[role.role]">
            <el-tab-pane label="手写板" name="pad">
              <SignaturePad @signed="(path) => onPad(role.role, path)" />
            </el-tab-pane>
            <el-tab-pane label="拍照 / 上传" name="photo">
              <el-upload
                :action="uploadUrl"
                name="mf"
                :show-file-list="false"
                :on-success="(res: any) => onPhoto(role.role, res)"
                :before-upload="beforeImage"
                :with-credentials="true"
                accept="image/*"
              >
                <el-button type="primary" plain>上传签字照片</el-button>
              </el-upload>
            </el-tab-pane>
          </el-tabs>
        </div>
        <el-text v-else type="danger">尚未签字</el-text>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import SignaturePad from '@/components/SignaturePad.vue'
import { deleteAttachment, deleteSignature, loadVoucher, saveAttachment, saveSignature } from '@/api/voucher'
import { getImageUrl } from '@/api/file'
import { BASE_URL } from '@/utils/request'

const props = withDefaults(defineProps<{
  bizType: string
  bizId?: number
  readonly?: boolean
  attachTypes?: { type: string; label: string }[]
  signRoles: { role: string; label: string }[]
}>(), {
  readonly: false,
  attachTypes: () => ([
    { type: 'invoice', label: '发票 / 电子发票' },
    { type: 'packing', label: '随货同行单' }
  ])
})

const uploadUrl = BASE_URL + '/file/uploadFile'
const attachments = ref<any[]>([])
const signatures = ref<any[]>([])
const signerNames = reactive<Record<string, string>>({})
const signTabs = reactive<Record<string, string>>({})

const fileUrl = (path: string) => getImageUrl(path)
const isImage = (path: string) => /\.(png|jpe?g|gif|bmp|webp)(_temp)?$/i.test(path || '')
const filesOf = (type: string) => attachments.value.filter((x) => x.attachType === type)
const signOf = (role: string) => signatures.value.find((x) => x.signRole === role)

const load = async () => {
  if (!props.bizId) {
    attachments.value = []
    signatures.value = []
    return
  }
  const res: any = await loadVoucher(props.bizType, props.bizId)
  attachments.value = res.data?.attachments || []
  signatures.value = res.data?.signatures || []
}

watch(() => props.bizId, load)
onMounted(() => {
  props.signRoles.forEach((r) => {
    if (!signTabs[r.role]) signTabs[r.role] = 'pad'
  })
  load()
})

const beforeUpload = (file: File) => {
  const ok = file.type.startsWith('image/') || file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf') || file.name.toLowerCase().endsWith('.html')
  if (!ok) {
    ElMessage.error('请上传 PDF 或图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('文件不能超过 5MB')
    return false
  }
  return true
}

const beforeImage = (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

const onUpload = async (attachType: string, res: any, file: any) => {
  if (!res?.path || !props.bizId) {
    ElMessage.error(res?.msg || '上传失败')
    return
  }
  await saveAttachment({
    bizType: props.bizType,
    bizId: props.bizId,
    attachType,
    filePath: res.path,
    fileName: file?.name
  })
  ElMessage.success('附件已保存')
  await load()
}

const removeAttach = async (id: number) => {
  await deleteAttachment(id)
  await load()
}

const persistSign = async (role: string, source: string, path: string) => {
  if (!props.bizId) return
  const name = (signerNames[role] || '').trim()
  if (!name) {
    ElMessage.warning('请先填写签字人姓名')
    return
  }
  await saveSignature({
    bizType: props.bizType,
    bizId: props.bizId,
    signRole: role,
    signerName: name,
    signSource: source,
    imagePath: path
  })
  ElMessage.success('签字已保存')
  await load()
}

const onPad = (role: string, path: string) => persistSign(role, 'pad', path)
const onPhoto = (role: string, res: any) => {
  if (!res?.path) {
    ElMessage.error(res?.msg || '上传失败')
    return
  }
  persistSign(role, 'photo', res.path)
}

const removeSign = async (id: number) => {
  await deleteSignature(id)
  await load()
}

defineExpose({ reload: load })
</script>

<style scoped>
.voucher-panel {
  margin-top: 16px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-color);
}
.voucher-block {
  margin-bottom: 16px;
}
.voucher-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.req { color: var(--el-color-danger); }
.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.file-card {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 120px;
  min-height: 80px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  color: inherit;
  text-decoration: none;
  background: var(--bg-tertiary);
  padding: 6px;
  font-size: 12px;
}
.file-card img {
  width: 100%;
  height: 72px;
  object-fit: cover;
}
.file-del { margin-top: 4px; }
.sign-preview {
  display: flex;
  gap: 12px;
  align-items: center;
}
.sign-preview img {
  width: 220px;
  height: 80px;
  object-fit: contain;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}
.sign-meta { font-size: 13px; color: var(--text-secondary); }
</style>
