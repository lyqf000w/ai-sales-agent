<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="760">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-divider content-position="left">基本信息</el-divider>
      <el-form-item label="GeWe 凭证" prop="geweCredentialId">
        <el-select
          v-model="formData.geweCredentialId"
          class="!w-1/1"
          filterable
          placeholder="请选择该微信号所属 GeWe 凭证"
        >
          <el-option
            v-for="credential in credentialList"
            :key="credential.id"
            :label="credential.name"
            :value="credential.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="Gewe Appid" prop="geweAppId">
        <el-input v-model="formData.geweAppId" placeholder="请输入 Gewe Appid" />
      </el-form-item>
      <el-form-item label="绑定 Agent" prop="agentId">
        <el-select
          v-model="formData.agentId"
          class="!w-1/1"
          clearable
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
      <el-form-item label="Gewe 账号" prop="geweAccountId">
        <el-input v-model="formData.geweAccountId" placeholder="请输入 Gewe 账号标识" />
      </el-form-item>
      <el-form-item label="微信号" prop="wechatId">
        <el-input v-model="formData.wechatId" placeholder="请输入托管微信号" />
      </el-form-item>
      <el-form-item label="微信昵称" prop="nickname">
        <el-input v-model="formData.nickname" placeholder="请输入微信昵称" />
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUserId">
        <UserSelect v-model="formData.ownerUserId" placeholder="请选择负责人" />
      </el-form-item>
      <el-form-item label="知识库" prop="knowledgeBaseId">
        <el-select v-model="formData.knowledgeBaseId" class="!w-1/1" clearable filterable placeholder="继承 Agent 默认知识库">
          <el-option v-for="item in knowledgeBaseList" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">对话策略</el-divider>
      <el-form-item label="回复模式" prop="replyMode">
        <el-radio-group v-model="formData.replyMode">
          <el-radio-button label="MANUAL_CONFIRM">人工确认</el-radio-button>
          <el-radio-button label="AUTO_REPLY">自动回复</el-radio-button>
          <el-radio-button label="RECORD_ONLY">仅记录</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="静默秒数" prop="quietSeconds">
        <div class="policy-field">
          <el-input-number v-model="formData.quietSeconds" :min="1" class="!w-180px" />
          <div class="field-help">
            默认 90 秒。收到客户最后一条消息后等待该时间，再完整分析期间所有对话并回复客户。
          </div>
        </div>
      </el-form-item>
      <el-form-item label="营业时间" prop="businessHours">
        <div class="time-range">
          <el-time-select
            v-model="businessHours.start"
            class="!w-160px"
            end="23:30"
            placeholder="开始时间"
            start="00:00"
            step="00:30"
          />
          <span class="time-separator">至</span>
          <el-time-select
            v-model="businessHours.end"
            class="!w-160px"
            end="23:30"
            placeholder="结束时间"
            start="00:00"
            step="00:30"
          />
        </div>
      </el-form-item>

      <el-divider content-position="left">状态</el-divider>
      <el-form-item label="账号状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button :label="0">启用</el-radio-button>
          <el-radio-button :label="1">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as AgentApi from '@/api/agent/agent'
import * as GeweCredentialApi from '@/api/agent/geweCredential'
import * as KnowledgeApi from '@/api/agent/knowledge'
import * as WechatAccountApi from '@/api/agent/wechatAccount'
import UserSelect from '@/views/system/user/components/UserSelect.vue'

defineOptions({ name: 'AgentWechatAccountForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<WechatAccountApi.AgentWechatAccountVO>({
  id: undefined,
  geweCredentialId: undefined,
  agentId: undefined,
  knowledgeBaseId: undefined,
  ownerUserId: undefined,
  geweAppId: '',
  geweAccountId: '',
  wechatId: '',
  nickname: '',
  replyMode: 'MANUAL_CONFIRM',
  quietSeconds: 90,
  businessHours: {
    start: '08:00',
    end: '22:00'
  },
  status: 0
})
const formRules = reactive({
  geweCredentialId: [{ required: true, message: 'GeWe 凭证不能为空', trigger: 'change' }],
  geweAppId: [{ required: true, message: 'Gewe Appid 不能为空', trigger: 'blur' }],
  replyMode: [{ required: true, message: '回复模式不能为空', trigger: 'change' }],
  quietSeconds: [{ validator: validateQuietSeconds, trigger: 'blur' }],
  businessHours: [{ validator: validateBusinessHours, trigger: 'change' }],
  status: [{ required: true, message: '账号状态不能为空', trigger: 'change' }]
})
const formRef = ref()
const agentList = ref<AgentApi.AgentSimpleVO[]>([])
const knowledgeBaseList = ref<KnowledgeApi.AgentKnowledgeBaseVO[]>([])
const credentialList = ref<GeweCredentialApi.AgentGeweCredentialVO[]>([])

const loadAgentList = async () => {
  if (agentList.value.length > 0) return
  agentList.value = await AgentApi.getSimpleAgentList()
}

const loadKnowledgeBaseList = async () => {
  if (knowledgeBaseList.value.length > 0) return
  knowledgeBaseList.value = await KnowledgeApi.getSimpleKnowledgeBaseList()
}

const loadCredentialList = async () => {
  credentialList.value = await GeweCredentialApi.getEnabledCredentialList()
}

const open = async (id: number) => {
  dialogVisible.value = true
  dialogTitle.value = '编辑微信账号'
  resetForm()
  await Promise.all([loadAgentList(), loadKnowledgeBaseList(), loadCredentialList()])
  formLoading.value = true
  try {
    formData.value = normalizeAccount(await WechatAccountApi.getWechatAccount(id))
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  formLoading.value = true
  try {
    await WechatAccountApi.updateWechatAccount(formData.value)
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    geweCredentialId: undefined,
    agentId: undefined,
    knowledgeBaseId: undefined,
    ownerUserId: undefined,
    geweAppId: '',
    geweAccountId: '',
    wechatId: '',
    nickname: '',
    replyMode: 'MANUAL_CONFIRM',
    quietSeconds: 90,
    businessHours: {
      start: '08:00',
      end: '22:00'
    },
    status: 0
  }
  formRef.value?.resetFields()
}

const normalizeAccount = (
  account: WechatAccountApi.AgentWechatAccountVO
): WechatAccountApi.AgentWechatAccountVO => ({
  ...account,
  replyMode: account.replyMode || 'MANUAL_CONFIRM',
  quietSeconds: normalizeQuietSeconds(account),
  businessHours: normalizeBusinessHours(account.businessHours)
})

type LegacyAccountPolicy = WechatAccountApi.AgentWechatAccountVO & { quietMinutes?: number }

const normalizeQuietSeconds = (account: WechatAccountApi.AgentWechatAccountVO) => {
  const legacyQuietSeconds = ((account as LegacyAccountPolicy).quietMinutes ?? 1) * 60
  const quietSeconds = account.quietSeconds ?? legacyQuietSeconds
  return quietSeconds > 0 ? quietSeconds : 90
}

const normalizeBusinessHours = (businessHours?: { start?: string; end?: string }) => ({
  start: businessHours?.start || '08:00',
  end: businessHours?.end || '22:00'
})

const businessHours = computed({
  get: () => {
    if (!formData.value.businessHours) {
      formData.value.businessHours = { start: '', end: '' }
    }
    return formData.value.businessHours
  },
  set: (value) => {
    formData.value.businessHours = value
  }
})

const formatAgentLabel = (agent: AgentApi.AgentSimpleVO) => {
  return agent.aliasName ? `${agent.name}（${agent.aliasName}）` : agent.name
}

const formatAgentMeta = (agent: AgentApi.AgentSimpleVO) => {
  const parts = [agent.scene, agent.llmProvider, agent.llmModel].filter(Boolean)
  return parts.length > 0 ? parts.join(' / ') : '未配置场景或模型'
}

function validateBusinessHours(_: unknown, value: { start?: string; end?: string }, callback: (error?: Error) => void) {
  const hours = value || formData.value.businessHours
  if (!hours?.start || !hours?.end) {
    callback(new Error('营业时间不能为空'))
    return
  }
  callback()
}

function validateQuietSeconds(_: unknown, value: number | null | undefined, callback: (error?: Error) => void) {
  if (!value || value < 1) {
    callback(new Error('静默秒数不能为空，且至少为 1 秒'))
    return
  }
  callback()
}

onMounted(loadAgentList)
onMounted(loadKnowledgeBaseList)
onMounted(loadCredentialList)
</script>

<style scoped>
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

.time-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.policy-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-help {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.time-separator {
  color: var(--el-text-color-secondary);
}
</style>
