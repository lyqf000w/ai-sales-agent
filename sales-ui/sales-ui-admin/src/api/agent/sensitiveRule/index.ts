import request from '@/config/axios'

export interface AgentSensitiveRuleVO {
  id?: number
  name: string
  agentId?: number
  routeApp?: string
  matchType: number
  triggerType: string
  pattern: string
  action: number
  riskLevel: number
  sort: number
  status: number
  remark?: string
  createTime?: Date
}

export interface AgentSensitiveRuleOptionVO {
  value: string
  label: string
  description?: string
}

export interface AgentSensitiveRuleOptionsVO {
  triggerTypes: AgentSensitiveRuleOptionVO[]
  actions: AgentSensitiveRuleOptionVO[]
  riskLevels: AgentSensitiveRuleOptionVO[]
  intents: AgentSensitiveRuleOptionVO[]
  sentiments: AgentSensitiveRuleOptionVO[]
  customerLevels: AgentSensitiveRuleOptionVO[]
}

export const getSensitiveRulePage = async (params: PageParam) => {
  return await request.get({ url: '/agent/sensitive-rule/page', params })
}

export const getSensitiveRuleOptions = async (): Promise<AgentSensitiveRuleOptionsVO> => {
  return await request.get({ url: '/agent/sensitive-rule/options' })
}

export const getSensitiveRule = async (id: number) => {
  return await request.get({ url: '/agent/sensitive-rule/get?id=' + id })
}

export const createSensitiveRule = async (data: AgentSensitiveRuleVO) => {
  return await request.post({ url: '/agent/sensitive-rule/create', data })
}

export const updateSensitiveRule = async (data: AgentSensitiveRuleVO) => {
  return await request.put({ url: '/agent/sensitive-rule/update', data })
}

export const deleteSensitiveRule = async (id: number) => {
  return await request.delete({ url: '/agent/sensitive-rule/delete?id=' + id })
}
