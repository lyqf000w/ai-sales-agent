import request from '@/config/axios'

export interface AgentConversationVO {
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

export interface AgentConversationContactVO {
  id: number
  contactId: number
  conversationId: number
  agentId?: number
  wechatAccountId: number
  externalUserId: string
  wechatId?: string
  nickname?: string
  displayName?: string
  remark?: string
  avatar?: string
  customerLevel: number
  ownerUserId?: number
  riskLevel: number
  lastMessageTime?: Date
  lastConversationStatus?: number
  replyMode?: string | null
  quietSeconds?: number | null
  businessHours?: {
    start?: string
    end?: string
  } | null
  purchaseIntention?: string
  salesStage?: string
  customerSentiment?: string
  followUpPriority?: string
  status: number
  lastMessageId?: number
  continuousAutoReplyCount?: number
  humanTakeoverUserId?: number
  humanTakeoverTime?: Date
  createTime?: Date
}

export interface AgentMessageVO {
  id: number
  conversationId: number
  direction: number
  senderType: number
  senderDisplayName?: string
  messageType: number
  content?: string
  mediaUrl?: string
  mediaAesKey?: string
  thumbUrl?: string
  mediaName?: string
  mediaDurationMillis?: number
  geweMessageId?: string
  sendStatus: number
  intent?: string
  matchedPolicy?: string
  auditNote?: string
  operatorUserId?: number
  messageTime?: Date
  createTime?: Date
}

export interface AgentConversationContactSettingsSaveReqVO {
  conversationId: number
  contactId: number
  customerLevel?: number
  replyMode?: string | null
  quietSeconds?: number | null
  businessHours?: {
    start?: string
    end?: string
  } | null
  purchaseIntention?: string
  salesStage?: string
  customerSentiment?: string
  followUpPriority?: string
  tagIds?: number[]
}

export interface AgentConversationContactSettingsRespVO {
  contact: AgentConversationContactVO
  policy: {
    wechatAccountId?: number
    contactId?: number
    conversationId?: number
    replyMode?: string
    quietSeconds?: number
    businessHours?: {
      start?: string
      end?: string
    }
    source?: string
  }
  tagIds: number[]
}

export const getConversationPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/conversation/page', params })
}

export const getConversationContactPage = async (params: PageParam & Record<string, any>) => {
  return await request.get({ url: '/agent/conversation/contact-page', params })
}

export const getConversationMessages = async (conversationId: number) => {
  return await request.get({ url: '/agent/conversation/messages', params: { conversationId } })
}

export const downloadVoiceMedia = async (messageId: number): Promise<Blob> => {
  return await request.download({
    url: '/agent/conversation/voice-media',
    params: { messageId },
    headers: { skipErrorMessage: true }
  })
}

export const downloadGeweMedia = async (messageId: number): Promise<Blob> => {
  return await request.download({ url: '/agent/conversation/gewe-media', params: { messageId } })
}

export const sendMessage = async (conversationId: number, content: string) => {
  return await request.post({ url: '/agent/conversation/send-message', data: { conversationId, content } })
}

export const restoreOriginalPolicy = async (conversationId: number) => {
  return await request.post({ url: '/agent/conversation/restore-original-policy', data: { conversationId } })
}

export const acknowledgePending = async (conversationId: number): Promise<boolean> => {
  return await request.post({ url: '/agent/conversation/acknowledge-pending', data: { conversationId } })
}

export const saveContactSettings = async (
  data: AgentConversationContactSettingsSaveReqVO
): Promise<AgentConversationContactSettingsRespVO> => {
  return await request.post({ url: '/agent/conversation/contact-settings/save', data })
}

export const approveMessage = async (messageId: number, content?: string) => {
  return await request.post({ url: '/agent/conversation/approve-message', data: { messageId, content } })
}

export const rejectMessage = async (messageId: number, reason?: string) => {
  return await request.post({ url: '/agent/conversation/reject-message', data: { messageId, reason } })
}
