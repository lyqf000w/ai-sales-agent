import request from '@/config/axios'

export interface AgentKnowledgeBaseVO {
  id?: number
  name: string
  description?: string
  status: number
  createTime?: Date
}

export interface AgentKnowledgeItemVO {
  id?: number
  knowledgeBaseId: number
  title: string
  productName?: string
  category?: string
  keywords?: string
  question?: string
  answer: string
  embeddingStatus?: string
  sort: number
  status: number
  createTime?: Date
}

export const getKnowledgeBasePage = async (params: PageParam) => {
  return await request.get({ url: '/agent/knowledge-base/page', params })
}

export const getSimpleKnowledgeBaseList = async (): Promise<AgentKnowledgeBaseVO[]> => {
  return await request.get({ url: '/agent/knowledge-base/simple-list' })
}

export const getKnowledgeBase = async (id: number) => {
  return await request.get({ url: '/agent/knowledge-base/get?id=' + id })
}

export const createKnowledgeBase = async (data: AgentKnowledgeBaseVO) => {
  return await request.post({ url: '/agent/knowledge-base/create', data })
}

export const updateKnowledgeBase = async (data: AgentKnowledgeBaseVO) => {
  return await request.put({ url: '/agent/knowledge-base/update', data })
}

export const deleteKnowledgeBase = async (id: number) => {
  return await request.delete({ url: '/agent/knowledge-base/delete?id=' + id })
}

export const getKnowledgeItemPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/knowledge/page', params })
}

export const getKnowledgeItem = async (id: number) => {
  return await request.get({ url: '/agent/knowledge/get?id=' + id })
}

export const createKnowledgeItem = async (data: AgentKnowledgeItemVO) => {
  return await request.post({ url: '/agent/knowledge/create', data })
}

export const updateKnowledgeItem = async (data: AgentKnowledgeItemVO) => {
  return await request.put({ url: '/agent/knowledge/update', data })
}

export const deleteKnowledgeItem = async (id: number) => {
  return await request.delete({ url: '/agent/knowledge/delete?id=' + id })
}

export const rebuildKnowledgeItemIndex = async (id: number) => {
  return await request.post({ url: '/agent/knowledge/rebuild-index?id=' + id })
}
