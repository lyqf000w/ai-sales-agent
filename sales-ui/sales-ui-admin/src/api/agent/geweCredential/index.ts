import request from '@/config/axios'

export interface AgentGeweCredentialVO {
  id?: number
  name: string
  geweApiBaseUrl: string
  geweToken?: string
  geweTokenConfigured?: boolean
  callbackToken?: string
  callbackSecret?: string
  callbackUrl?: string
  callbackConfiguredTime?: Date
  status: number
  createTime?: Date
}

export const getCredentialList = async () => {
  return await request.get({ url: '/agent/gewe-credential/list' })
}

export const getEnabledCredentialList = async () => {
  return await request.get({ url: '/agent/gewe-credential/list-enabled' })
}

export const getCredential = async (id: number) => {
  return await request.get({ url: '/agent/gewe-credential/get?id=' + id })
}

export const saveCredential = async (data: AgentGeweCredentialVO) => {
  return await request.post({ url: '/agent/gewe-credential/save', data })
}

export const getDefaultCredential = async () => {
  return await request.get({ url: '/agent/gewe-credential/default' })
}

export const saveDefaultCredential = async (data: AgentGeweCredentialVO) => {
  return await request.post({ url: '/agent/gewe-credential/save-default', data })
}
