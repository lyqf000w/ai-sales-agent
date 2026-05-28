import request from '@/config/axios'

export interface AgentStatisticsDimensionVO {
  code: string
  count: number
}

export interface AgentStatisticsSummaryVO {
  todayMessageCount: number
  todayAutoReplyCount: number
  pendingReviewCount: number
  riskConversationCount: number
  purchaseIntentionStats?: AgentStatisticsDimensionVO[]
  salesStageStats?: AgentStatisticsDimensionVO[]
  customerSentimentStats?: AgentStatisticsDimensionVO[]
  followUpPriorityStats?: AgentStatisticsDimensionVO[]
}

export interface AgentStatisticsMessageVO {
  id: number
  conversationId: number
  wechatAccountId: number
  contactId: number
  accountName?: string
  contactName?: string
  direction: number
  senderType: number
  messageType: number
  content?: string
  sendStatus: number
  intent?: string
  matchedPolicy?: string
  auditNote?: string
  messageTime?: Date
  createTime?: Date
}

export const getStatisticsSummary = async () => {
  return await request.get({ url: '/agent/statistics/summary' })
}

export const getStatisticsMessagePage = async (params: PageParam & { scope: string }) => {
  return await request.get({ url: '/agent/statistics/messages', params })
}
