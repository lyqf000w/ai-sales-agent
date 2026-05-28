import request from '@/config/axios'

export interface AgentWechatAccountVO {
  id?: number
  geweCredentialId?: number
  geweCredentialName?: string
  agentId?: number
  knowledgeBaseId?: number
  ownerUserId?: number
  geweAppId?: string
  geweAccountId?: string
  wechatId?: string
  nickname?: string
  avatar?: string
  mobile?: string
  callbackUrl?: string
  callbackSecret?: string
  geweApiBaseUrl?: string
  geweToken?: string
  geweTokenConfigured?: boolean
  replyMode?: string
  quietSeconds?: number
  businessHours?: {
    start?: string
    end?: string
  }
  loginStatus?: number
  status: number
  lastHeartbeatTime?: Date
  createTime?: Date
}

export interface AgentWechatBindSessionCreateReqVO {
  credentialId?: number
  agentId: number
  ownerUserId: number
}

export interface AgentWechatBindSessionVO {
  id: number
  credentialId: number
  agentId: number
  ownerUserId: number
  appId?: string
  uuid?: string
  qrData?: string
  qrImgBase64?: string
  verifyUrl?: string
  nickName?: string
  avatar?: string
  status: string
  expiresAt?: Date
  bindAccountId?: number
  errorMessage?: string
}

export const getWechatAccountPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/wechat-account/page', params })
}

export const getWechatAccount = async (id: number) => {
  return await request.get({ url: '/agent/wechat-account/get?id=' + id })
}

export const createWechatAccount = async (data: AgentWechatAccountVO) => {
  return await request.post({ url: '/agent/wechat-account/create', data })
}

export const updateWechatAccount = async (data: AgentWechatAccountVO) => {
  return await request.put({ url: '/agent/wechat-account/update', data })
}

export const deleteWechatAccount = async (id: number) => {
  return await request.delete({ url: '/agent/wechat-account/delete?id=' + id })
}

export const createBindSession = async (data: AgentWechatBindSessionCreateReqVO) => {
  return await request.post({ url: '/agent/wechat-account/bind-session/create', data })
}

export const getBindSession = async (id: number) => {
  return await request.get({ url: '/agent/wechat-account/bind-session/get?id=' + id })
}

export const checkBindSession = async (id: number) => {
  return await request.post({ url: '/agent/wechat-account/bind-session/check?id=' + id })
}
