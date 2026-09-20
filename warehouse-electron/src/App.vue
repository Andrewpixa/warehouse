<template>
  <Intro v-if="!token && gate === 'intro'" @enter="gate = 'login'" />
  <Login v-else-if="!token" @success="onLogin" @back="gate = 'intro'" />
  <Workbench v-else :user-name="userName" :permissions="permissions" @logout="onLogout" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Intro from './views/Intro.vue'
import Login from './views/Login.vue'
import Workbench from './views/Workbench.vue'
import { TOKEN_KEY, USER_KEY } from './utils/request'
import { readPermissions, savePermissions } from './utils/perm'
import { currentUser } from './api/login'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const userName = ref(localStorage.getItem(USER_KEY) || '员工')
const permissions = ref<string[]>(readPermissions())
const gate = ref<'intro' | 'login'>('intro')

const onLogin = (payload: { token: string; name: string; permissions: string[] }) => {
  token.value = payload.token
  userName.value = payload.name
  permissions.value = payload.permissions || []
}

const onLogout = () => {
  token.value = ''
  userName.value = '员工'
  permissions.value = []
  gate.value = 'intro'
}

onMounted(async () => {
  if (!token.value) return
  try {
    const res: any = await currentUser()
    if (res.code === 200) {
      userName.value = res.user?.name || userName.value
      permissions.value = res.permissions || []
      localStorage.setItem(USER_KEY, userName.value)
      savePermissions(permissions.value)
    }
  } catch {
    onLogout()
  }
})
</script>
