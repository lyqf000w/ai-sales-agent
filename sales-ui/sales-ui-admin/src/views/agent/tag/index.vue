<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" class="!w-240px" clearable placeholder="标签名称" />
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
      <el-table-column label="标签" min-width="180">
        <template #default="scope">
          <el-tag
            class="tag-name-action"
            effect="plain"
            :style="getReadableTagStyle(scope.row.color)"
            @click="openTaggedContacts(scope.row)"
          >
            {{ scope.row.name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="sort" width="100" />
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
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openTaggedContacts(scope.row)">客户</el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id)">编辑</el-button>
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

  <el-drawer v-model="taggedContactVisible" :title="taggedContactTitle" size="720px">
    <div class="tagged-contact-toolbar">
      <el-input
        v-model="taggedContactQuery.keyword"
        class="!w-240px"
        clearable
        placeholder="搜索客户昵称 / 备注 / wxid"
        @keyup.enter="handleTaggedContactQuery"
      />
      <el-button type="primary" @click="handleTaggedContactQuery">
        <Icon icon="ep:search" class="mr-5px" />搜索
      </el-button>
      <el-button @click="resetTaggedContactQuery">
        <Icon icon="ep:refresh" class="mr-5px" />重置
      </el-button>
      <el-tag effect="plain">共 {{ taggedContactTotal }} 个客户</el-tag>
    </div>
    <el-table v-loading="taggedContactLoading" :data="taggedContactList" row-key="id">
      <el-table-column label="客户" min-width="180">
        <template #default="scope">
          <div class="tagged-contact-name">
            <strong>{{ formatContactName(scope.row) }}</strong>
            <span>#{{ scope.row.id }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="微信账号" prop="wechatAccountId" width="100" />
      <el-table-column label="客户等级" align="center" width="100">
        <template #default="scope">
          <el-tag :type="getCustomerLevelMeta(scope.row.customerLevel).type" effect="plain">
            {{ getCustomerLevelMeta(scope.row.customerLevel).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跟进优先级" align="center" width="120">
        <template #default="scope">
          <el-tag :type="getFollowUpPriorityMeta(scope.row.followUpPriority).type" effect="plain">
            {{ getFollowUpPriorityMeta(scope.row.followUpPriority).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近消息" width="180">
        <template #default="scope">
          {{ scope.row.lastMessageTime ? formatDate(scope.row.lastMessageTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="110" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openConversation(scope.row)">查看会话</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="taggedContactQuery.pageSize"
      v-model:page="taggedContactQuery.pageNo"
      :total="taggedContactTotal"
      @pagination="getTaggedContactList"
    />
  </el-drawer>

  <Dialog v-model="dialogVisible" :title="dialogTitle" width="560">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="90px">
      <el-form-item label="标签名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入标签名称" />
      </el-form-item>
      <el-form-item label="颜色" prop="color">
        <el-color-picker v-model="formData.color" />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model="formData.description" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button :label="0">启用</el-radio-button>
          <el-radio-button :label="1">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter, formatDate } from '@/utils/formatTime'
import { getReadableTagStyle } from '@/utils/color'
import * as ContactApi from '@/api/agent/contact'
import * as TagApi from '@/api/agent/tag'

defineOptions({ name: 'AgentTag' })

const { t } = useI18n()
const message = useMessage()
const router = useRouter()

const loading = ref(true)
const total = ref(0)
const list = ref<TagApi.AgentContactTagVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const taggedContactVisible = ref(false)
const taggedContactLoading = ref(false)
const taggedContactList = ref<ContactApi.AgentWechatContactVO[]>([])
const taggedContactTotal = ref(0)
const activeTag = ref<TagApi.AgentContactTagVO>()
const taggedContactQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  tagId: undefined as number | undefined
})
const formData = ref<TagApi.AgentContactTagVO>(createDefaultForm())
const formRules = reactive({
  name: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const getList = async () => {
  loading.value = true
  try {
    const data = await TagApi.getTagPage(queryParams)
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

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TagApi.getTag(id)
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
      await TagApi.createTag(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await TagApi.updateTag(formData.value)
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
    await TagApi.deleteTag(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const openTaggedContacts = async (tag: TagApi.AgentContactTagVO) => {
  activeTag.value = tag
  taggedContactQuery.pageNo = 1
  taggedContactQuery.keyword = ''
  taggedContactQuery.tagId = tag.id
  taggedContactVisible.value = true
  await getTaggedContactList()
}

const getTaggedContactList = async () => {
  if (!taggedContactQuery.tagId) return
  taggedContactLoading.value = true
  try {
    const data = await ContactApi.getWechatContactPage(taggedContactQuery)
    taggedContactList.value = data.list
    taggedContactTotal.value = data.total
  } finally {
    taggedContactLoading.value = false
  }
}

const handleTaggedContactQuery = () => {
  taggedContactQuery.pageNo = 1
  getTaggedContactList()
}

const resetTaggedContactQuery = () => {
  taggedContactQuery.keyword = ''
  handleTaggedContactQuery()
}

const taggedContactTitle = computed(() => {
  return activeTag.value ? `标签客户：${activeTag.value.name}` : '标签客户'
})

const cleanWechatText = (value?: unknown) => {
  if (value == null) return ''
  return String(value).trim()
}

const isRawWechatIdentifier = (text: string) => {
  return text === 'weixin' || text.startsWith('wxid_') || text.endsWith('@chatroom')
}

const firstWechatText = (...values: unknown[]) => {
  for (const value of values) {
    const text = cleanWechatText(value)
    if (text && !isRawWechatIdentifier(text)) return text
  }
  return ''
}

const formatContactName = (contact: ContactApi.AgentWechatContactVO) => {
  return firstWechatText(contact.displayName, contact.remark, contact.nickname, contact.wechatId, contact.externalUserId)
    || `客户 #${contact.id}`
}

const getCustomerLevelMeta = (value?: number): { label: string; type: TagType } => {
  if (value === 2) return { label: '重要', type: 'danger' }
  if (value === 1) return { label: '目标', type: 'warning' }
  return { label: '普通', type: 'info' }
}

const getFollowUpPriorityMeta = (value?: string): { label: string; type: TagType } => {
  if (value === 'URGENT') return { label: '紧急跟进', type: 'danger' }
  if (value === 'FOCUS') return { label: '重点跟进', type: 'warning' }
  return { label: '普通跟进', type: 'info' }
}

const openConversation = (contact: ContactApi.AgentWechatContactVO) => {
  router.push({ path: '/agent/conversation', query: { contactId: contact.id } })
}

function createDefaultForm(): TagApi.AgentContactTagVO {
  return {
    id: undefined,
    name: '',
    color: '#409EFF',
    description: '',
    sort: 0,
    status: 0
  }
}

const resetForm = () => {
  formData.value = createDefaultForm()
  formRef.value?.resetFields()
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.tag-name-action {
  cursor: pointer;
}

.tagged-contact-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.tagged-contact-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tagged-contact-name span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
