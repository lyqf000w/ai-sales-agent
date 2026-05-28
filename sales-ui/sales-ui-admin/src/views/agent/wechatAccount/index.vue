<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="78px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          class="!w-240px"
          clearable
          placeholder="昵称 / 微信号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="账号状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="全部">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="登录状态" prop="loginStatus">
        <el-select v-model="queryParams.loginStatus" class="!w-180px" clearable placeholder="全部">
          <el-option label="未知" :value="0" />
          <el-option label="在线" :value="1" />
          <el-option label="离线" :value="2" />
          <el-option label="失效" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="primary" @click="openBindDialog">
          <Icon icon="ep:connection" class="mr-5px" />绑定微信号
        </el-button>
        <el-button plain @click="openGeweCredential">
          <Icon icon="ep:setting" class="mr-5px" />GeWe 凭证
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="微信账号" min-width="180">
        <template #default="scope">
          <div class="account-cell">
            <el-avatar :size="34" :src="scope.row.avatar">
              {{ getInitial(scope.row.nickname || scope.row.wechatId) }}
            </el-avatar>
            <div>
              <div class="account-name">{{ scope.row.nickname || '-' }}</div>
              <div class="account-meta">{{ scope.row.wechatId || scope.row.geweAccountId || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="GeWe 凭证" min-width="150">
        <template #default="scope">
          <el-text truncated>{{ scope.row.geweCredentialName || '未绑定凭证' }}</el-text>
        </template>
      </el-table-column>
      <el-table-column label="Agent" min-width="150">
        <template #default="scope">
          <el-text truncated>{{ getAgentName(scope.row.agentId) }}</el-text>
        </template>
      </el-table-column>
      <el-table-column label="默认策略" min-width="170">
        <template #default="scope">
          <div class="policy-cell">
            <el-tag effect="plain">{{ getReplyModeLabel(scope.row.replyMode) }}</el-tag>
            <span>{{ formatAccountPolicy(scope.row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Gewe Appid" align="center" prop="geweAppId" width="180" />
      <el-table-column label="凭证 Token" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.geweTokenConfigured ? 'success' : 'info'" effect="plain">
            {{ scope.row.geweTokenConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="回调地址" min-width="300">
        <template #default="scope">
          <div class="callback-cell">
            <el-text truncated>{{ scope.row.callbackUrl || '-' }}</el-text>
            <el-button
              v-if="scope.row.callbackUrl"
              link
              type="primary"
              @click="copyCallback(scope.row.callbackUrl)"
            >
              复制
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="登录" align="center" prop="loginStatus" width="90">
        <template #default="scope">
          <el-tag :type="getLoginStatus(scope.row.loginStatus).type" effect="plain">
            {{ getLoginStatus(scope.row.loginStatus).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'info'" effect="plain">
            {{ scope.row.status === 0 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="最近同步"
        align="center"
        prop="lastHeartbeatTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm(scope.row.id)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <WechatAccountForm ref="formRef" @success="getList" />
  <WechatBindDialog ref="bindDialogRef" @success="getList" />
  <GeweCredentialForm ref="geweCredentialRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as WechatAccountApi from '@/api/agent/wechatAccount'
import * as AgentApi from '@/api/agent/agent'
import GeweCredentialForm from './GeweCredentialForm.vue'
import WechatBindDialog from './WechatBindDialog.vue'
import WechatAccountForm from './WechatAccountForm.vue'

defineOptions({ name: 'AgentWechatAccount' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const total = ref(0)
const list = ref<WechatAccountApi.AgentWechatAccountVO[]>([])
const agentList = ref<AgentApi.AgentSimpleVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  loginStatus: undefined
})

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const loginStatusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '未知', type: 'info' },
  1: { label: '在线', type: 'success' },
  2: { label: '离线', type: 'warning' },
  3: { label: '失效', type: 'danger' }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await WechatAccountApi.getWechatAccountPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const getAgentList = async () => {
  agentList.value = await AgentApi.getSimpleAgentList()
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formRef = ref()
const openForm = (id: number) => {
  formRef.value.open(id)
}

const bindDialogRef = ref()
const openBindDialog = () => {
  bindDialogRef.value.open()
}

const geweCredentialRef = ref()
const openGeweCredential = () => {
  geweCredentialRef.value.open()
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await WechatAccountApi.deleteWechatAccount(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const copyCallback = async (callbackUrl: string) => {
  const targetUrl = /^https?:\/\//i.test(callbackUrl)
    ? callbackUrl
    : `${window.location.origin}${callbackUrl.startsWith('/') ? callbackUrl : `/${callbackUrl}`}`
  await navigator.clipboard.writeText(targetUrl)
  message.success('已复制回调地址')
}

const getInitial = (text?: string) => {
  return text ? text.slice(0, 1) : '微'
}

const getLoginStatus = (loginStatus?: number) => {
  return loginStatusMap[loginStatus ?? 0] || loginStatusMap[0]
}

const getReplyModeLabel = (replyMode?: string) => {
  const labels: Record<string, string> = {
    MANUAL_CONFIRM: '人工确认',
    AUTO_REPLY: '自动回复',
    RECORD_ONLY: '仅记录'
  }
  return labels[replyMode || ''] || '人工确认'
}

const getAgentName = (agentId?: number) => {
  if (!agentId) {
    return '未绑定'
  }
  const agent = agentList.value.find((item) => item.id === agentId)
  return agent ? `${agent.name}${agent.aliasName ? `（${agent.aliasName}）` : ''}` : `Agent #${agentId}`
}

const formatAccountPolicy = (row: WechatAccountApi.AgentWechatAccountVO) => {
  const quietSeconds = getAccountQuietSeconds(row)
  const businessHours = row.businessHours
  const businessHoursText =
    businessHours?.start && businessHours?.end ? `营业 ${businessHours.start}-${businessHours.end}` : ''
  return [`静默 ${quietSeconds} 秒`, businessHoursText].filter(Boolean).join(' · ')
}

type LegacyAccountPolicy = WechatAccountApi.AgentWechatAccountVO & { quietMinutes?: number }

const getAccountQuietSeconds = (row: WechatAccountApi.AgentWechatAccountVO) => {
  if (row.quietSeconds && row.quietSeconds > 0) {
    return row.quietSeconds
  }
  const legacyQuietSeconds = ((row as LegacyAccountPolicy).quietMinutes ?? 0) * 60
  return legacyQuietSeconds > 0 ? legacyQuietSeconds : 90
}

onMounted(() => {
  getAgentList()
  getList()
})
</script>

<style scoped>
.account-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.account-name {
  line-height: 20px;
  font-weight: 500;
}

.account-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.callback-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.policy-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 18px;
}
</style>
