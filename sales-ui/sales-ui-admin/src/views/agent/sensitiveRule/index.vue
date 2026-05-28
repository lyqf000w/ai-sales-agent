<template>
  <ContentWrap>
    <el-alert
      class="mb-16px"
      :closable="false"
      show-icon
      title="人工升级规则用于拦截高风险消息。命中后会暂停自动回复，进入人工确认或人工接管。"
      type="warning"
    />
    <el-alert
      v-if="showRuleRiskAlert"
      class="mb-16px"
      :closable="false"
      show-icon
      type="error"
    >
      <template #title>
        <div class="risk-rule-alert-title">
          <span>当前有 {{ riskAlert.total }} 个会话仍处于待确认/人工接管状态，自动回复已暂停。</span>
          <el-button link type="danger" @click="goRiskWorkbench">去客户工作台处理</el-button>
        </div>
      </template>
      <div class="risk-rule-alert-preview">
        <span v-for="item in riskAlert.preview" :key="item.contactId" class="risk-rule-preview-item">
          {{ formatRiskContact(item) }}
        </span>
      </div>
    </el-alert>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" class="!w-220px" clearable placeholder="规则名称" />
      </el-form-item>
      <el-form-item label="Agent" prop="agentId">
        <el-select v-model="queryParams.agentId" class="!w-180px" clearable filterable placeholder="全部 Agent">
          <el-option
            v-for="agent in agentList"
            :key="agent.id"
            :label="formatAgentLabel(agent)"
            :value="agent.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="应用" prop="routeApp">
        <el-select v-model="queryParams.routeApp" class="!w-140px" clearable placeholder="全部">
          <el-option label="GeWe" value="GEWE" />
        </el-select>
      </el-form-item>
      <el-form-item label="触发类型" prop="triggerType">
        <el-select v-model="queryParams.triggerType" class="!w-180px" clearable placeholder="全部">
          <el-option
            v-for="item in triggerTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="动作" prop="action">
        <el-select v-model="queryParams.action" class="!w-150px" clearable placeholder="全部">
          <el-option
            v-for="item in actionOptions"
            :key="item.value"
            :label="item.label"
            :value="Number(item.value)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-120px" clearable placeholder="全部">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="primary" plain @click="openForm('create')">
          <Icon icon="ep:plus" class="mr-5px" />新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="规则名称" prop="name" min-width="160" />
      <el-table-column label="作用范围" min-width="160">
        <template #default="scope">
          {{ scope.row.agentId ? getAgentName(scope.row.agentId) : '全局' }}
          <el-tag v-if="scope.row.routeApp" class="ml-6px" effect="plain" size="small">
            {{ scope.row.routeApp }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发类型" align="center" prop="triggerType" width="130">
        <template #default="scope">
          <el-tag effect="plain">{{ getTriggerTypeLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发条件" prop="pattern" min-width="240" show-overflow-tooltip>
        <template #default="scope">{{ getPatternText(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="动作" align="center" prop="action" width="120">
        <template #default="scope">
          <el-tag :type="getAction(scope.row.action).type" effect="plain">
            {{ getAction(scope.row.action).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险" align="center" prop="riskLevel" width="90">
        <template #default="scope">
          <el-tag :type="getRiskType(scope.row.riskLevel)" effect="plain">
            {{ getRiskLabel(scope.row.riskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'info'" effect="plain">
            {{ scope.row.status === 0 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <Dialog v-model="dialogVisible" :title="dialogTitle" width="840">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="120px">
      <el-form-item label="规则名称" prop="name">
        <el-input v-model="formData.name" placeholder="例如：退款投诉转人工" />
      </el-form-item>
      <el-form-item label="Agent" prop="agentId">
        <el-select v-model="formData.agentId" class="!w-280px" clearable filterable placeholder="全局规则">
          <el-option
            v-for="agent in agentList"
            :key="agent.id"
            :label="formatAgentLabel(agent)"
            :value="agent.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="接入应用" prop="routeApp">
        <el-select v-model="formData.routeApp" class="!w-180px" clearable placeholder="全部应用">
          <el-option label="GeWe" value="GEWE" />
        </el-select>
      </el-form-item>
      <el-form-item label="触发类型" prop="triggerType">
        <el-select
          v-model="formData.triggerType"
          class="!w-320px"
          placeholder="请选择触发类型"
          @change="handleTriggerTypeChange"
        >
          <el-option
            v-for="item in triggerTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
            <div class="trigger-option">
              <span>{{ item.label }}</span>
              <small>{{ item.description }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="触发条件" prop="pattern">
        <el-select
          v-if="formData.triggerType === 'INTENT'"
          v-model="formData.pattern"
          class="!w-420px"
          allow-create
          filterable
          placeholder="选择或输入意图编码"
        >
          <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select
          v-else-if="formData.triggerType === 'SENTIMENT'"
          v-model="formData.pattern"
          class="!w-320px"
          placeholder="请选择情绪"
        >
          <el-option v-for="item in sentimentOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select
          v-else-if="formData.triggerType === 'CUSTOMER_LEVEL'"
          v-model="formData.pattern"
          class="!w-320px"
          placeholder="请选择客户等级"
        >
          <el-option v-for="item in customerLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-else
          v-model="formData.pattern"
          :disabled="!isPatternRequired"
          :placeholder="patternPlaceholder"
        />
      </el-form-item>
      <el-form-item label="动作" prop="action">
        <el-radio-group v-model="formData.action">
          <el-radio-button
            v-for="item in actionOptions"
            :key="item.value"
            :label="Number(item.value)"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevel">
        <el-radio-group v-model="formData.riskLevel">
          <el-radio-button
            v-for="item in riskLevelOptions"
            :key="item.value"
            :label="Number(item.value)"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-180px" />
        <span class="ml-10px text-12px text-gray-500">数值越小越靠前；同时命中时优先按风险和动作处理。</span>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button :label="0">启用</el-radio-button>
          <el-radio-button :label="1">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" :rows="2" type="textarea" placeholder="说明这个规则用于什么场景" />
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
import * as ConversationApi from '@/api/agent/conversation'
import * as SensitiveRuleApi from '@/api/agent/sensitiveRule'

defineOptions({ name: 'AgentSensitiveRule' })

const { t } = useI18n()
const message = useMessage()
const router = useRouter()

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const loading = ref(true)
const total = ref(0)
const list = ref<SensitiveRuleApi.AgentSensitiveRuleVO[]>([])
const agentList = ref<AgentApi.AgentSimpleVO[]>([])
const triggerTypeOptions = ref<SensitiveRuleApi.AgentSensitiveRuleOptionVO[]>([])
const actionOptions = ref<SensitiveRuleApi.AgentSensitiveRuleOptionVO[]>([])
const riskLevelOptions = ref<SensitiveRuleApi.AgentSensitiveRuleOptionVO[]>([])
const intentOptions = ref<SensitiveRuleApi.AgentSensitiveRuleOptionVO[]>([])
const sentimentOptions = ref<SensitiveRuleApi.AgentSensitiveRuleOptionVO[]>([])
const customerLevelOptions = ref<SensitiveRuleApi.AgentSensitiveRuleOptionVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  agentId: undefined,
  routeApp: undefined,
  triggerType: undefined,
  action: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<SensitiveRuleApi.AgentSensitiveRuleVO>(createDefaultForm())
const riskAlert = reactive({
  total: 0,
  preview: [] as ConversationApi.AgentConversationContactVO[]
})
const activeRuleTotal = ref(0)
const showRuleRiskAlert = computed(() => riskAlert.total > 0 && activeRuleTotal.value > 0)
let riskPollTimer: number | undefined
let lastRiskTotal = 0
let riskAlertReady = false

const formRules = reactive({
  name: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
  triggerType: [{ required: true, message: '触发类型不能为空', trigger: 'change' }],
  pattern: [{ validator: validatePattern, trigger: 'change' }],
  action: [{ required: true, message: '动作不能为空', trigger: 'change' }],
  riskLevel: [{ required: true, message: '风险等级不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const isPatternRequired = computed(() =>
  ['KEYWORD', 'REGEX', 'INTENT', 'SENTIMENT', 'CUSTOMER_LEVEL'].includes(formData.value.triggerType)
)

const patternPlaceholder = computed(() => {
  const placeholders: Record<string, string> = {
    KEYWORD: '多个关键词可用逗号分隔，例如：退款,投诉,赔偿',
    REGEX: '例如：.*(退款|投诉|赔偿).*',
    RAG_MISS: '知识库未命中时自动触发，无需填写',
    REQUEST_HUMAN: '客户要求人工时自动触发，无需填写'
  }
  return placeholders[formData.value.triggerType] || '请输入触发条件'
})

function validatePattern(_: unknown, value: string, callback: (error?: Error) => void) {
  if (isPatternRequired.value && !value) {
    callback(new Error('触发条件不能为空'))
    return
  }
  callback()
}

const loadAgentList = async () => {
  agentList.value = await AgentApi.getSimpleAgentList()
}

const loadRuleOptions = async () => {
  const options = await SensitiveRuleApi.getSensitiveRuleOptions()
  triggerTypeOptions.value = options.triggerTypes || []
  actionOptions.value = options.actions || []
  riskLevelOptions.value = options.riskLevels || []
  intentOptions.value = options.intents || []
  sentimentOptions.value = options.sentiments || []
  customerLevelOptions.value = options.customerLevels || []
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SensitiveRuleApi.getSensitiveRulePage(queryParams)
    list.value = data.list
    total.value = data.total
    await loadActiveRuleTotal()
  } finally {
    loading.value = false
  }
}

const loadActiveRuleTotal = async () => {
  const data = await SensitiveRuleApi.getSensitiveRulePage({
    pageNo: 1,
    pageSize: 1,
    status: 0
  } as PageParam)
  activeRuleTotal.value = data.total || 0
}

const loadRiskAlert = async () => {
  await loadActiveRuleTotal()
  if (activeRuleTotal.value <= 0) {
    riskAlert.total = 0
    riskAlert.preview = []
    lastRiskTotal = 0
    riskAlertReady = true
    return
  }
  const data = await ConversationApi.getConversationContactPage({
    pageNo: 1,
    pageSize: 5,
    queueType: 'RISK'
  } as PageParam)
  riskAlert.total = data.total
  riskAlert.preview = data.list
  lastRiskTotal = data.total
  riskAlertReady = true
}

const goRiskWorkbench = () => {
  router.push({ path: '/agent/conversation', query: { queueType: 'RISK' } })
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增人工升级规则' : '编辑人工升级规则'
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = normalizeRule(await SensitiveRuleApi.getSensitiveRule(id))
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const payload = normalizeRule(formData.value)
    if (formType.value === 'create') {
      await SensitiveRuleApi.createSensitiveRule(payload)
      message.success(t('common.createSuccess'))
    } else {
      await SensitiveRuleApi.updateSensitiveRule(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await SensitiveRuleApi.deleteSensitiveRule(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

function createDefaultForm(): SensitiveRuleApi.AgentSensitiveRuleVO {
  return {
    id: undefined,
    name: '',
    agentId: undefined,
    routeApp: 'GEWE',
    matchType: 3,
    triggerType: 'INTENT',
    pattern: '',
    action: 1,
    riskLevel: 1,
    sort: 10,
    status: 0,
    remark: ''
  }
}

const resetForm = () => {
  formData.value = createDefaultForm()
  formRef.value?.resetFields()
}

const handleTriggerTypeChange = () => {
  formData.value.matchType = toMatchType(formData.value.triggerType)
  formData.value.pattern = defaultPattern(formData.value.triggerType)
  formRef.value?.clearValidate('pattern')
}

const getRiskType = (riskLevel?: number): TagType => {
  if (riskLevel === 2) return 'danger'
  if (riskLevel === 1) return 'warning'
  return 'success'
}

const getRiskLabel = (riskLevel?: number) => {
  return riskLevelOptions.value.find((item) => Number(item.value) === (riskLevel ?? 0))?.label || '绿色'
}

const getAction = (action?: number): { label: string; type: TagType } => {
  const typeMap: Record<number, TagType> = {
    0: 'success',
    1: 'warning',
    2: 'danger',
    3: 'danger'
  }
  return {
    label: actionOptions.value.find((item) => Number(item.value) === (action ?? 1))?.label || '人工确认',
    type: typeMap[action ?? 1] || 'warning'
  }
}

const getTriggerTypeLabel = (rule: SensitiveRuleApi.AgentSensitiveRuleVO) => {
  const triggerType = rule.triggerType || toTriggerType(rule.matchType)
  return triggerTypeOptions.value.find((item) => item.value === triggerType)?.label || '关键词'
}

const getPatternText = (rule: SensitiveRuleApi.AgentSensitiveRuleVO) => {
  const triggerType = rule.triggerType || toTriggerType(rule.matchType)
  if (triggerType === 'REQUEST_HUMAN') return '客户明确要求人工'
  if (triggerType === 'RAG_MISS') return '知识库未命中'
  if (triggerType === 'CUSTOMER_LEVEL') {
    return customerLevelOptions.value.find((item) => item.value === `${rule.pattern}`)?.label || rule.pattern || '-'
  }
  return rule.pattern || '-'
}

const normalizeRule = (
  rule: SensitiveRuleApi.AgentSensitiveRuleVO
): SensitiveRuleApi.AgentSensitiveRuleVO => {
  const triggerType = rule.triggerType || toTriggerType(rule.matchType)
  const pattern = ['REQUEST_HUMAN', 'RAG_MISS'].includes(triggerType) ? '' : rule.pattern
  return {
    ...rule,
    routeApp: rule.routeApp || '',
    triggerType,
    matchType: toMatchType(triggerType),
    pattern
  }
}

const toTriggerType = (matchType?: number) => {
  if (matchType === 1) return 'KEYWORD'
  if (matchType === 2) return 'REGEX'
  return 'INTENT'
}

const toMatchType = (triggerType?: string) => {
  if (triggerType === 'KEYWORD') return 1
  if (triggerType === 'REGEX') return 2
  return 3
}

const defaultPattern = (triggerType?: string) => {
  if (triggerType === 'SENTIMENT') return 'angry'
  if (triggerType === 'CUSTOMER_LEVEL') return '1'
  if (['REQUEST_HUMAN', 'RAG_MISS'].includes(triggerType || '')) return ''
  return ''
}

const formatAgentLabel = (agent: AgentApi.AgentSimpleVO) => {
  return agent.aliasName ? `${agent.name}（${agent.aliasName}）` : agent.name
}

const getAgentName = (agentId?: number) => {
  const agent = agentList.value.find((item) => item.id === agentId)
  return agent ? formatAgentLabel(agent) : `Agent #${agentId}`
}

const cleanWechatText = (value?: unknown) => {
  if (value == null) return ''
  return String(value).trim()
}

const isRawWechatIdentifier = (text: string) => {
  return text === 'weixin' || text.startsWith('wxid_') || text.endsWith('@chatroom')
}

const formatRiskContact = (contact: ConversationApi.AgentConversationContactVO) => {
  const candidates = [contact.displayName, contact.remark, contact.nickname, contact.wechatId, contact.externalUserId]
  for (const value of candidates) {
    const text = cleanWechatText(value)
    if (text && !isRawWechatIdentifier(text)) {
      return text
    }
  }
  return `会话 #${contact.conversationId || contact.contactId}`
}

onMounted(() => {
  loadAgentList()
  loadRuleOptions()
  getList()
  loadRiskAlert()
  riskPollTimer = window.setInterval(loadRiskAlert, 30000)
})

onBeforeUnmount(() => {
  if (riskPollTimer) {
    window.clearInterval(riskPollTimer)
  }
})
</script>

<style scoped>
.trigger-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.trigger-option small {
  color: var(--el-text-color-secondary);
}

.risk-rule-alert-title,
.risk-rule-alert-preview {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-rule-alert-title {
  justify-content: space-between;
  width: 100%;
}

.risk-rule-alert-preview {
  flex-wrap: wrap;
  margin-top: 6px;
}

.risk-rule-preview-item {
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger-dark-2);
  font-size: 12px;
}
</style>
