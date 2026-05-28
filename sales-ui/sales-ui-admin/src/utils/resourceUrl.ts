import { config } from '@/config/axios/config'

const LEGACY_FILE_HOSTS = new Set([
  'test.sales.iocoder.cn',
  'static.sales.iocoder.cn',
  'mall.sales.iocoder.cn'
])

const isLegacyFileHost = (host: string) => LEGACY_FILE_HOSTS.has(host.toLowerCase())

export const shouldProxyResourceUrl = (url?: string) => {
  if (!url) return false
  try {
    const parsed = new URL(url)
    return ['http:', 'https:'].includes(parsed.protocol) && isLegacyFileHost(parsed.hostname)
  } catch {
    return false
  }
}

export const normalizeRenderableResourceUrl = (url?: string) => {
  if (!url) return ''
  if (!shouldProxyResourceUrl(url)) return url
  const params = new URLSearchParams({ url })
  return `${config.base_url}/agent/conversation/media-proxy?${params.toString()}`
}
