<template>
  <ContentWrap>
    <div class="toolbar">
      <div class="toolbar-title">链路诊断</div>
      <el-button :loading="summaryLoading || tableLoading" type="primary" plain @click="refreshAll">
        <Icon icon="ep:refresh" class="mr-5px" />刷新
      </el-button>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div v-loading="summaryLoading" class="status-grid">
      <div class="status-item">
        <div class="status-label">DeepSeek</div>
        <div class="status-main">
          <el-tag :type="deepSeekReady ? 'success' : 'danger'" effect="plain">
            {{ deepSeekReady ? '可用配置' : '配置不完整' }}
          </el-tag>
          <span>{{ summary.deepSeekModel || '-' }}</span>
        </div>
        <div class="status-meta">{{ summary.deepSeekUrl || '-' }}</div>
      </div>
      <div class="status-item">
        <div class="status-label">旧 Python 桥接</div>
        <div class="status-main">
          <el-tag :type="summary.pythonBackendEnabled ? 'warning' : 'info'" effect="plain">
            {{ summary.pythonBackendEnabled ? '已启用' : '未启用' }}
          </el-tag>
          <span>{{ summary.pythonBackendBaseUrl || '-' }}</span>
        </div>
        <div class="status-meta">扫码登录若仍走桥接，需要同步验证旧服务节点</div>
      </div>
      <div class="status-item">
        <div class="status-label">微信账号</div>
        <div class="status-main">
          <strong>{{ summary.onlineWechatAccountCount || 0 }}</strong>
          <span>/ {{ summary.wechatAccountCount || 0 }} 在线</span>
        </div>
        <div class="status-meta">GeWe 凭证 {{ summary.enabledGeweCredentialCount || 0 }} / {{ summary.geweCredentialCount || 0 }}</div>
      </div>
      <div class="status-item">
        <div class="status-label">待处理</div>
        <div class="status-main">
          <strong>{{ summary.pendingReviewCount || 0 }}</strong>
          <span>待审核，{{ summary.riskConversationCount || 0 }} 风险会话</span>
        </div>
        <div class="status-meta">最近回复 {{ summary.lastReplyDecisionTime ? formatDate(summary.lastReplyDecisionTime) : '-' }}</div>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div v-loading="summaryLoading" class="metric-grid">
      <div class="metric-item">
        <span>今日回调</span>
        <strong>{{ summary.todayWebhookCount || 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>失败回调</span>
        <strong>{{ summary.failedWebhookCount || 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>DeepSeek 生成</span>
        <strong>{{ summary.recentDeepSeekReplyCount || 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>知识库生成</span>
        <strong>{{ summary.recentKnowledgeReplyCount || 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>兜底生成</span>
        <strong>{{ summary.recentFallbackReplyCount || 0 }}</strong>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div class="table-toolbar">
      <el-form ref="queryFormRef" :inline="true" :model="queryParams" label-width="80px">
        <el-form-item label="微信账号" prop="wechatAccountId">
          <el-input-number v-model="queryParams.wechatAccountId" :min="1" class="!w-160px" controls-position="right" />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-input v-model="queryParams.eventType" class="!w-160px" clearable />
        </el-form-item>
        <el-form-item label="处理状态" prop="processStatus">
          <el-select v-model="queryParams.processStatus" class="!w-150px" clearable placeholder="全部">
            <el-option label="新建" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="重复" :value="2" />
            <el-option label="失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="tableLoading" :data="webhookList" row-key="id">
      <el-table-column label="事件" min-width="230">
        <template #default="scope">
          <div class="event-cell">
            <span>{{ getEventTitle(scope.row) }}</span>
            <el-text truncated>{{ getEventTargetName(scope.row) }}</el-text>
            <el-text v-if="scope.row.groupMemberDisplayName" truncated type="info">
              {{ getGroupMemberName(scope.row) }}
            </el-text>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="账号/对象" min-width="180">
        <template #default="scope">
          <div class="event-cell">
            <span>{{ scope.row.wechatAccountName || `账号 #${scope.row.wechatAccountId || '-'}` }}</span>
            <el-text truncated>{{ getObjectDisplayName(scope.row) }}</el-text>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="110">
        <template #default="scope">
          <el-tag :type="getWebhookStatus(scope.row.processStatus).type" effect="plain">
            {{ getWebhookStatus(scope.row.processStatus).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="签名" align="center" width="92">
        <template #default="scope">
          <el-tag :type="scope.row.signatureValid === false ? 'danger' : 'success'" effect="plain">
            {{ scope.row.signatureValid === false ? '异常' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误" prop="errorMessage" min-width="220" show-overflow-tooltip />
      <el-table-column label="时间" align="center" prop="createTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openWebhookDetail(scope.row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="loadWebhookPage"
    />
  </ContentWrap>

  <el-drawer v-model="detailVisible" title="回调详情" size="680px">
    <div v-if="activeEvent" class="detail-stack">
      <div class="detail-row">
        <span>事件编号</span>
        <strong>{{ activeEvent.eventId || '-' }}</strong>
      </div>
      <div class="detail-row">
        <span>事件类型</span>
        <strong>{{ activeEvent.eventTypeName || activeEvent.eventType || '-' }}</strong>
      </div>
      <div class="detail-row">
        <span>客户/群聊</span>
        <strong>{{ getObjectDisplayName(activeEvent) }}</strong>
      </div>
      <div v-if="activeEvent.groupMemberDisplayName" class="detail-row">
        <span>群成员</span>
        <strong>{{ activeEvent.groupMemberDisplayName }}</strong>
      </div>
      <div class="detail-row">
        <span>微信账号</span>
        <strong>{{ activeEvent.wechatAccountName || activeEvent.wechatAccountId || '-' }}</strong>
      </div>
      <div class="detail-row">
        <span>错误信息</span>
        <strong>{{ activeEvent.errorMessage || '-' }}</strong>
      </div>
      <pre class="payload-box">{{ formatPayload(activeEvent.rawPayload) }}</pre>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as DiagnosticsApi from '@/api/agent/diagnostics'

defineOptions({ name: 'AgentDiagnostics' })

const summaryLoading = ref(false)
const tableLoading = ref(false)
const total = ref(0)
const queryFormRef = ref()
const webhookList = ref<DiagnosticsApi.AgentWebhookEventVO[]>([])
const activeEvent = ref<DiagnosticsApi.AgentWebhookEventVO>()
const detailVisible = ref(false)

const summary = ref<DiagnosticsApi.AgentDiagnosticsSummaryVO>({
  deepSeekEnabled: false,
  deepSeekApiKeyConfigured: false,
  pythonBackendEnabled: false,
  wechatAccountCount: 0,
  onlineWechatAccountCount: 0,
  geweCredentialCount: 0,
  enabledGeweCredentialCount: 0,
  todayWebhookCount: 0,
  failedWebhookCount: 0,
  pendingReviewCount: 0,
  riskConversationCount: 0,
  recentDeepSeekReplyCount: 0,
  recentKnowledgeReplyCount: 0,
  recentFallbackReplyCount: 0,
  recentGenerations: []
})

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  wechatAccountId: undefined as number | undefined,
  eventType: '',
  processStatus: undefined as number | undefined
})

const deepSeekReady = computed(() => {
  return summary.value.deepSeekEnabled && summary.value.deepSeekApiKeyConfigured
})

const isGroupEvent = (row?: DiagnosticsApi.AgentWebhookEventVO) => {
  return !!row?.groupDisplayName || !!row?.contactWxid?.endsWith('@chatroom')
}

const getEventTitle = (row: DiagnosticsApi.AgentWebhookEventVO) => {
  if (isGroupEvent(row) && row.groupDisplayName) {
    return row.groupDisplayName
  }
  return row.eventTypeName || row.eventType || '-'
}

const getEventTargetName = (row: DiagnosticsApi.AgentWebhookEventVO) => {
  if (isGroupEvent(row)) {
    return row.groupDisplayName ? row.groupDisplayName : '群聊名称待同步'
  }
  return row.contactDisplayName || row.eventSummary || '-'
}

const getObjectDisplayName = (row?: DiagnosticsApi.AgentWebhookEventVO) => {
  if (!row) {
    return '-'
  }
  if (isGroupEvent(row)) {
    return row.groupDisplayName ? row.groupDisplayName : '群聊名称待同步'
  }
  return row.contactDisplayName ? `客户 ${row.contactDisplayName}` : '-'
}

const getGroupMemberName = (row: DiagnosticsApi.AgentWebhookEventVO) => {
  return row.groupMemberDisplayName ? `成员 ${row.groupMemberDisplayName}` : ''
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    summary.value = await DiagnosticsApi.getDiagnosticsSummary()
  } finally {
    summaryLoading.value = false
  }
}

const loadWebhookPage = async () => {
  tableLoading.value = true
  try {
    const data = await DiagnosticsApi.getWebhookEventPage(queryParams)
    webhookList.value = data.list
    total.value = data.total
  } finally {
    tableLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadSummary(), loadWebhookPage()])
}

const handleQuery = () => {
  queryParams.pageNo = 1
  loadWebhookPage()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  loadWebhookPage()
}

const openWebhookDetail = async (id: number) => {
  activeEvent.value = await DiagnosticsApi.getWebhookEvent(id)
  detailVisible.value = true
}

const formatPayload = (payload?: Record<string, any>) => {
  return payload ? JSON.stringify(payload, null, 2) : '{}'
}

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const getWebhookStatus = (status?: number): { label: string; type: TagType } => {
  const map: Record<number, { label: string; type: TagType }> = {
    0: { label: '新建', type: 'info' },
    1: { label: '已处理', type: 'success' },
    2: { label: '重复', type: 'warning' },
    3: { label: '失败', type: 'danger' }
  }
  return map[status ?? 0] || map[0]
}

onMounted(refreshAll)
</script>

<style scoped>
.toolbar,
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-height: 116px;
}

.status-item,
.metric-item {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.status-label,
.metric-item span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.status-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: var(--el-text-color-primary);
  line-height: 24px;
}

.status-main strong {
  font-size: 24px;
}

.status-meta {
  margin-top: 8px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-item strong {
  color: var(--el-text-color-primary);
  font-size: 22px;
  line-height: 28px;
}

.event-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.event-cell span {
  color: var(--el-text-color-primary);
  font-weight: 500;
  line-height: 20px;
}

.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 12px;
  font-size: 13px;
  line-height: 22px;
}

.detail-row span {
  color: var(--el-text-color-secondary);
}

.detail-row strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.payload-box {
  max-height: 520px;
  padding: 12px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
  color: var(--el-text-color-primary);
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
}

@media (max-width: 1200px) {
  .status-grid,
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .status-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
