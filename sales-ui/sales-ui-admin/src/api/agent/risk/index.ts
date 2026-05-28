import request from '@/config/axios'
import type { AgentMessageVO } from '@/api/agent/conversation'

export interface AgentRiskConversationVO {
  id: number
  agentId?: number
  wechatAccountId: number
  contactId: number
  status: number
  riskLevel: number
  lastMessageId?: number
  lastMessageTime?: Date
  continuousAutoReplyCount?: number
  humanTakeoverUserId?: number
  humanTakeoverTime?: Date
  createTime?: Date
}

export const getRiskPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/risk/page', params })
}

export const getRiskMessages = async (conversationId: number) => {
  return await request.get({ url: '/agent/risk/messages', params: { conversationId } })
}

export const takeoverRiskConversation = async (conversationId: number) => {
  return await request.post({ url: '/agent/risk/takeover', data: { conversationId } })
}

export const closeRiskConversation = async (conversationId: number) => {
  return await request.post({ url: '/agent/risk/close', data: { conversationId } })
}

export type { AgentMessageVO }
