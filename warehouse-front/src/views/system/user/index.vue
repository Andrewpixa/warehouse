<template>
  <div class="page-container">
    <div class="page-header animate-fade-in-up">
      <h1 class="page-header-title">人员管理</h1>
      <p class="page-header-desc">点销售部列出一部、二部人员；总经理在总经办，不进销售细分。</p>
    </div>
    <el-row :gutter="16" class="org-layout">
      <el-col :span="6" class="org-left">
        <TreePanel ref="treeRef" title="组织架构" :load-api="loadDeptManagerLeftTreeJson" @node-click="handleNodeClick" />
      </el-col>
      <el-col :span="18">
        <el-card>
          <div v-if="currentDeptTitle" class="org-hint">{{ currentDeptTitle }}（含下级）</div>
          <SearchForm v-model="searchParams" @search="handleSearch" @reset="handleReset">
            <el-form-item label="用户名">
              <el-input v-model="searchParams.name" placeholder="姓名 / 登录名" clearable />
            </el-form-item>
            <el-form-item label="地址">
              <el-input v-model="searchParams.address" placeholder="地址" clearable />
            </el-form-item>
          </SearchForm>

          <CrudTable ref="tableRef" :load-api="loadAllUser" :search-params="searchParams">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="头像" width="60">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.imgpath ? getImageUrl(row.imgpath) : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="loginname" label="登录名" />
        <el-table-column prop="deptname" label="部门" />
        <el-table-column prop="remark" label="岗位" min-width="120" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" />
        <el-table-column label="性别" width="60">
          <template #default="{ row }">{{ row.sex === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column label="可用" width="60">
          <template #default="{ row }">
            <el-tag :type="row.available === 1 ? 'success' : 'danger'" size="small">
              {{ row.available === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ordernum" label="排序" width="60" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="success" link @click="handleAssignRole(row)">分配角色</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #toolbar>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加
          </el-button>
        </template>
      </CrudTable>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加/编辑弹窗 -->
    <CrudDialog ref="dialogRef" :submit-api="handleSubmitApi" :rules="userRules" width="600px" @success="tableRef?.reload()">
      <template #default="{ formData, isEdit: editMode }">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="formData.name" @blur="handleNameChange(formData)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录名" prop="loginname">
              <el-input v-model="formData.loginname" :disabled="editMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="部门" prop="deptid">
          <el-tree-select v-model="formData.deptid" :data="deptTree" :props="{ label: 'title', value: 'id', children: 'children' }" placeholder="必须选择启用中的部门" clearable filterable check-strictly />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="formData.sex">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="formData.ordernum" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item label="岗位">
          <el-input v-model="formData.remark" placeholder="如 销售一部经理 / 销售代表" />
        </el-form-item>
        <el-form-item label="是否可用">
          <el-radio-group v-model="formData.available">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="头像">
          <ImageUpload v-model="formData.imgpath" />
        </el-form-item>
      </template>
    </CrudDialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="600px">
      <el-table :data="allRoles" @selection-change="handleRoleSelectionChange" ref="roleTableRef">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="角色名" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUserRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import CrudTable from '@/components/CrudTable.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import TreePanel from '@/components/TreePanel.vue'
import { loadAllUser, addUser, updateUser, deleteUser, resetPwd, changeChineseToPinyin, initRoleByUserId, saveUserRole } from '@/api/user'
import { loadDeptManagerLeftTreeJson } from '@/api/dept'
import { getImageUrl } from '@/api/file'

const tableRef = ref()
const treeRef = ref()
const route = useRoute()
const dialogRef = ref()
const roleTableRef = ref()
const isEdit = ref(false)
const deptTree = ref<any[]>([])
const allRoles = ref<any[]>([])
const roleDialogVisible = ref(false)
const currentUserId = ref(0)
const selectedRoles = ref<any[]>([])
const currentDeptTitle = ref('全公司')

const searchParams = reactive({ name: '', address: '', deptid: null as number | null })

const userRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  loginname: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  deptid: [{ required: true, message: '用户必须归属启用中的部门', trigger: 'change' }]
}

const handleSearch = () => tableRef.value?.reload()
const handleReset = () => {
  searchParams.name = ''
  searchParams.address = ''
  searchParams.deptid = null
  currentDeptTitle.value = '全公司'
  tableRef.value?.reload()
}
const handleNodeClick = (data: any) => {
  searchParams.deptid = data.id
  currentDeptTitle.value = data.title || data.name || '当前部门'
  tableRef.value?.reload()
}
const handleAdd = () => {
  isEdit.value = false
  dialogRef.value?.open({ sex: 1, available: 1, ordernum: 1, imgpath: '', deptid: searchParams.deptid }, false)
}
const handleEdit = (row: any) => { isEdit.value = true; dialogRef.value?.open(row, true) }
const handleSubmitApi = (data: any) => isEdit.value ? updateUser(data) : addUser(data)

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  await deleteUser(row.id)
  tableRef.value?.reload()
}

const handleResetPwd = async (row: any) => {
  await ElMessageBox.confirm(`确认重置 ${row.name} 的密码为123456？`, '提示', { type: 'warning' })
  const res: any = await resetPwd(row.id)
  if (res.code === 200) ElMessage.success('重置成功')
}

const handleNameChange = async (formData: any) => {
  if (formData.name && !formData.loginname) {
    try {
      const res: any = await changeChineseToPinyin(formData.name)
      if (res.value) formData.loginname = res.value
    } catch {}
  }
}

const handleAssignRole = async (row: any) => {
  currentUserId.value = row.id
  try {
    const res: any = await initRoleByUserId(row.id)
    allRoles.value = res.data || []
    // 设置已选中
    roleDialogVisible.value = true
    setTimeout(() => {
      allRoles.value.forEach((r: any) => {
        if (r.LAY_CHECKED === true) {
          roleTableRef.value?.toggleRowSelection(r, true)
        }
      })
    }, 100)
  } catch {}
}

const handleRoleSelectionChange = (rows: any[]) => {
  selectedRoles.value = rows
}

const handleSaveUserRole = async () => {
  const ids = selectedRoles.value.map((r: any) => r.id)
  const res: any = await saveUserRole(currentUserId.value, ids)
  if (res.code === 200) {
    ElMessage.success('分配成功')
    roleDialogVisible.value = false
  }
}

onMounted(async () => {
  try {
    const res: any = await loadDeptManagerLeftTreeJson()
    deptTree.value = res.data || []
  } catch {}
  if (route.query.deptid) {
    searchParams.deptid = Number(route.query.deptid)
    currentDeptTitle.value = '指定部门'
    tableRef.value?.reload()
  }
})
</script>

<style scoped>
.org-layout { min-height: calc(100vh - 170px); }
.org-left { height: 100%; }
.org-hint { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.page-container :deep(.el-card) {
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.page-container :deep(.el-card:hover) {
  box-shadow: var(--shadow-md);
}
</style>
