import request from '@/config/axios'

export interface AgentWechatContactVO {
  id: number
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
  purchaseIntention?: string
  salesStage?: string
  customerSentiment?: string
  followUpPriority?: string
  quietSeconds?: number | null
  businessHours?: {
    start?: string
    end?: string
  } | null
  createTime?: Date
}

export interface AgentWechatContactReplyPolicyReqVO {
  id: number
  replyMode?: string | null
  quietSeconds?: number | null
  businessHours?: {
    start?: string
    end?: string
  } | null
}

export interface AgentWechatContactSalesInsightReqVO {
  id: number
  purchaseIntention?: string
  salesStage?: string
  customerSentiment?: string
  followUpPriority?: string
}

export interface AgentWechatContactSyncRespVO {
  accountCount: number
  fetchedCount: number
  createdCount: number
  updatedCount: number
  skippedCount: number
}

export const getWechatContactPage = async (params: PageParam & Record<string, any>) => {
  return await request.get({ url: '/agent/contact/page', params })
}

export const syncWechatContacts = async (wechatAccountId?: number): Promise<AgentWechatContactSyncRespVO> => {
  return await request.post({ url: '/agent/contact/sync', params: { wechatAccountId } })
}

export const updateWechatContactLevel = async (id: number, customerLevel: number) => {
  return await request.put({ url: '/agent/contact/update-level', data: { id, customerLevel } })
}

export const updateWechatContactOwner = async (id: number, ownerUserId?: number) => {
  return await request.put({ url: '/agent/contact/update-owner', data: { id, ownerUserId } })
}

export const updateWechatContactReplyPolicy = async (data: AgentWechatContactReplyPolicyReqVO) => {
  return await request.put({ url: '/agent/contact/update-reply-policy', data })
}

export const updateWechatContactSalesInsight = async (data: AgentWechatContactSalesInsightReqVO) => {
  return await request.put({ url: '/agent/contact/update-sales-insight', data })
}

export const getWechatContactTags = async (contactId: number) => {
  return await request.get({ url: '/agent/contact/tags', params: { contactId } })
}

export const updateWechatContactTags = async (contactId: number, tagIds: number[]) => {
  return await request.put({ url: '/agent/contact/tags', data: { contactId, tagIds } })
}
