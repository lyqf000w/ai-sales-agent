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
        <el-input-number v-model="queryParams.wechatAccountId" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="客户编号" prop="contactId">
        <el-input-number v-model="queryParams.contactId" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="全部">
          <el-option label="待确认" :value="2" />
          <el-option label="人工接管" :value="3" />
          <el-option label="已关闭" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" class="!w-160px" clearable placeholder="全部">
          <el-option label="黄色" :value="1" />
          <el-option label="红色" :value="2" />
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
      <el-table-column label="会话编号" align="center" prop="id" width="100" />
      <el-table-column label="Agent" align="center" prop="agentId" width="100" />
      <el-table-column label="微信账号" align="center" prop="wechatAccountId" width="110" />
      <el-table-column label="客户编号" align="center" prop="contactId" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template #default="scope">
          <el-tag :type="getStatus(scope.row.status).type" effect="plain">
            {{ getStatus(scope.row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险" align="center" prop="riskLevel" width="100">
        <template #default="scope">
          <el-tag :type="getRiskLevel(scope.row.riskLevel).type" effect="plain">
            {{ getRiskLevel(scope.row.riskLevel).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="最近消息"
        align="center"
        prop="lastMessageTime"
        min-width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="自动回复次数" align="center" prop="continuousAutoReplyCount" width="120" />
      <el-table-column label="接管人" align="center" prop="humanTakeoverUserId" width="100" />
      <el-table-column
        label="接管时间"
        align="center"
        prop="humanTakeoverTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openMessages(scope.row)">查看</el-button>
          <el-button
            link
            type="warning"
            :disabled="scope.row.status === 3"
            @click="takeoverConversation(scope.row.id)"
          >
            接管
          </el-button>
          <el-button link type="success" @click="closeConversation(scope.row.id)">关闭</el-button>
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

  <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px">
    <div v-loading="messageLoading" class="message-list">
      <el-empty v-if="messages.length === 0" description="暂无消息" />
      <div
        v-for="item in messages"
        :key="item.id"
        class="message-row"
        :class="{ outbound: item.direction === 2 }"
      >
        <div class="message-meta">
          <span>{{ getSenderType(item.senderType) }}</span>
          <span>{{ item.messageTime ? formatDate(item.messageTime) : '' }}</span>
        </div>
        <div class="message-bubble">
          {{ item.content || getMessageTypeLabel(item.messageType) }}
        </div>
        <div class="message-extra">
          <el-tag v-if="item.direction === 2" :type="getSendStatus(item.sendStatus).type" size="small" effect="plain">
            {{ getSendStatus(item.sendStatus).label }}
          </el-tag>
          <span v-if="item.geweMessageId">Gewe: {{ item.geweMessageId }}</span>
          <span v-if="item.matchedPolicy">规则: {{ item.matchedPolicy }}</span>
          <span v-if="item.auditNote">{{ item.auditNote }}</span>
        </div>
      </div>
    </div>
    <div class="drawer-actions">
      <el-button :disabled="!activeConversationId" type="warning" @click="takeoverActiveConversation">接管</el-button>
      <el-button :disabled="!activeConversationId" type="success" @click="closeActiveConversation">关闭</el-button>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as RiskApi from '@/api/agent/risk'

defineOptions({ name: 'AgentRisk' })

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const message = useMessage()
const loading = ref(true)
const total = ref(0)
const list = ref<RiskApi.AgentRiskConversationVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  wechatAccountId: undefined,
  contactId: undefined,
  status: undefined,
  riskLevel: undefined
})

const statusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '打开', type: 'success' },
  1: { label: '自动回复', type: 'primary' },
  2: { label: '待确认', type: 'warning' },
  3: { label: '人工接管', type: 'danger' },
  4: { label: '已关闭', type: 'info' }
}

const riskLevelMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '绿色', type: 'success' },
  1: { label: '黄色', type: 'warning' },
  2: { label: '红色', type: 'danger' }
}

const senderTypeMap: Record<number, string> = {
  1: '客户',
  2: 'AI',
  3: '人工',
  4: '系统'
}

const sendStatusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '已接收', type: 'info' },
  1: { label: '待确认', type: 'warning' },
  2: { label: '已发送', type: 'success' },
  3: { label: '发送失败', type: 'danger' },
  4: { label: '已驳回', type: 'info' }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await RiskApi.getRiskPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const drawerVisible = ref(false)
const drawerTitle = ref('风险会话')
const messageLoading = ref(false)
const messages = ref<RiskApi.AgentMessageVO[]>([])
const activeConversationId = ref<number>()

const openMessages = async (row: RiskApi.AgentRiskConversationVO) => {
  drawerVisible.value = true
  drawerTitle.value = `风险会话 #${row.id} · 客户 #${row.contactId}`
  activeConversationId.value = row.id
  await loadMessages(row.id)
}

const loadMessages = async (conversationId: number) => {
  messageLoading.value = true
  try {
    messages.value = await RiskApi.getRiskMessages(conversationId)
  } finally {
    messageLoading.value = false
  }
}

const takeoverConversation = async (conversationId: number) => {
  try {
    await message.confirm('确认将该会话切换为人工接管吗？')
    await RiskApi.takeoverRiskConversation(conversationId)
    message.success('已接管')
    if (activeConversationId.value === conversationId) await loadMessages(conversationId)
    await getList()
  } catch {}
}

const closeConversation = async (conversationId: number) => {
  try {
    await message.confirm('确认关闭该风险会话吗？')
    await RiskApi.closeRiskConversation(conversationId)
    message.success('已关闭')
    if (activeConversationId.value === conversationId) await loadMessages(conversationId)
    await getList()
  } catch {}
}

const takeoverActiveConversation = async () => {
  if (activeConversationId.value) await takeoverConversation(activeConversationId.value)
}

const closeActiveConversation = async () => {
  if (activeConversationId.value) await closeConversation(activeConversationId.value)
}

const getMessageTypeLabel = (messageType: number) => {
  const labels: Record<number, string> = {
    1: '[文本消息]',
    3: '[图片消息]',
    34: '[语音消息]',
    43: '[视频消息]',
    49: '[文件或链接]'
  }
  return labels[messageType] || '[未知消息]'
}

const getStatus = (status?: number) => {
  return statusMap[status ?? -1] || { label: '未知', type: 'info' }
}

const getRiskLevel = (riskLevel?: number) => {
  return riskLevelMap[riskLevel ?? 0] || riskLevelMap[0]
}

const getSenderType = (senderType?: number) => {
  return senderTypeMap[senderType ?? -1] || '未知'
}

const getSendStatus = (sendStatus?: number) => {
  return sendStatusMap[sendStatus ?? 0] || sendStatusMap[0]
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.message-list {
  min-height: calc(100% - 52px);
}

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 16px;
}

.message-row.outbound {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message-bubble {
  max-width: 82%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.outbound .message-bubble {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.message-extra {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 82%;
  margin-top: 4px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  word-break: break-all;
}

.drawer-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  background: var(--el-bg-color);
}
</style>
