import request from '@/config/axios'

export interface AgentReplyReviewVO {
  id: number
  conversationId: number
  contactId?: number
  inboundMessageId?: number
  suggestedMessageId?: number
  sentMessageId?: number
  decisionType: string
  riskLevel: number
  confidence?: number
  llmModel?: string
  generationSource?: string
  llmProvider?: string
  actualLlmModel?: string
  knowledgeRefs?: Record<string, any>
  guardrailHits?: Record<string, any>
  decisionReason?: string
  reviewStatus: string
  reviewNote?: string
  reviewUserId?: number
  reviewTime?: Date
  suggestedContent?: string
  matchedPolicy?: string
  auditNote?: string
  createTime?: Date
}

export const getReviewPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/review/page', params })
}

export const approveReview = async (decisionId: number, content?: string) => {
  return await request.post({ url: '/agent/review/approve', data: { decisionId, content } })
}

export const rejectReview = async (decisionId: number, reason: string) => {
  return await request.post({ url: '/agent/review/reject', data: { decisionId, reason } })
}
