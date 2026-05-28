import request from '@/config/axios'

export interface AgentContactTagVO {
  id?: number
  name: string
  color?: string
  description?: string
  sort: number
  status: number
  createTime?: Date
}

export const getTagPage = async (params: PageParam) => {
  return await request.get({ url: '/agent/tag/page', params })
}

export const getSimpleTagList = async () => {
  return await request.get({ url: '/agent/tag/simple-list' })
}

export const getTag = async (id: number) => {
  return await request.get({ url: '/agent/tag/get?id=' + id })
}

export const createTag = async (data: AgentContactTagVO) => {
  return await request.post({ url: '/agent/tag/create', data })
}

export const updateTag = async (data: AgentContactTagVO) => {
  return await request.put({ url: '/agent/tag/update', data })
}

export const deleteTag = async (id: number) => {
  return await request.delete({ url: '/agent/tag/delete?id=' + id })
}
