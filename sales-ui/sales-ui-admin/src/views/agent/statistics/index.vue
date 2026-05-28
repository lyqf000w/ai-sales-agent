<template>
  <ContentWrap>
    <div class="toolbar">
      <div class="toolbar-title">运营统计</div>
      <el-button :loading="loading" type="primary" plain @click="loadSummary">
        <Icon icon="ep:refresh" class="mr-5px" />刷新
      </el-button>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div v-loading="loading" class="metric-grid">
      <button
        v-for="item in metrics"
        :key="item.key"
        class="metric-item"
        type="button"
        @click="openMetricDetail(item)"
      >
        <div class="metric-icon">
          <Icon :icon="item.icon" />
        </div>
        <div class="metric-body">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-hint">{{ item.hint }}</div>
        </div>
        <Icon class="metric-arrow" icon="ep:arrow-right" />
      </button>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div class="section-header">
      <div class="section-title">销售洞察</div>
    </div>
    <div v-loading="loading" class="insight-grid">
      <div v-for="card in insightCards" :key="card.key" class="insight-card">
        <div class="insight-title">{{ card.title }}</div>
        <div v-if="card.items.length === 0" class="empty-dimension">暂无数据</div>
        <div v-for="item in card.items" :key="item.code" class="dimension-row">
          <div class="dimension-line">
            <span>{{ card.formatLabel(item.code) }}</span>
            <strong>{{ item.count }}</strong>
          </div>
          <div class="dimension-track">
            <div
              class="dimension-fill"
              :style="{ width: `${getDimensionPercent(item.count, card.total)}%`, background: card.color }"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </ContentWrap>

  <el-drawer v-model="detailVisible" :title="activeMetric?.detailTitle" size="760px">
    <div class="detail-toolbar">
      <div>
        <div class="detail-title">{{ activeMetric?.label }}</div>
        <div class="detail-subtitle">{{ activeMetric?.description }}</div>
      </div>
      <el-button v-if="activeMetric?.targetQueue" plain type="primary" @click="openWorkbench(activeMetric.targetQueue)">
        <Icon icon="ep:position" class="mr-5px" />去客户工作台
      </el-button>
    </div>

    <el-table v-if="detailMode === 'message'" v-loading="detailLoading" :data="messageRows" border>
      <el-table-column label="客户" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.contactName || `客户 #${row.contactId}` }}
        </template>
      </el-table-column>
      <el-table-column label="方向" width="90">
        <template #default="{ row }">
          <el-tag effect="plain" :type="row.direction === 2 ? 'success' : 'info'">
            {{ row.direction === 2 ? '发出' : '收到' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.content || getMessageTypeLabel(row.messageType) }}
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ row.messageTime ? formatDate(row.messageTime) : '' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openConversationByContact(row.contactId)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else-if="detailMode === 'review'" v-loading="detailLoading" :data="reviewRows" border>
      <el-table-column label="决策类型" width="110">
        <template #default="{ row }">
          <el-tag effect="plain">{{ getDecisionTypeLabel(row.decisionType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建议内容" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.suggestedContent || '暂无可审核的 AI 建议' }}
        </template>
      </el-table-column>
      <el-table-column label="原因" min-width="180" show-overflow-tooltip prop="decisionReason" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ row.createTime ? formatDate(row.createTime) : '' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReviewRow(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else v-loading="detailLoading" :data="riskRows" border>
      <el-table-column label="客户" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatContactName(row) }}
        </template>
      </el-table-column>
      <el-table-column label="风险" width="90">
        <template #default="{ row }">
          <el-tag :type="row.riskLevel > 1 ? 'danger' : 'warning'" effect="plain">
            {{ getRiskLabel(row.riskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getConversationStatusType(row.lastConversationStatus)" effect="plain">
            {{ getConversationStatusLabel(row.lastConversationStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近消息" width="170">
        <template #default="{ row }">
          {{ row.lastMessageTime ? formatDate(row.lastMessageTime) : '' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openConversationByContact(row.contactId)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="detailQuery.pageSize"
      v-model:page="detailQuery.pageNo"
      :total="detailTotal"
      @pagination="loadMetricDetail"
    />
  </el-drawer>
</template>

<script lang="ts" setup>
import * as ConversationApi from '@/api/agent/conversation'
import * as ReviewApi from '@/api/agent/review'
import * as StatisticsApi from '@/api/agent/statistics'

defineOptions({ name: 'AgentStatistics' })

type MetricKey = 'todayMessageCount' | 'todayAutoReplyCount' | 'pendingReviewCount' | 'riskConversationCount'
type DetailMode = 'message' | 'review' | 'risk'
type QueueType = 'RISK' | 'PENDING_REVIEW'

interface MetricItem {
  key: MetricKey
  label: string
  value: number
  icon: string
  hint: string
  detailTitle: string
  description: string
  mode: DetailMode
  scope?: string
  targetQueue?: QueueType
}

const router = useRouter()
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTotal = ref(0)
const activeMetric = ref<MetricItem>()
const messageRows = ref<StatisticsApi.AgentStatisticsMessageVO[]>([])
const reviewRows = ref<ReviewApi.AgentReplyReviewVO[]>([])
const riskRows = ref<ConversationApi.AgentConversationContactVO[]>([])
const detailQuery = reactive({
  pageNo: 1,
  pageSize: 10
})

const summary = ref<StatisticsApi.AgentStatisticsSummaryVO>({
  todayMessageCount: 0,
  todayAutoReplyCount: 0,
  pendingReviewCount: 0,
  riskConversationCount: 0,
  purchaseIntentionStats: [],
  salesStageStats: [],
  customerSentimentStats: [],
  followUpPriorityStats: []
})

const metrics = computed<MetricItem[]>(() => [
  {
    key: 'todayMessageCount',
    label: '今日消息',
    value: summary.value.todayMessageCount || 0,
    icon: 'ep:chat-dot-round',
    hint: '查看今天进入系统的消息',
    detailTitle: '今日消息明细',
    description: '按消息时间倒序展示今天收到和发出的全部消息。',
    mode: 'message',
    scope: 'TODAY_MESSAGES'
  },
  {
    key: 'todayAutoReplyCount',
    label: '今日自动回复',
    value: summary.value.todayAutoReplyCount || 0,
    icon: 'ep:promotion',
    hint: '查看今天自动发出的回复',
    detailTitle: '今日自动回复明细',
    description: '只展示今天由 AI 自动发送成功的回复。',
    mode: 'message',
    scope: 'TODAY_AUTO_REPLY'
  },
  {
    key: 'pendingReviewCount',
    label: '待审核回复',
    value: summary.value.pendingReviewCount || 0,
    icon: 'ep:finished',
    hint: '进入待审核回复队列',
    detailTitle: '待审核回复',
    description: '需要人工确认后才能发送的 AI 回复建议。',
    mode: 'review',
    targetQueue: 'PENDING_REVIEW'
  },
  {
    key: 'riskConversationCount',
    label: '需人工处理',
    value: summary.value.riskConversationCount || 0,
    icon: 'ep:warning',
    hint: '查看风险和人工接管会话',
    detailTitle: '需人工处理的会话',
    description: '包含高风险、待确认和人工接管中的会话。',
    mode: 'risk',
    targetQueue: 'RISK'
  }
])

const detailMode = computed(() => activeMetric.value?.mode)

const purchaseIntentionLabels: Record<string, string> = {
  LOW: '低意愿',
  MEDIUM: '中意愿',
  HIGH: '高意愿',
  STRONG: '强烈意愿',
  UNKNOWN: '未识别'
}

const salesStageLabels: Record<string, string> = {
  NEW_LEAD: '新线索',
  NEEDS_CONFIRMED: '需求确认',
  PRODUCT_INTRO: '产品介绍',
  QUOTE_NEGOTIATION: '报价议价',
  DEAL_PROGRESS: '成交推进',
  AFTER_SALES: '售后咨询',
  UNKNOWN: '未识别'
}

const customerSentimentLabels: Record<string, string> = {
  POSITIVE: '正向',
  NEUTRAL: '中性',
  NEGATIVE: '负向',
  UNKNOWN: '未识别'
}

const followUpPriorityLabels: Record<string, string> = {
  NORMAL: '普通跟进',
  FOCUS: '重点跟进',
  URGENT: '紧急跟进',
  UNKNOWN: '未识别'
}

const sortStats = (items?: StatisticsApi.AgentStatisticsDimensionVO[]) =>
  [...(items || [])].sort((a, b) => (b.count || 0) - (a.count || 0))

const sumStats = (items: StatisticsApi.AgentStatisticsDimensionVO[]) =>
  items.reduce((total, item) => total + (item.count || 0), 0)

const insightCards = computed(() => {
  const purchaseItems = sortStats(summary.value.purchaseIntentionStats)
  const stageItems = sortStats(summary.value.salesStageStats)
  const sentimentItems = sortStats(summary.value.customerSentimentStats)
  const priorityItems = sortStats(summary.value.followUpPriorityStats)
  return [
    {
      key: 'purchaseIntention',
      title: '购买意愿',
      items: purchaseItems,
      total: sumStats(purchaseItems),
      color: 'var(--el-color-warning)',
      formatLabel: (code: string) => purchaseIntentionLabels[code] || code
    },
    {
      key: 'salesStage',
      title: '销售阶段',
      items: stageItems,
      total: sumStats(stageItems),
      color: 'var(--el-color-primary)',
      formatLabel: (code: string) => salesStageLabels[code] || code
    },
    {
      key: 'customerSentiment',
      title: '客户情绪',
      items: sentimentItems,
      total: sumStats(sentimentItems),
      color: 'var(--el-color-success)',
      formatLabel: (code: string) => customerSentimentLabels[code] || code
    },
    {
      key: 'followUpPriority',
      title: '跟进优先级',
      items: priorityItems,
      total: sumStats(priorityItems),
      color: 'var(--el-color-danger)',
      formatLabel: (code: string) => followUpPriorityLabels[code] || code
    }
  ]
})

const getDimensionPercent = (count: number, total: number) => {
  if (!total) return 0
  return Math.round((count / total) * 100)
}

const loadSummary = async () => {
  loading.value = true
  try {
    summary.value = await StatisticsApi.getStatisticsSummary()
  } finally {
    loading.value = false
  }
}

const openMetricDetail = async (metric: MetricItem) => {
  activeMetric.value = metric
  detailQuery.pageNo = 1
  detailTotal.value = 0
  messageRows.value = []
  reviewRows.value = []
  riskRows.value = []
  detailVisible.value = true
  await loadMetricDetail()
}

const loadMetricDetail = async () => {
  if (!activeMetric.value) return
  detailLoading.value = true
  try {
    if (activeMetric.value.mode === 'message') {
      const data = await StatisticsApi.getStatisticsMessagePage({
        ...detailQuery,
        scope: activeMetric.value.scope || 'TODAY_MESSAGES'
      })
      messageRows.value = data.list
      detailTotal.value = data.total
      return
    }
    if (activeMetric.value.mode === 'review') {
      const data = await ReviewApi.getReviewPage({
        ...detailQuery,
        reviewStatus: 'PENDING'
      })
      reviewRows.value = data.list
      detailTotal.value = data.total
      return
    }
    const data = await ConversationApi.getConversationContactPage({
      ...detailQuery,
      queueType: 'RISK'
    })
    riskRows.value = data.list
    detailTotal.value = data.total
  } finally {
    detailLoading.value = false
  }
}

const openWorkbench = (queueType: QueueType) => {
  router.push({
    path: '/agent/conversation',
    query: {
      queueType
    }
  })
}

const openReviewRow = (row: ReviewApi.AgentReplyReviewVO) => {
  if (row.contactId) {
    router.push({
      path: '/agent/conversation',
      query: {
        queueType: 'PENDING_REVIEW',
        contactId: row.contactId
      }
    })
    return
  }
  openWorkbench('PENDING_REVIEW')
}

const openConversationByContact = (contactId?: number) => {
  if (!contactId) return
  router.push({
    path: '/agent/conversation',
    query: {
      contactId
    }
  })
}

const formatContactName = (row: ConversationApi.AgentConversationContactVO) => {
  return row.displayName || row.remark || row.nickname || row.wechatId || row.externalUserId || `客户 #${row.contactId}`
}

const getMessageTypeLabel = (messageType: number) => {
  const labels: Record<number, string> = {
    1: '[文本]',
    3: '[图片]',
    34: '[语音]',
    43: '[视频]',
    47: '[表情]',
    49: '[链接/文件]'
  }
  return labels[messageType] || '[未知消息]'
}

const getDecisionTypeLabel = (decisionType?: string) => {
  const labels: Record<string, string> = {
    AUTO_REPLY: '自动回复',
    MANUAL_CONFIRM: '人工确认',
    HUMAN_TAKEOVER: '人工接管',
    RECORD_ONLY: '仅记录'
  }
  return labels[decisionType || ''] || '未知'
}

const getRiskLabel = (riskLevel?: number) => {
  if (!riskLevel) return '低'
  if (riskLevel >= 2) return '高'
  return '中'
}

const getConversationStatusLabel = (status?: number) => {
  const labels: Record<number, string> = {
    0: '正常跟进',
    1: '自动回复',
    2: '待确认',
    3: '人工接管',
    4: '已关闭'
  }
  return labels[status ?? 0] || '未知'
}

const getConversationStatusType = (status?: number) => {
  const types: Record<number, 'success' | 'primary' | 'warning' | 'danger' | 'info'> = {
    0: 'success',
    1: 'primary',
    2: 'warning',
    3: 'danger',
    4: 'info'
  }
  return types[status ?? 0] || 'info'
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped>
.toolbar,
.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-title,
.section-title,
.detail-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.detail-toolbar {
  margin-bottom: 14px;
}

.detail-subtitle {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.metric-grid,
.insight-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-grid {
  min-height: 104px;
}

.insight-grid {
  min-height: 220px;
}

.metric-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 112px;
  padding: 16px;
  text-align: left;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.metric-item:hover,
.metric-item:focus-visible {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 8px 20px rgb(30 79 160 / 10%);
  transform: translateY(-1px);
  outline: none;
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--el-color-primary);
  font-size: 22px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
}

.metric-body {
  min-width: 0;
}

.metric-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.metric-value {
  margin-top: 4px;
  color: var(--el-text-color-primary);
  font-size: 26px;
  font-weight: 600;
  line-height: 32px;
}

.metric-hint {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.metric-arrow {
  position: absolute;
  right: 14px;
  bottom: 14px;
  color: var(--el-color-primary);
  font-size: 16px;
  opacity: 0.72;
}

.insight-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.insight-title {
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.dimension-row {
  margin-bottom: 12px;
}

.dimension-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.dimension-line strong {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.dimension-track {
  height: 6px;
  margin-top: 6px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--el-fill-color-light);
}

.dimension-fill {
  height: 100%;
  min-width: 2px;
  border-radius: inherit;
}

.empty-dimension {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 1200px) {
  .metric-grid,
  .insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
