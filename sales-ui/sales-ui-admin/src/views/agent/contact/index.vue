<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="78px"
    >
      <el-form-item label="微信账号" prop="wechatAccountId">
        <el-select
          v-model="queryParams.wechatAccountId"
          class="!w-240px"
          clearable
          filterable
          placeholder="全部微信账号"
        >
          <el-option
            v-for="account in wechatAccountList"
            :key="account.id"
            :label="formatWechatAccountLabel(account)"
            :value="account.id!"
          >
            <div class="wechat-account-option">
              <span class="wechat-account-title">{{ account.nickname || account.wechatId || '-' }}</span>
              <span class="wechat-account-meta">{{ account.wechatId || account.geweAppId || account.id }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          class="!w-240px"
          clearable
          placeholder="昵称 / 微信号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户等级" prop="customerLevel">
        <el-select v-model="queryParams.customerLevel" class="!w-180px" clearable placeholder="全部">
          <el-option label="普通客户" :value="0" />
          <el-option label="目标客户" :value="1" />
          <el-option label="重要客户" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="购买意愿" prop="purchaseIntention">
        <el-select v-model="queryParams.purchaseIntention" class="!w-160px" clearable placeholder="全部">
          <el-option
            v-for="option in purchaseIntentionOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="销售阶段" prop="salesStage">
        <el-select v-model="queryParams.salesStage" class="!w-180px" clearable placeholder="全部">
          <el-option v-for="option in salesStageOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="跟进优先级" prop="followUpPriority">
        <el-select v-model="queryParams.followUpPriority" class="!w-160px" clearable placeholder="全部">
          <el-option
            v-for="option in followUpPriorityOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="客户" min-width="210">
        <template #default="scope">
          <div class="contact-cell">
            <el-avatar :size="34" :src="scope.row.avatar">
              {{ getInitial(scope.row.nickname || scope.row.wechatId) }}
            </el-avatar>
            <div>
              <div class="contact-name">{{ scope.row.nickname || '-' }}</div>
              <div class="contact-meta">{{ scope.row.wechatId || scope.row.externalUserId }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="微信账号" align="center" prop="wechatAccountId" width="110" />
      <el-table-column label="客户等级" align="center" prop="customerLevel" width="150">
        <template #default="scope">
          <el-select
            v-model="scope.row.customerLevel"
            class="!w-120px"
            size="small"
            @change="(value) => handleLevelChange(scope.row.id, value)"
          >
            <el-option label="普通" :value="0" />
            <el-option label="目标" :value="1" />
            <el-option label="重要" :value="2" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="销售洞察" min-width="320">
        <template #default="scope">
          <div class="insight-cell">
            <div class="insight-tags">
              <el-tag :type="getPurchaseIntentionMeta(scope.row.purchaseIntention).type" effect="plain" size="small">
                {{ getPurchaseIntentionMeta(scope.row.purchaseIntention).label }}
              </el-tag>
              <el-tag :type="getFollowUpPriorityMeta(scope.row.followUpPriority).type" effect="plain" size="small">
                {{ getFollowUpPriorityMeta(scope.row.followUpPriority).label }}
              </el-tag>
              <el-tag :type="getSentimentMeta(scope.row.customerSentiment).type" effect="plain" size="small">
                {{ getSentimentMeta(scope.row.customerSentiment).label }}
              </el-tag>
            </div>
            <div class="insight-stage">{{ getSalesStageLabel(scope.row.salesStage) }}</div>
            <div class="contact-tags">
              <el-tag
                v-for="tag in getContactTags(scope.row.id)"
                :key="tag.id"
                effect="plain"
                size="small"
                :style="getReadableTagStyle(tag.color)"
              >
                {{ tag.name }}
              </el-tag>
              <span v-if="getContactTags(scope.row.id).length === 0" class="empty-tags">暂无标签</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="会话状态" align="center" prop="lastConversationStatus" width="120">
        <template #default="scope">
          <el-tag effect="plain">
            {{ getConversationStatus(scope.row.lastConversationStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="回复策略" min-width="150">
        <template #default="scope">
          <div class="policy-cell">
            <el-tag :type="hasContactPolicy(scope.row) ? 'warning' : 'info'" effect="plain">
              {{ hasContactPolicy(scope.row) ? '好友覆盖' : '继承微信号' }}
            </el-tag>
            <span>{{ getContactPolicySummary(scope.row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="ownerUserId" width="100" />
      <el-table-column
        label="最近消息"
        align="center"
        prop="lastMessageTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openPolicyDialog(scope.row)">策略</el-button>
          <el-button link type="primary" @click="openInsightDialog(scope.row)">洞察</el-button>
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

  <Dialog v-model="policyDialogVisible" title="回复策略" width="640">
    <el-form :model="policyForm" label-width="110px">
      <el-form-item label="客户">
        <el-input v-model="policyForm.contactName" disabled />
      </el-form-item>
      <el-form-item label="回复模式">
        <el-select v-model="policyForm.replyMode" class="!w-240px" clearable placeholder="继承微信号策略">
          <el-option label="人工确认" value="MANUAL_CONFIRM" />
          <el-option label="自动回复" value="AUTO_REPLY" />
          <el-option label="仅记录" value="RECORD_ONLY" />
        </el-select>
      </el-form-item>
      <el-form-item label="静默秒数">
        <div class="policy-control">
          <el-checkbox v-model="policyInherit.quietSeconds">继承</el-checkbox>
          <el-input-number
            v-model="policyForm.quietSeconds"
            :disabled="policyInherit.quietSeconds"
            :min="0"
            class="!w-180px"
          />
        </div>
      </el-form-item>
      <el-form-item label="营业时间">
        <div class="policy-control">
          <el-checkbox v-model="policyInherit.businessHours">继承</el-checkbox>
          <div class="time-range">
            <el-time-select
              v-model="policyBusinessHours.start"
              :disabled="policyInherit.businessHours"
              class="!w-140px"
              end="23:30"
              placeholder="开始时间"
              start="00:00"
              step="00:30"
            />
            <span class="time-separator">至</span>
            <el-time-select
              v-model="policyBusinessHours.end"
              :disabled="policyInherit.businessHours"
              class="!w-140px"
              end="23:30"
              placeholder="结束时间"
              start="00:00"
              step="00:30"
            />
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitPolicy">确 定</el-button>
      <el-button @click="policyDialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="insightDialogVisible" title="销售洞察" width="640">
    <el-form :model="insightForm" label-width="100px">
      <el-form-item label="客户">
        <el-input v-model="insightForm.contactName" disabled />
      </el-form-item>
      <el-form-item label="购买意愿">
        <el-select v-model="insightForm.purchaseIntention" class="!w-240px">
          <el-option
            v-for="option in purchaseIntentionOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="销售阶段">
        <el-select v-model="insightForm.salesStage" class="!w-240px">
          <el-option v-for="option in salesStageOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户情绪">
        <el-select v-model="insightForm.customerSentiment" class="!w-240px">
          <el-option
            v-for="option in customerSentimentOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="跟进优先级">
        <el-select v-model="insightForm.followUpPriority" class="!w-240px">
          <el-option
            v-for="option in followUpPriorityOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="客户标签">
        <el-select v-model="selectedTagIds" class="!w-full" multiple placeholder="请选择标签">
          <el-option v-for="tag in allTags" :key="tag.id" :label="tag.name" :value="tag.id!" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitInsight">确 定</el-button>
      <el-button @click="insightDialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { getReadableTagStyle } from '@/utils/color'
import * as ContactApi from '@/api/agent/contact'
import * as ReplyPolicyApi from '@/api/agent/replyPolicy'
import * as TagApi from '@/api/agent/tag'
import * as WechatAccountApi from '@/api/agent/wechatAccount'

defineOptions({ name: 'AgentContact' })

const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<ContactApi.AgentWechatContactVO[]>([])
const wechatAccountList = ref<WechatAccountApi.AgentWechatAccountVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  wechatAccountId: undefined,
  keyword: '',
  customerLevel: undefined,
  purchaseIntention: undefined,
  salesStage: undefined,
  followUpPriority: undefined
})
const allTags = ref<TagApi.AgentContactTagVO[]>([])
const contactTagMap = ref<Record<number, TagApi.AgentContactTagVO[]>>({})
const selectedTagIds = ref<number[]>([])
const policyDialogVisible = ref(false)
const policyForm = reactive<{
  id?: number
  contactName: string
  replyMode?: string
  quietSeconds: number
  businessHours: {
    start?: string
    end?: string
  }
}>({
  id: undefined,
  contactName: '',
  replyMode: undefined,
  quietSeconds: 0,
  businessHours: {
    start: '',
    end: ''
  }
})
const policyInherit = reactive({
  quietSeconds: true,
  businessHours: true
})
const insightDialogVisible = ref(false)
const insightForm = reactive({
  id: undefined as number | undefined,
  contactName: '',
  purchaseIntention: 'MEDIUM',
  salesStage: 'NEW_LEAD',
  customerSentiment: 'NEUTRAL',
  followUpPriority: 'NORMAL'
})

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const purchaseIntentionOptions = [
  { label: '低意愿', value: 'LOW', type: 'info' as TagType },
  { label: '中意愿', value: 'MEDIUM', type: 'primary' as TagType },
  { label: '高意愿', value: 'HIGH', type: 'warning' as TagType },
  { label: '强烈意愿', value: 'STRONG', type: 'danger' as TagType }
]

const salesStageOptions = [
  { label: '新线索', value: 'NEW_LEAD' },
  { label: '需求确认', value: 'NEEDS_CONFIRMED' },
  { label: '产品介绍', value: 'PRODUCT_INTRO' },
  { label: '报价议价', value: 'QUOTE_NEGOTIATION' },
  { label: '成交推进', value: 'DEAL_PROGRESS' },
  { label: '售后咨询', value: 'AFTER_SALES' }
]

const customerSentimentOptions = [
  { label: '正向', value: 'POSITIVE', type: 'success' as TagType },
  { label: '中性', value: 'NEUTRAL', type: 'info' as TagType },
  { label: '负向', value: 'NEGATIVE', type: 'warning' as TagType }
]

const followUpPriorityOptions = [
  { label: '普通跟进', value: 'NORMAL', type: 'info' as TagType },
  { label: '重点跟进', value: 'FOCUS', type: 'warning' as TagType },
  { label: '紧急跟进', value: 'URGENT', type: 'danger' as TagType }
]

const conversationStatusMap: Record<number, string> = {
  0: '打开',
  1: '自动回复',
  2: '待确认',
  3: '人工接管',
  4: '已关闭'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await ContactApi.getWechatContactPage(queryParams)
    list.value = data.list
    total.value = data.total
    await loadContactTags(data.list)
  } finally {
    loading.value = false
  }
}

const loadContactTags = async (contacts: ContactApi.AgentWechatContactVO[]) => {
  const entries = await Promise.all(
    contacts.map(async (contact) => {
      const tagIds = await ContactApi.getWechatContactTags(contact.id)
      const tags = allTags.value.filter((tag) => tag.id && tagIds.includes(tag.id))
      return [contact.id, tags] as const
    })
  )
  contactTagMap.value = Object.fromEntries(entries)
}

const loadWechatAccountList = async () => {
  const data = await WechatAccountApi.getWechatAccountPage({
    pageNo: 1,
    pageSize: 100
  })
  wechatAccountList.value = data.list
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const handleLevelChange = async (id: number, customerLevel: number) => {
  await ContactApi.updateWechatContactLevel(id, customerLevel)
  message.success('客户等级已更新')
}

const openPolicyDialog = (row: ContactApi.AgentWechatContactVO) => {
  policyForm.id = row.id
  policyForm.contactName = row.nickname || row.remark || row.wechatId || row.externalUserId
  policyForm.replyMode = row.replyMode || undefined
  policyInherit.quietSeconds = row.quietSeconds == null
  policyInherit.businessHours = row.businessHours == null
  policyForm.quietSeconds = row.quietSeconds ?? ((row as LegacyContactPolicy).quietMinutes ?? 0) * 60
  policyForm.businessHours = {
    start: '',
    end: '',
    ...(row.businessHours || {})
  }
  policyDialogVisible.value = true
}

const openInsightDialog = async (row: ContactApi.AgentWechatContactVO) => {
  insightForm.id = row.id
  insightForm.contactName = row.nickname || row.remark || row.wechatId || row.externalUserId
  insightForm.purchaseIntention = row.purchaseIntention || 'MEDIUM'
  insightForm.salesStage = row.salesStage || 'NEW_LEAD'
  insightForm.customerSentiment = row.customerSentiment || 'NEUTRAL'
  insightForm.followUpPriority = row.followUpPriority || 'NORMAL'
  selectedTagIds.value = await ContactApi.getWechatContactTags(row.id)
  insightDialogVisible.value = true
}

const submitPolicy = async () => {
  if (!policyForm.id) return
  await ReplyPolicyApi.saveReplyPolicy({
    contactId: policyForm.id,
    replyMode: policyForm.replyMode || null,
    quietSeconds: policyInherit.quietSeconds ? null : policyForm.quietSeconds,
    businessHours: policyInherit.businessHours ? null : policyForm.businessHours
  })
  message.success('回复策略已更新')
  policyDialogVisible.value = false
  await getList()
}

const submitInsight = async () => {
  if (!insightForm.id) return
  await ContactApi.updateWechatContactSalesInsight({
    id: insightForm.id,
    purchaseIntention: insightForm.purchaseIntention,
    salesStage: insightForm.salesStage,
    customerSentiment: insightForm.customerSentiment,
    followUpPriority: insightForm.followUpPriority
  })
  await ContactApi.updateWechatContactTags(insightForm.id, selectedTagIds.value)
  message.success('销售洞察已更新')
  insightDialogVisible.value = false
  await getList()
}

const getInitial = (text?: string) => {
  return text ? text.slice(0, 1) : '客'
}

const getConversationStatus = (status?: number) => {
  return conversationStatusMap[status ?? -1] || '未知'
}

const getPurchaseIntentionMeta = (value?: string) => {
  return purchaseIntentionOptions.find((option) => option.value === value) || purchaseIntentionOptions[1]
}

const getSalesStageLabel = (value?: string) => {
  return salesStageOptions.find((option) => option.value === value)?.label || salesStageOptions[0].label
}

const getSentimentMeta = (value?: string) => {
  return customerSentimentOptions.find((option) => option.value === value) || customerSentimentOptions[1]
}

const getFollowUpPriorityMeta = (value?: string) => {
  return followUpPriorityOptions.find((option) => option.value === value) || followUpPriorityOptions[0]
}

const getContactTags = (contactId: number) => {
  return contactTagMap.value[contactId] || []
}

const getReplyModeLabel = (replyMode?: string | null) => {
  const labels: Record<string, string> = {
    MANUAL_CONFIRM: '人工确认',
    AUTO_REPLY: '自动回复',
    RECORD_ONLY: '仅记录'
  }
  return labels[replyMode || ''] || '继承'
}

const hasContactPolicy = (row: ContactApi.AgentWechatContactVO) => {
  return (
    row.replyMode != null ||
    row.quietSeconds != null ||
    row.businessHours != null
  )
}

const getContactPolicySummary = (row: ContactApi.AgentWechatContactVO) => {
  if (!hasContactPolicy(row)) return '-'
  const parts = [
    row.replyMode ? getReplyModeLabel(row.replyMode) : '',
    row.quietSeconds != null ? `静默 ${row.quietSeconds} 秒` : '',
    row.businessHours?.start && row.businessHours?.end
      ? `营业 ${row.businessHours.start}-${row.businessHours.end}`
      : ''
  ].filter(Boolean)
  return parts.join(' · ') || '已覆盖'
}

type LegacyContactPolicy = ContactApi.AgentWechatContactVO & { quietMinutes?: number }

const formatWechatAccountLabel = (account: WechatAccountApi.AgentWechatAccountVO) => {
  const name = account.nickname || account.wechatId || account.geweAppId || `账号 ${account.id}`
  const meta = account.wechatId || account.geweAppId
  return meta && meta !== name ? `${name}（${meta}）` : name
}

const policyBusinessHours = computed({
  get: () => policyForm.businessHours,
  set: (value) => {
    policyForm.businessHours = value
  }
})

onMounted(async () => {
  await Promise.all([
    loadWechatAccountList(),
    TagApi.getSimpleTagList().then((data) => {
      allTags.value = data
    })
  ])
  await getList()
})
</script>

<style scoped>
.contact-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.contact-name {
  line-height: 20px;
  font-weight: 500;
}

.contact-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.wechat-account-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.wechat-account-title {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wechat-account-meta {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.policy-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 18px;
}

.insight-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.insight-tags,
.contact-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.insight-stage,
.empty-tags {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.policy-control {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-separator {
  color: var(--el-text-color-secondary);
}
</style>
