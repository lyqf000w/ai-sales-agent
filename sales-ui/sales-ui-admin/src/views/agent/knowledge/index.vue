<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="知识库" prop="knowledgeBaseId">
        <el-select v-model="queryParams.knowledgeBaseId" class="!w-220px" filterable placeholder="请选择知识库">
          <el-option v-for="item in knowledgeBaseList" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" class="!w-220px" clearable placeholder="标题、商品、关键词" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-120px" clearable placeholder="全部">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button plain @click="openBaseForm('create')">
          <Icon icon="ep:folder-add" class="mr-5px" />知识库
        </el-button>
        <el-button type="primary" plain @click="openItemForm('create')">
          <Icon icon="ep:plus" class="mr-5px" />资料
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="资料" min-width="220">
        <template #default="scope">
          <div class="knowledge-title">{{ scope.row.title }}</div>
          <div class="knowledge-meta">
            {{ scope.row.productName || '未填商品' }}
            <span v-if="scope.row.category"> · {{ scope.row.category }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="关键词" prop="keywords" min-width="200" show-overflow-tooltip />
      <el-table-column label="客户问题" prop="question" min-width="220" show-overflow-tooltip />
      <el-table-column label="答案/资料" prop="answer" min-width="300" show-overflow-tooltip />
      <el-table-column label="向量" align="center" prop="embeddingStatus" width="110">
        <template #default="scope">
          <el-tag :type="getEmbeddingStatus(scope.row.embeddingStatus).type" effect="plain">
            {{ getEmbeddingStatus(scope.row.embeddingStatus).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'info'" effect="plain">
            {{ scope.row.status === 0 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="210" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openItemForm('update', scope.row.id)">编辑</el-button>
          <el-button link type="primary" @click="handleRebuildIndex(scope.row.id)">重建索引</el-button>
          <el-button link type="danger" @click="handleDeleteItem(scope.row.id)">删除</el-button>
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

  <Dialog v-model="baseDialogVisible" :title="baseDialogTitle" width="560">
    <el-form ref="baseFormRef" v-loading="baseFormLoading" :model="baseFormData" :rules="baseFormRules" label-width="90px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="baseFormData.name" placeholder="例如 默认商品库" />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model="baseFormData.description" :rows="3" type="textarea" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="baseFormData.status">
          <el-radio-button :label="0">启用</el-radio-button>
          <el-radio-button :label="1">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="baseFormLoading" type="primary" @click="submitBaseForm">确定</el-button>
      <el-button @click="baseDialogVisible = false">取消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="820">
    <el-form ref="itemFormRef" v-loading="itemFormLoading" :model="itemFormData" :rules="itemFormRules" label-width="100px">
      <el-form-item label="知识库" prop="knowledgeBaseId">
        <el-select v-model="itemFormData.knowledgeBaseId" class="!w-320px" filterable>
          <el-option v-for="item in knowledgeBaseList" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="itemFormData.title" placeholder="请输入资料标题" />
      </el-form-item>
      <el-form-item label="商品" prop="productName">
        <el-input v-model="itemFormData.productName" placeholder="例如 AI 销冠企业版" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="itemFormData.category" class="!w-220px" allow-create clearable filterable default-first-option>
          <el-option label="报价" value="报价" />
          <el-option label="功能" value="功能" />
          <el-option label="部署" value="部署" />
          <el-option label="售后" value="售后" />
          <el-option label="合同" value="合同" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keywords">
        <el-input v-model="itemFormData.keywords" placeholder="多个关键词用逗号分隔" />
      </el-form-item>
      <el-form-item label="客户问题" prop="question">
        <el-input v-model="itemFormData.question" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="资料内容" prop="answer">
        <el-input v-model="itemFormData.answer" :rows="6" type="textarea" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="itemFormData.sort" :min="0" class="!w-180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="itemFormData.status">
          <el-radio-button :label="0">启用</el-radio-button>
          <el-radio-button :label="1">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="itemFormLoading" type="primary" @click="submitItemForm">确定</el-button>
      <el-button @click="itemDialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as KnowledgeApi from '@/api/agent/knowledge'

defineOptions({ name: 'AgentKnowledge' })

const { t } = useI18n()
const message = useMessage()

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const loading = ref(true)
const total = ref(0)
const list = ref<KnowledgeApi.AgentKnowledgeItemVO[]>([])
const knowledgeBaseList = ref<KnowledgeApi.AgentKnowledgeBaseVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  knowledgeBaseId: undefined as number | undefined,
  keyword: '',
  status: undefined as number | undefined
})

const loadKnowledgeBaseList = async () => {
  knowledgeBaseList.value = await KnowledgeApi.getSimpleKnowledgeBaseList()
  if (!queryParams.knowledgeBaseId && knowledgeBaseList.value[0]?.id) {
    queryParams.knowledgeBaseId = knowledgeBaseList.value[0].id
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await KnowledgeApi.getKnowledgeItemPage(queryParams)
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
  if (knowledgeBaseList.value[0]?.id) queryParams.knowledgeBaseId = knowledgeBaseList.value[0].id
  handleQuery()
}

const baseDialogVisible = ref(false)
const baseDialogTitle = ref('')
const baseFormLoading = ref(false)
const baseFormType = ref('')
const baseFormRef = ref()
const baseFormData = ref<KnowledgeApi.AgentKnowledgeBaseVO>({
  id: undefined,
  name: '',
  description: '',
  status: 0
})
const baseFormRules = reactive({
  name: [{ required: true, message: '知识库名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const openBaseForm = async (type: string, id?: number) => {
  baseDialogVisible.value = true
  baseDialogTitle.value = type === 'create' ? '新增知识库' : '编辑知识库'
  baseFormType.value = type
  baseFormData.value = { id: undefined, name: '', description: '', status: 0 }
  if (id) baseFormData.value = await KnowledgeApi.getKnowledgeBase(id)
}

const submitBaseForm = async () => {
  const valid = await baseFormRef.value.validate()
  if (!valid) return
  baseFormLoading.value = true
  try {
    if (baseFormType.value === 'create') {
      await KnowledgeApi.createKnowledgeBase(baseFormData.value)
      message.success(t('common.createSuccess'))
    } else {
      await KnowledgeApi.updateKnowledgeBase(baseFormData.value)
      message.success(t('common.updateSuccess'))
    }
    baseDialogVisible.value = false
    await loadKnowledgeBaseList()
    await getList()
  } finally {
    baseFormLoading.value = false
  }
}

const itemDialogVisible = ref(false)
const itemDialogTitle = ref('')
const itemFormLoading = ref(false)
const itemFormType = ref('')
const itemFormRef = ref()
const itemFormData = ref<KnowledgeApi.AgentKnowledgeItemVO>({
  id: undefined,
  knowledgeBaseId: 0,
  title: '',
  productName: '',
  category: '',
  keywords: '',
  question: '',
  answer: '',
  sort: 0,
  status: 0
})
const itemFormRules = reactive({
  knowledgeBaseId: [{ required: true, message: '知识库不能为空', trigger: 'change' }],
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  answer: [{ required: true, message: '资料内容不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const resetItemForm = () => {
  itemFormData.value = {
    id: undefined,
    knowledgeBaseId: queryParams.knowledgeBaseId || knowledgeBaseList.value[0]?.id || 0,
    title: '',
    productName: '',
    category: '',
    keywords: '',
    question: '',
    answer: '',
    sort: 0,
    status: 0
  }
}

const openItemForm = async (type: string, id?: number) => {
  itemDialogVisible.value = true
  itemDialogTitle.value = type === 'create' ? '新增资料' : '编辑资料'
  itemFormType.value = type
  resetItemForm()
  if (id) itemFormData.value = await KnowledgeApi.getKnowledgeItem(id)
}

const submitItemForm = async () => {
  const valid = await itemFormRef.value.validate()
  if (!valid) return
  itemFormLoading.value = true
  try {
    if (itemFormType.value === 'create') {
      await KnowledgeApi.createKnowledgeItem(itemFormData.value)
      message.success(t('common.createSuccess'))
    } else {
      await KnowledgeApi.updateKnowledgeItem(itemFormData.value)
      message.success(t('common.updateSuccess'))
    }
    itemDialogVisible.value = false
    await getList()
  } finally {
    itemFormLoading.value = false
  }
}

const handleRebuildIndex = async (id: number) => {
  await KnowledgeApi.rebuildKnowledgeItemIndex(id)
  message.success('索引已重建')
  await getList()
}

const handleDeleteItem = async (id: number) => {
  try {
    await message.delConfirm()
    await KnowledgeApi.deleteKnowledgeItem(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const getEmbeddingStatus = (status?: string): { label: string; type: TagType } => {
  const map: Record<string, { label: string; type: TagType }> = {
    READY: { label: '已就绪', type: 'success' },
    FAILED: { label: '失败', type: 'danger' },
    PENDING: { label: '待生成', type: 'warning' }
  }
  return map[status || 'PENDING'] || map.PENDING
}

onMounted(async () => {
  await loadKnowledgeBaseList()
  await getList()
})
</script>

<style scoped>
.knowledge-title {
  color: var(--el-text-color-primary);
  font-weight: 600;
  line-height: 1.4;
}

.knowledge-meta {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
