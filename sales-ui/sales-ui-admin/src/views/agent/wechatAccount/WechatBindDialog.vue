<template>
  <Dialog v-model="dialogVisible" title="绑定微信号" width="780">
    <div class="bind-layout">
      <el-form
        ref="formRef"
        v-loading="loading"
        :model="formData"
        :rules="formRules"
        label-width="96px"
      >
        <el-form-item label="GeWe 凭证" prop="credentialId">
          <el-select
            v-model="formData.credentialId"
            class="!w-1/1"
            filterable
            placeholder="桥接 Python 后端时可不选 GeWe 凭证"
          >
            <el-option
              v-for="credential in credentialList"
              :key="credential.id"
              :label="credential.name"
              :value="credential.id!"
            >
              <div class="credential-option">
                <span class="credential-option-title">{{ credential.name }}</span>
                <span class="credential-option-meta">
                  {{ credential.geweTokenConfigured ? 'Token 已配置' : 'Token 未配置' }}
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="绑定 Agent" prop="agentId">
          <el-select
            v-model="formData.agentId"
            class="!w-1/1"
            filterable
            placeholder="请选择处理消息的 Agent"
          >
            <el-option
              v-for="agent in agentList"
              :key="agent.id"
              :label="formatAgentLabel(agent)"
              :value="agent.id"
            >
              <div class="agent-option">
                <span class="agent-option-title">{{ agent.name }}</span>
                <span class="agent-option-meta">{{ formatAgentMeta(agent) }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="ownerUserId">
          <UserSelect v-model="formData.ownerUserId" placeholder="请选择负责人" />
        </el-form-item>
        <el-form-item>
          <el-button :disabled="loading" type="primary" @click="createSession">
            <Icon icon="ep:connection" class="mr-5px" />生成二维码
          </el-button>
          <el-button :disabled="!session?.id || checking" @click="refreshStatus">
            <Icon icon="ep:refresh" class="mr-5px" />刷新状态
          </el-button>
        </el-form-item>
      </el-form>

      <div class="qr-panel">
        <div class="qr-box">
          <img v-if="qrImageSrc" :src="qrImageSrc" alt="微信登录二维码" />
          <el-empty v-else :image-size="82" description="等待生成二维码" />
        </div>
        <div class="status-row">
          <span>绑定状态</span>
          <el-tag :type="statusMeta.type" effect="plain">{{ statusMeta.label }}</el-tag>
        </div>
        <div v-if="session?.appId" class="meta-line">AppId：{{ session.appId }}</div>
        <div v-if="session?.nickName" class="meta-line">扫码账号：{{ session.nickName }}</div>
        <el-link v-if="session?.verifyUrl" :href="session.verifyUrl" target="_blank" type="primary">
          打开确认链接
        </el-link>
        <el-alert
          v-if="session?.errorMessage"
          :closable="false"
          :title="session.errorMessage"
          class="mt-12px"
          type="error"
        />
        <el-input
          v-if="session?.qrData && !qrImageSrc"
          v-model="session.qrData"
          class="mt-12px"
          readonly
          type="textarea"
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as AgentApi from '@/api/agent/agent'
import * as GeweCredentialApi from '@/api/agent/geweCredential'
import * as WechatAccountApi from '@/api/agent/wechatAccount'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import QRCode from 'qrcode'

defineOptions({ name: 'AgentWechatBindDialog' })

const message = useMessage()

const dialogVisible = ref(false)
const loading = ref(false)
const checking = ref(false)
const formRef = ref()
const session = ref<WechatAccountApi.AgentWechatBindSessionVO>()
const generatedQrImage = ref('')
const agentList = ref<AgentApi.AgentSimpleVO[]>([])
const credentialList = ref<GeweCredentialApi.AgentGeweCredentialVO[]>([])
const formData = reactive<WechatAccountApi.AgentWechatBindSessionCreateReqVO>({
  credentialId: undefined,
  agentId: undefined as unknown as number,
  ownerUserId: undefined as unknown as number
})
const formRules = reactive({
  agentId: [{ required: true, message: '绑定 Agent 不能为空', trigger: 'change' }],
  ownerUserId: [{ required: true, message: '负责人不能为空', trigger: 'change' }]
})
let pollTimer: number | undefined

const statusMap: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }> = {
  WAIT_SCAN: { label: '等待扫码', type: 'info' },
  WAIT_CONFIRM: { label: '等待确认', type: 'warning' },
  BOUND: { label: '已绑定', type: 'success' },
  EXPIRED: { label: '已过期', type: 'danger' },
  FAILED: { label: '绑定失败', type: 'danger' }
}

const open = async () => {
  dialogVisible.value = true
  reset()
  await Promise.all([loadAgentList(), loadCredentialList()])
}
defineExpose({ open })

const emit = defineEmits(['success'])

const createSession = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  try {
    session.value = await WechatAccountApi.createBindSession(formData)
    startPolling()
  } finally {
    loading.value = false
  }
}

const refreshStatus = async () => {
  if (!session.value?.id || checking.value) return
  const sessionId = session.value.id
  checking.value = true
  try {
    const checkedSession = await WechatAccountApi.checkBindSession(sessionId)
    session.value = checkedSession
    if (checkedSession.status === 'BOUND') {
      stopPolling()
      message.success('微信号已绑定')
      emit('success')
    }
    if (['EXPIRED', 'FAILED'].includes(checkedSession.status)) {
      stopPolling()
    }
  } catch {
    const latestSession = await WechatAccountApi.getBindSession(sessionId)
    session.value = latestSession
    if (['EXPIRED', 'FAILED'].includes(latestSession.status)) {
      stopPolling()
    }
  } finally {
    checking.value = false
  }
}

const loadAgentList = async () => {
  if (agentList.value.length > 0) return
  agentList.value = await AgentApi.getSimpleAgentList()
  if (agentList.value.length === 1) {
    formData.agentId = agentList.value[0].id
  }
}

const loadCredentialList = async () => {
  credentialList.value = await GeweCredentialApi.getEnabledCredentialList()
  if (credentialList.value.length === 1) {
    formData.credentialId = credentialList.value[0].id
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = window.setInterval(refreshStatus, 5000)
}

const stopPolling = () => {
  if (!pollTimer) return
  window.clearInterval(pollTimer)
  pollTimer = undefined
}

const reset = () => {
  stopPolling()
  session.value = undefined
  generatedQrImage.value = ''
  formData.credentialId = undefined
  formData.agentId = undefined as unknown as number
  formData.ownerUserId = undefined as unknown as number
  formRef.value?.resetFields()
}

const normalizeVerifyUrl = (value?: string) => {
  if (!value) return ''
  try {
    return new URL(value).searchParams.get('t') || value
  } catch {
    return value
  }
}

const qrSourceText = computed(() => {
  return session.value?.status === 'WAIT_CONFIRM'
    ? normalizeVerifyUrl(session.value?.verifyUrl) || session.value?.qrData || ''
    : session.value?.qrData || session.value?.verifyUrl || ''
})

const showingVerifyQr = computed(() => {
  return session.value?.status === 'WAIT_CONFIRM' && !!session.value?.verifyUrl
})

const qrImageSrc = computed(() => {
  if (showingVerifyQr.value) {
    return generatedQrImage.value
  }
  const value = session.value?.qrImgBase64
  if (value) {
    return value.startsWith('data:') ? value : `data:image/png;base64,${value}`
  }
  return generatedQrImage.value
})

const statusMeta = computed(() => {
  return statusMap[session.value?.status || 'WAIT_SCAN'] || statusMap.WAIT_SCAN
})

const formatAgentLabel = (agent: AgentApi.AgentSimpleVO) => {
  return agent.aliasName ? `${agent.name}（${agent.aliasName}）` : agent.name
}

const formatAgentMeta = (agent: AgentApi.AgentSimpleVO) => {
  const parts = [agent.scene, agent.llmProvider, agent.llmModel].filter(Boolean)
  return parts.length > 0 ? parts.join(' / ') : '未配置场景或模型'
}

watch(dialogVisible, (visible) => {
  if (!visible) {
    stopPolling()
  }
})

watch(
  qrSourceText,
  async (value) => {
    generatedQrImage.value = ''
    if (!value || (session.value?.qrImgBase64 && !showingVerifyQr.value)) return
    try {
      generatedQrImage.value = await QRCode.toDataURL(value, {
        width: 180,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
    } catch {
      generatedQrImage.value = ''
    }
  },
  { immediate: true }
)

onUnmounted(stopPolling)
</script>

<style scoped>
.bind-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
}

.qr-panel {
  min-height: 306px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.qr-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 190px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.qr-box img {
  width: 168px;
  height: 168px;
  object-fit: contain;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  color: var(--el-text-color-regular);
}

.meta-line {
  margin-top: 8px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.agent-option-title {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-option-meta {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.credential-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.credential-option-title {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credential-option-meta {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 768px) {
  .bind-layout {
    grid-template-columns: 1fr;
  }
}
</style>
