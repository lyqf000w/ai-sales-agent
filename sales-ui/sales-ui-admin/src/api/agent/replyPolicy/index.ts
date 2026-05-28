import request from '@/config/axios'

export interface AgentReplyPolicyVO {
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

export interface AgentReplyPolicySaveReqVO {
  wechatAccountId?: number
  contactId?: number
  conversationId?: number
  replyMode?: string | null
  quietSeconds?: number | null
  businessHours?: {
    start?: string
    end?: string
  } | null
}

export const resolveReplyPolicy = async (params: {
  wechatAccountId?: number
  contactId?: number
  conversationId?: number
}): Promise<AgentReplyPolicyVO> => {
  return await request.get({ url: '/agent/reply-policy/resolve', params })
}

export const saveReplyPolicy = async (data: AgentReplyPolicySaveReqVO): Promise<AgentReplyPolicyVO> => {
  return await request.post({ url: '/agent/reply-policy/save', data })
}
