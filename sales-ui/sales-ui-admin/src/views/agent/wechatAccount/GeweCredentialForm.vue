<template>
  <Dialog v-model="dialogVisible" title="GeWe 凭证管理" width="980">
    <div class="credential-layout">
      <div class="credential-list">
        <div class="list-toolbar">
          <span class="list-title">凭证列表</span>
          <el-button plain type="primary" @click="createCredential">
            <Icon icon="ep:plus" class="mr-5px" />新增凭证
          </el-button>
        </div>
        <el-table
          v-loading="listLoading"
          :data="credentialList"
          highlight-current-row
          row-key="id"
          @row-click="editCredential"
        >
          <el-table-column label="名称" min-width="130" prop="name" show-overflow-tooltip />
          <el-table-column label="Token" align="center" width="82">
            <template #default="scope">
              <el-tag :type="scope.row.geweTokenConfigured ? 'success' : 'info'" effect="plain">
                {{ scope.row.geweTokenConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="76">
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'success' : 'info'" effect="plain">
                {{ scope.row.status === 0 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="78">
            <template #default="scope">
              <el-button link type="primary" @click.stop="editCredential(scope.row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-form
        ref="formRef"
        v-loading="formLoading"
        :model="formData"
        :rules="formRules"
        class="credential-form"
        label-width="96px"
      >
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="formData.name" placeholder="例如 华东 GeWe 托管服务" />
        </el-form-item>
        <el-form-item label="API 地址" prop="geweApiBaseUrl">
          <el-input v-model="formData.geweApiBaseUrl" placeholder="例如 https://api.geweapi.com" />
        </el-form-item>
        <el-form-item label="API Token" prop="geweToken">
          <el-input
            v-model="formData.geweToken"
            :placeholder="tokenConfigured ? '已配置，留空则不修改' : '请输入 GeWe API Token'"
            show-password
          >
            <template #append>
              <el-tag :type="tokenConfigured ? 'success' : 'info'" effect="plain">
                {{ tokenConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="回调地址" prop="callbackUrl">
          <el-input
            v-model="formData.callbackUrl"
            placeholder="建议填写公网地址，例如 https://域名/api/v1/gewechat/callback?token=xxx"
          >
            <template #append>
              <el-button :disabled="!formData.callbackUrl" @click="copyCallback">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="回调令牌" prop="callbackToken">
          <el-input v-model="formData.callbackToken" disabled placeholder="保存后自动生成" />
        </el-form-item>
        <el-form-item label="签名密钥" prop="callbackSecret">
          <el-input v-model="formData.callbackSecret" placeholder="可选，用于校验回调签名" show-password />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio-button :label="0">启用</el-radio-button>
            <el-radio-button :label="1">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保 存</el-button>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as GeweCredentialApi from '@/api/agent/geweCredential'

defineOptions({ name: 'AgentGeweCredentialForm' })

const message = useMessage()

const dialogVisible = ref(false)
const listLoading = ref(false)
const formLoading = ref(false)
const tokenConfigured = ref(false)
const formRef = ref()
const credentialList = ref<GeweCredentialApi.AgentGeweCredentialVO[]>([])
const formData = ref<GeweCredentialApi.AgentGeweCredentialVO>({
  id: undefined,
  name: '默认 GeWe',
  geweApiBaseUrl: 'https://api.geweapi.com',
  geweToken: '',
  callbackToken: '',
  callbackSecret: '',
  callbackUrl: '',
  status: 0
})
const formRules = reactive({
  name: [{ required: true, message: '配置名称不能为空', trigger: 'blur' }],
  geweApiBaseUrl: [{ required: true, message: 'API 地址不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const open = async () => {
  dialogVisible.value = true
  await loadCredentialList()
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  formLoading.value = true
  try {
    const id = await GeweCredentialApi.saveCredential(formData.value)
    message.success('GeWe 凭证已保存')
    await loadCredentialList(id)
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const loadCredentialList = async (selectedId?: number) => {
  listLoading.value = true
  try {
    credentialList.value = await GeweCredentialApi.getCredentialList()
    const selected =
      credentialList.value.find((item) => item.id === selectedId) ||
      credentialList.value.find((item) => item.id === formData.value.id) ||
      credentialList.value[0]
    if (selected) {
      editCredential(selected)
    } else {
      createCredential()
    }
  } finally {
    listLoading.value = false
  }
}

const createCredential = () => {
  tokenConfigured.value = false
  formData.value = {
    id: undefined,
    name: '默认 GeWe',
    geweApiBaseUrl: 'https://api.geweapi.com',
    geweToken: '',
    callbackToken: '',
    callbackSecret: '',
    callbackUrl: '',
    status: 0
  }
  formRef.value?.clearValidate()
}

const editCredential = (row: GeweCredentialApi.AgentGeweCredentialVO) => {
  tokenConfigured.value = !!row.geweTokenConfigured
  formData.value = {
    ...row,
    geweToken: ''
  }
  formRef.value?.clearValidate()
}

const copyCallback = async () => {
  if (!formData.value.callbackUrl) return
  const targetUrl = /^https?:\/\//i.test(formData.value.callbackUrl)
    ? formData.value.callbackUrl
    : `${window.location.origin}${
        formData.value.callbackUrl.startsWith('/') ? formData.value.callbackUrl : `/${formData.value.callbackUrl}`
      }`
  await navigator.clipboard.writeText(targetUrl)
  message.success('已复制回调地址')
}
</script>

<style scoped>
.credential-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(0, 1.1fr);
  gap: 20px;
}

.credential-list {
  min-width: 0;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.list-title {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.credential-form {
  min-width: 0;
  padding-left: 18px;
  border-left: 1px solid var(--el-border-color-lighter);
}

@media (max-width: 900px) {
  .credential-layout {
    grid-template-columns: 1fr;
  }

  .credential-form {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
