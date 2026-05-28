import request from '@/config/axios'

export interface AgentDiagnosticsGenerationVO {
  decisionId: number
  conversationId: number
  generationSource?: string
  llmProvider?: string
  llmModel?: string
  reviewStatus?: string
  createTime?: Date
}

export interface AgentDiagnosticsSummaryVO {
  deepSeekEnabled: boolean
  deepSeekApiKeyConfigured: boolean
  deepSeekUrl?: string
  deepSeekModel?: string
  pythonBackendEnabled: boolean
  pythonBackendBaseUrl?: string
  wechatAccountCount: number
  onlineWechatAccountCount: number
  geweCredentialCount: number
  enabledGeweCredentialCount: number
  todayWebhookCount: number
  failedWebhookCount: number
  pendingReviewCount: number
  riskConversationCount: number
  recentDeepSeekReplyCount: number
  recentKnowledgeReplyCount: number
  recentFallbackReplyCount: number
  lastWebhookTime?: Date
  lastReplyDecisionTime?: Date
  recentGenerations?: AgentDiagnosticsGenerationVO[]
}

export interface AgentWebhookEventVO {
  id: number
  wechatAccountId?: number
  wechatAccountName?: string
  wechatId?: string
  contactWxid?: string
  contactDisplayName?: string
  groupDisplayName?: string
  groupMemberDisplayName?: string
  eventSummary?: string
  eventId?: string
  eventType?: string
  eventTypeName?: string
  signatureValid?: boolean
  processStatus?: number
  errorMessage?: string
  rawPayload?: Record<string, any>
  createTime?: Date
}

export const getDiagnosticsSummary = async (): Promise<AgentDiagnosticsSummaryVO> => {
  return await request.get({ url: '/agent/diagnostics/summary' })
}

export const getWebhookEventPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/diagnostics/webhook-event/page', params })
}

export const getWebhookEvent = async (id: number): Promise<AgentWebhookEventVO> => {
  return await request.get({ url: '/agent/diagnostics/webhook-event/get', params: { id } })
}
