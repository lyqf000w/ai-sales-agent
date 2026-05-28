<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          class="!w-240px"
          clearable
          placeholder="Agent 名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="全部">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="primary" plain @click="openForm('create')">
          <Icon icon="ep:plus" class="mr-5px" />新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="Agent" min-width="220">
        <template #default="scope">
          <div class="name-cell">
            <div class="name-main">{{ scope.row.name }}</div>
            <div class="name-meta">{{ scope.row.aliasName || scope.row.scene || '-' }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="模型" min-width="190">
        <template #default="scope">
          <div class="model-cell">
            <el-tag effect="plain">{{ getLlmProviderLabel(scope.row.llmProvider) }}</el-tag>
            <span>{{ scope.row.llmModel || 'deepseek-v4-pro' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="知识库" min-width="150">
        <template #default="scope">{{ getKnowledgeBaseName(scope.row.knowledgeBaseId) }}</template>
      </el-table-column>
      <el-table-column label="目标客户" prop="targetCustomerDesc" min-width="220" show-overflow-tooltip />
      <el-table-column label="版本" align="center" width="130">
        <template #default="scope">
          <div class="version-cell">
            <span>{{ scope.row.onlineVersion || 0 }} / {{ scope.row.draftVersion || 1 }}</span>
            <el-tag v-if="hasDraftChange(scope.row)" size="small" type="warning" effect="plain">待发布</el-tag>
          </div>
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
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)">编辑</el-button>
          <el-button link type="success" :disabled="!hasDraftChange(scope.row)" @click="openPublish(scope.row)">
            发布
          </el-button>
          <el-button link type="info" @click="openVersions(scope.row)">版本</el-button>
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

  <Dialog v-model="dialogVisible" :title="dialogTitle" width="820">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="Agent 名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入 Agent 名称" />
      </el-form-item>
      <el-form-item label="别名" prop="aliasName">
        <el-input v-model="formData.aliasName" placeholder="请输入别名" />
      </el-form-item>
      <el-form-item label="销售场景" prop="scene">
        <el-input v-model="formData.scene" placeholder="例如 SaaS 线索转化" />
      </el-form-item>
      <el-form-item label="目标客户" prop="targetCustomerDesc">
        <el-input v-model="formData.targetCustomerDesc" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="模型厂商" prop="llmProvider">
        <el-select v-model="formData.llmProvider" class="!w-240px">
          <el-option label="DeepSeek" value="DEEPSEEK" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型名称" prop="llmModel">
        <el-select v-model="formData.llmModel" class="!w-320px" filterable default-first-option>
          <el-option
            v-for="item in currentModelOptions"
            :key="item.model"
            :label="item.label"
            :value="item.model"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="默认知识库" prop="knowledgeBaseId">
        <el-select v-model="formData.knowledgeBaseId" class="!w-320px" clearable filterable placeholder="不绑定知识库">
          <el-option v-for="item in knowledgeBaseList" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="系统提示词" prop="systemPrompt">
        <el-input
          v-model="formData.systemPrompt"
          :rows="5"
          maxlength="4000"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="回复语气" prop="tone">
        <el-input v-model="formData.tone" placeholder="例如 专业、克制、简洁" />
      </el-form-item>
      <el-form-item label="欢迎语" prop="welcomeMessage">
        <el-input v-model="formData.welcomeMessage" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="转人工话术" prop="handoverMessage">
        <el-input v-model="formData.handoverMessage" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button :label="0">启用</el-radio-button>
          <el-radio-button :label="1">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="publishDialogVisible" title="发布 Agent 配置" width="520">
    <el-form :model="publishForm" label-width="88px">
      <el-form-item label="Agent">
        <el-input v-model="publishForm.agentName" disabled />
      </el-form-item>
      <el-form-item label="版本">
        <el-tag type="warning" effect="plain">草稿 v{{ publishForm.draftVersion || 1 }}</el-tag>
      </el-form-item>
      <el-form-item label="变更说明">
        <el-input v-model="publishForm.changeSummary" :rows="3" maxlength="512" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="publishLoading" type="primary" @click="submitPublish">发 布</el-button>
      <el-button @click="publishDialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <el-drawer v-model="versionDrawerVisible" :title="versionDrawerTitle" size="720px">
    <el-table v-loading="versionLoading" :data="versionList" row-key="id">
      <el-table-column label="版本" align="center" prop="version" width="90">
        <template #default="scope">v{{ scope.row.version }}</template>
      </el-table-column>
      <el-table-column label="变更说明" prop="changeSummary" min-width="180" show-overflow-tooltip />
      <el-table-column label="发布人" align="center" prop="publishUserId" width="100" />
      <el-table-column
        label="发布时间"
        align="center"
        prop="publishTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="配置摘要" min-width="260" show-overflow-tooltip>
        <template #default="scope">{{ formatVersionSnapshot(scope.row.configSnapshot) }}</template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ElMessageBox } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import * as AgentApi from '@/api/agent/agent'
import * as KnowledgeApi from '@/api/agent/knowledge'

defineOptions({ name: 'AgentConfig' })

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<AgentApi.AgentVO[]>([])
const llmModelOptions = ref<AgentApi.AgentLlmModelOptionVO[]>([])
const knowledgeBaseList = ref<KnowledgeApi.AgentKnowledgeBaseVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AgentApi.getAgentPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AgentApi.AgentVO>({
  id: undefined,
  name: '',
  aliasName: '',
  scene: '',
  targetCustomerDesc: '',
  systemPrompt: '',
  llmProvider: 'DEEPSEEK',
  llmModel: 'deepseek-v4-pro',
  knowledgeBaseId: undefined,
  replyMode: 'MANUAL_CONFIRM',
  tone: '',
  welcomeMessage: '',
  handoverMessage: '',
  status: 0
})
const formRules = reactive({
  name: [{ required: true, message: 'Agent 名称不能为空', trigger: 'blur' }],
  llmProvider: [{ required: true, message: '模型厂商不能为空', trigger: 'change' }],
  llmModel: [{ required: true, message: '模型名称不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const currentModelOptions = computed(() => {
  return llmModelOptions.value.filter((item) => item.provider === formData.value.llmProvider)
})

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = normalizeAgent(await AgentApi.getAgent(id))
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AgentApi.createAgent(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AgentApi.updateAgent(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AgentApi.deleteAgent(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const resetForm = () => {
  formData.value = normalizeAgent({
    id: undefined,
    name: '',
    aliasName: '',
    scene: '',
    targetCustomerDesc: '',
    systemPrompt: '',
    llmProvider: 'DEEPSEEK',
    llmModel: 'deepseek-v4-pro',
    knowledgeBaseId: undefined,
    replyMode: 'MANUAL_CONFIRM',
    tone: '',
    welcomeMessage: '',
    handoverMessage: '',
    status: 0
  })
  formRef.value?.resetFields()
}

const getLlmProviderLabel = (provider?: string) => {
  const labels: Record<string, string> = {
    DEEPSEEK: 'DeepSeek'
  }
  return labels[provider || ''] || provider || 'DeepSeek'
}

const normalizeAgent = (agent: AgentApi.AgentVO): AgentApi.AgentVO => ({
  ...agent,
  systemPrompt: agent.systemPrompt || '',
  llmProvider: agent.llmProvider || 'DEEPSEEK',
  llmModel: agent.llmModel || 'deepseek-v4-pro',
  knowledgeBaseId: agent.knowledgeBaseId,
  replyMode: agent.replyMode || 'MANUAL_CONFIRM'
})

watch(
  () => formData.value.llmProvider,
  () => {
    if (!currentModelOptions.value.some((item) => item.model === formData.value.llmModel)) {
      formData.value.llmModel =
        currentModelOptions.value.find((item) => item.defaultModel)?.model || currentModelOptions.value[0]?.model || ''
    }
  }
)

const getKnowledgeBaseName = (id?: number) => {
  return knowledgeBaseList.value.find((item) => item.id === id)?.name || '未绑定'
}

const hasDraftChange = (row: AgentApi.AgentVO) => {
  return Number(row.draftVersion || 1) > Number(row.onlineVersion || 0)
}

const publishDialogVisible = ref(false)
const publishLoading = ref(false)
const publishForm = reactive({
  agentId: undefined as number | undefined,
  agentName: '',
  draftVersion: 1,
  changeSummary: ''
})

const openPublish = (row: AgentApi.AgentVO) => {
  publishForm.agentId = row.id
  publishForm.agentName = row.name
  publishForm.draftVersion = row.draftVersion || 1
  publishForm.changeSummary = ''
  publishDialogVisible.value = true
}

const submitPublish = async () => {
  if (!publishForm.agentId) return
  try {
    await ElMessageBox.confirm(`确认发布 ${publishForm.agentName} 的草稿 v${publishForm.draftVersion}？`, '提示', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  publishLoading.value = true
  try {
    await AgentApi.publishAgent(publishForm.agentId, publishForm.changeSummary)
    message.success('发布成功')
    publishDialogVisible.value = false
    await getList()
  } finally {
    publishLoading.value = false
  }
}

const versionDrawerVisible = ref(false)
const versionDrawerTitle = ref('配置版本')
const versionLoading = ref(false)
const versionList = ref<AgentApi.AgentConfigVersionVO[]>([])

const openVersions = async (row: AgentApi.AgentVO) => {
  if (!row.id) return
  versionDrawerVisible.value = true
  versionDrawerTitle.value = `${row.name} · 配置版本`
  versionLoading.value = true
  try {
    versionList.value = await AgentApi.getAgentVersions(row.id)
  } finally {
    versionLoading.value = false
  }
}

const formatVersionSnapshot = (snapshot?: Record<string, any>) => {
  if (!snapshot) return '-'
  const chunks = [
    snapshot.llmProvider ? getLlmProviderLabel(snapshot.llmProvider) : '',
    snapshot.llmModel ? snapshot.llmModel : '',
    snapshot.systemPrompt ? '含提示词' : '',
    snapshot.tone ? `语气 ${snapshot.tone}` : '',
    snapshot.welcomeMessage ? '含欢迎语' : '',
    snapshot.handoverMessage ? '含转人工话术' : ''
  ].filter(Boolean)
  return chunks.join('，') || '-'
}

onMounted(() => {
  AgentApi.getLlmModelOptions().then((data) => {
    llmModelOptions.value = data
  })
  KnowledgeApi.getSimpleKnowledgeBaseList().then((data) => (knowledgeBaseList.value = data))
  getList()
})
</script>

<style scoped>
.name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-main {
  font-weight: 500;
  line-height: 20px;
}

.name-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.model-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 18px;
}

.version-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 24px;
}

</style>
