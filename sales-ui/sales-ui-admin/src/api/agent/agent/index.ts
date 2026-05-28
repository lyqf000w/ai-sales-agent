import request from '@/config/axios'

export interface AgentVO {
  id?: number
  name: string
  aliasName?: string
  ownerUserId?: number
  scene?: string
  targetCustomerDesc?: string
  systemPrompt?: string
  llmProvider?: string
  llmModel?: string
  knowledgeBaseId?: number
  replyMode?: string
  quietSeconds?: number
  businessHours?: {
    start?: string
    end?: string
  }
  tone?: string
  welcomeMessage?: string
  handoverMessage?: string
  followUpPolicy?: {
    firstDelayMinutes?: number
    maxFollowUps?: number
  }
  materialPriority?: {
    primary?: string
    secondary?: string
  }
  status: number
  draftVersion?: number
  onlineVersion?: number
  publishedConfig?: Record<string, any>
  createTime?: Date
}

export interface AgentConfigVersionVO {
  id: number
  agentId: number
  version: number
  configSnapshot?: Record<string, any>
  changeSummary?: string
  publishUserId?: number
  publishTime?: Date
  createTime?: Date
}

export interface AgentSimpleVO {
  id: number
  name: string
  aliasName?: string
  scene?: string
  llmProvider?: string
  llmModel?: string
}

export interface AgentLlmModelOptionVO {
  provider: string
  model: string
  label: string
  defaultModel: boolean
}

export const getAgentPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/agent/page', params })
}

export const getSimpleAgentList = async (): Promise<AgentSimpleVO[]> => {
  return await request.get({ url: '/agent/agent/simple-list' })
}

export const getLlmModelOptions = async (): Promise<AgentLlmModelOptionVO[]> => {
  return await request.get({ url: '/agent/agent/llm-model-options' })
}

export const getAgent = async (id: number) => {
  return await request.get({ url: '/agent/agent/get?id=' + id })
}

export const createAgent = async (data: AgentVO) => {
  return await request.post({ url: '/agent/agent/create', data })
}

export const updateAgent = async (data: AgentVO) => {
  return await request.put({ url: '/agent/agent/update', data })
}

export const deleteAgent = async (id: number) => {
  return await request.delete({ url: '/agent/agent/delete?id=' + id })
}

export const publishAgent = async (agentId: number, changeSummary?: string) => {
  return await request.post({ url: '/agent/agent/publish', data: { agentId, changeSummary } })
}

export const getAgentVersions = async (agentId: number) => {
  return await request.get({ url: '/agent/agent/versions', params: { agentId } })
}
