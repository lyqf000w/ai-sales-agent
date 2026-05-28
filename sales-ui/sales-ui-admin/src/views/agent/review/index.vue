<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="会话编号" prop="conversationId">
        <el-input-number v-model="queryParams.conversationId" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="审核状态" prop="reviewStatus">
        <el-select v-model="queryParams.reviewStatus" class="!w-160px" clearable placeholder="全部">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已修改" value="EDITED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已自动发送" value="SENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" class="!w-140px" clearable placeholder="全部">
          <el-option label="绿色" :value="0" />
          <el-option label="黄色" :value="1" />
          <el-option label="红色" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="决策编号" align="center" prop="id" width="100" />
      <el-table-column label="会话" align="center" prop="conversationId" width="90" />
      <el-table-column label="类型" align="center" prop="decisionType" width="120">
        <template #default="scope">
          <el-tag effect="plain">{{ getDecisionTypeLabel(scope.row.decisionType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险" align="center" prop="riskLevel" width="90">
        <template #default="scope">
          <el-tag :type="getRiskLevel(scope.row.riskLevel).type" effect="plain">
            {{ getRiskLevel(scope.row.riskLevel).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="reviewStatus" width="110">
        <template #default="scope">
          <el-tag :type="getReviewStatus(scope.row.reviewStatus).type" effect="plain">
            {{ getReviewStatus(scope.row.reviewStatus).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建议回复" prop="suggestedContent" min-width="280" show-overflow-tooltip />
      <el-table-column label="决策原因" prop="decisionReason" min-width="180" show-overflow-tooltip />
      <el-table-column label="置信度" align="center" prop="confidence" width="90">
        <template #default="scope">{{ formatConfidence(scope.row.confidence) }}</template>
      </el-table-column>
      <el-table-column label="来源" align="center" width="150">
        <template #default="scope">
          <el-tag :type="getGenerationSource(scope.row).type" effect="plain">
            {{ getGenerationSource(scope.row).label }}
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
      <el-table-column label="操作" align="center" width="170" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.reviewStatus === 'PENDING'"
            link
            type="primary"
            @click="openApproveDialog(scope.row)"
          >
            审核
          </el-button>
          <el-button
            v-if="scope.row.reviewStatus === 'PENDING'"
            link
            type="danger"
            @click="openRejectDialog(scope.row)"
          >
            驳回
          </el-button>
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

  <Dialog v-model="approveDialogVisible" title="回复审核" width="680">
    <el-form label-width="90px">
      <el-form-item label="建议回复">
        <el-input v-model="approveContent" :rows="7" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="approveLoading" type="primary" @click="submitApprove">
        发送
      </el-button>
      <el-button @click="approveDialogVisible = false">取消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="rejectDialogVisible" title="驳回回复" width="560">
    <el-form label-width="90px">
      <el-form-item label="驳回原因">
        <el-input v-model="rejectReason" :rows="4" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="rejectLoading || !rejectReason.trim()" type="danger" @click="submitReject">
        驳回
      </el-button>
      <el-button @click="rejectDialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as ReviewApi from '@/api/agent/review'

defineOptions({ name: 'AgentReview' })

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const message = useMessage()
const loading = ref(true)
const total = ref(0)
const list = ref<ReviewApi.AgentReplyReviewVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  conversationId: undefined,
  reviewStatus: 'PENDING',
  riskLevel: undefined
})

const reviewStatusMap: Record<string, { label: string; type: TagType }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  EDITED: { label: '已修改', type: 'primary' },
  REJECTED: { label: '已驳回', type: 'info' },
  SENT: { label: '已自动发送', type: 'success' }
}

const riskLevelMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '绿色', type: 'success' },
  1: { label: '黄色', type: 'warning' },
  2: { label: '红色', type: 'danger' }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await ReviewApi.getReviewPage(queryParams)
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

const approveDialogVisible = ref(false)
const approveLoading = ref(false)
const activeDecisionId = ref<number>()
const approveContent = ref('')

const openApproveDialog = (row: ReviewApi.AgentReplyReviewVO) => {
  activeDecisionId.value = row.id
  approveContent.value = row.suggestedContent || ''
  approveDialogVisible.value = true
}

const submitApprove = async () => {
  if (!activeDecisionId.value) return
  approveLoading.value = true
  try {
    await ReviewApi.approveReview(activeDecisionId.value, approveContent.value)
    message.success('已发送')
    approveDialogVisible.value = false
    await getList()
  } finally {
    approveLoading.value = false
  }
}

const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')

const openRejectDialog = (row: ReviewApi.AgentReplyReviewVO) => {
  activeDecisionId.value = row.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const submitReject = async () => {
  if (!activeDecisionId.value || !rejectReason.value.trim()) return
  rejectLoading.value = true
  try {
    await ReviewApi.rejectReview(activeDecisionId.value, rejectReason.value.trim())
    message.success('已驳回')
    rejectDialogVisible.value = false
    await getList()
  } finally {
    rejectLoading.value = false
  }
}

const getDecisionTypeLabel = (decisionType?: string) => {
  const labels: Record<string, string> = {
    AUTO_SEND: '自动发送',
    MANUAL_CONFIRM: '人工确认',
    RECORD_ONLY: '仅记录'
  }
  return labels[decisionType || ''] || '未知'
}

const getReviewStatus = (reviewStatus?: string) => {
  return reviewStatusMap[reviewStatus || ''] || { label: '未知', type: 'info' }
}

const getRiskLevel = (riskLevel?: number) => {
  return riskLevelMap[riskLevel ?? 0] || riskLevelMap[0]
}

const formatConfidence = (confidence?: number) => {
  if (confidence === undefined || confidence === null) return '-'
  return `${Math.round(Number(confidence) * 100)}%`
}

const getGenerationSource = (row: ReviewApi.AgentReplyReviewVO) => {
  const source = row.generationSource || row.knowledgeRefs?.generationSource
  if (source === 'DEEPSEEK') {
    return { label: row.actualLlmModel || row.llmModel || 'DeepSeek', type: 'success' as TagType }
  }
  if (source === 'KNOWLEDGE') {
    return { label: '知识库', type: 'primary' as TagType }
  }
  return { label: '未知', type: 'info' as TagType }
}

onMounted(() => {
  getList()
})
</script>
