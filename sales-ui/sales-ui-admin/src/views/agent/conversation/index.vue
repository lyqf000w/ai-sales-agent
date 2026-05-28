<template>
  <ContentWrap class="conversation-filter-wrap" :body-style="{ padding: '8px 10px 2px' }">
    <el-form
      ref="queryFormRef"
      class="compact-filter-form"
      :inline="true"
      :model="queryParams"
      label-width="40px"
      size="small"
    >
      <el-form-item label="队列" prop="queueType" class="queue-form-item">
        <el-radio-group v-model="queryParams.queueType" @change="handleQueueChange">
          <el-radio-button label="RISK">待处理</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="PENDING_REVIEW">待确认</el-radio-button>
          <el-radio-button label="TAKEOVER">人工接管</el-radio-button>
          <el-radio-button label="FOCUS">重点跟进</el-radio-button>
          <el-radio-button label="URGENT">紧急跟进</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="微信" prop="wechatAccountId">
        <el-select
          v-model="queryParams.wechatAccountId"
          class="!w-150px"
          clearable
          filterable
          placeholder="全部微信账号"
          @change="handleWechatAccountChange"
        >
          <el-option
            v-for="account in wechatAccountList"
            :key="account.id"
            :label="formatWechatAccount(account)"
            :value="account.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="客户" prop="contactId">
        <el-select
          v-model="queryParams.contactId"
          class="!w-160px"
          clearable
          filterable
          remote
          reserve-keyword
          :loading="contactLoading"
          placeholder="全部客户"
          :remote-method="remoteContactSearch"
          @change="handleContactChange"
          @clear="handleContactClear"
          @visible-change="handleContactSelectorVisible"
        >
          <el-option
            v-for="contact in contactList"
            :key="contact.id"
            :label="formatContact(contact)"
            :value="contact.id"
          >
            <div class="contact-option">
              <span class="contact-option-title">{{ formatContact(contact) }}</span>
              <span class="contact-option-meta">#{{ contact.id }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item class="filter-actions">
        <el-tooltip content="同步通讯录" placement="top">
          <el-button
            aria-label="同步通讯录"
            :loading="syncLoading"
            plain
            type="primary"
            @click="handleSyncContacts"
          >
            <Icon icon="ep:connection" />
          </el-button>
        </el-tooltip>
        <el-tooltip content="搜索" placement="top">
          <el-button aria-label="搜索" type="primary" @click="handleQuery">
            <Icon icon="ep:search" />
          </el-button>
        </el-tooltip>
        <el-tooltip content="重置" placement="top">
          <el-button aria-label="重置" @click="resetQuery">
            <Icon icon="ep:refresh" />
          </el-button>
        </el-tooltip>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <el-alert
    v-if="riskAlert.total > 0"
    class="risk-workbench-alert"
    :closable="false"
    show-icon
    type="warning"
  >
    <template #title>
      <div class="risk-alert-title">
        <span>当前有 {{ riskAlert.total }} 个会话仍处于待确认/人工接管状态，自动回复已暂停等待处理。</span>
        <div class="risk-alert-actions">
          <el-button link type="warning" @click="openRiskQueue('PENDING_REVIEW')">查看待确认</el-button>
          <el-button link type="danger" @click="openRiskQueue('TAKEOVER')">查看人工接管</el-button>
        </div>
      </div>
    </template>
    <div class="risk-alert-preview">
      <span v-for="item in riskAlert.preview" :key="item.contactId" class="risk-preview-item">
        {{ formatQueueContact(item) }}
      </span>
    </div>
  </el-alert>

  <div class="conversation-workbench">
    <aside class="conversation-list">
      <div class="list-header">
        <span>会话队列</span>
        <el-tag effect="plain">{{ total }}</el-tag>
      </div>
      <div v-loading="loading" class="list-body">
        <el-empty v-if="list.length === 0" description="暂无会话" />
        <button
          v-for="item in list"
          :key="item.contactId"
          class="conversation-item"
          :class="{
            active: item.contactId === activeConversation?.contactId,
            'needs-human': isRiskConversation(item),
            takeover: item.status === 3
          }"
          @click="openConversation(item)"
        >
          <div class="item-main">
            <span class="item-title">{{ formatQueueContact(item) }}</span>
            <el-tag :type="getConversationKindMeta(item).type" effect="plain" size="small">
              {{ getConversationKindMeta(item).label }}
            </el-tag>
            <el-tag :type="getFollowUpPriorityMeta(item.followUpPriority).type" effect="plain" size="small">
              {{ getFollowUpPriorityMeta(item.followUpPriority).label }}
            </el-tag>
          </div>
          <div class="item-meta">
            <span>{{ getWechatAccountName(item.wechatAccountId) }}</span>
            <el-tag :type="getCustomerLevelMeta(item.customerLevel).type" effect="plain" size="small">
              {{ getCustomerLevelMeta(item.customerLevel).label }}
            </el-tag>
          </div>
          <div class="item-foot">
            <el-tag v-if="isRiskConversation(item)" :type="getStatus(item.status).type" effect="dark" size="small">
              {{ getStatus(item.status).label }}
            </el-tag>
            <span>{{ item.lastMessageTime ? formatDate(item.lastMessageTime) : '暂无消息' }}</span>
          </div>
        </button>
      </div>
      <Pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNo"
        :total="total"
        layout="prev, pager, next"
        small
        @pagination="getList"
      />
    </aside>

    <main class="message-panel">
      <template v-if="activeConversation">
        <div class="message-header">
          <div>
            <div class="message-title">{{ getContactName(activeConversation.contactId) }}</div>
            <div class="message-subtitle">
              {{ getWechatAccountName(activeConversation.wechatAccountId) }} · 会话 #{{ activeConversation.conversationId }}
            </div>
          </div>
          <div class="header-actions">
            <div class="header-tags">
              <el-tag :type="getConversationKindMeta(activeConversation).type" effect="plain">
                {{ getConversationKindMeta(activeConversation).label }}
              </el-tag>
              <el-tag :type="getFollowUpPriorityMeta(activeContact?.followUpPriority).type" effect="plain">
                {{ getFollowUpPriorityMeta(activeContact?.followUpPriority).label }}
              </el-tag>
              <el-tag :type="getCustomerLevelMeta(activeContact?.customerLevel).type" effect="plain">
                {{ getCustomerLevelMeta(activeContact?.customerLevel).label }}
              </el-tag>
            </div>
            <el-button
              v-if="isHumanTakeover"
              :loading="restorePolicyLoading"
              type="primary"
              @click="restoreOriginalPolicy"
            >
              <Icon icon="ep:refresh-left" />
              恢复原策略
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="isRiskConversation(activeConversation)"
          class="conversation-risk-alert"
          :closable="false"
          show-icon
          :type="activeConversation.status === 3 ? 'error' : 'warning'"
        >
          <template #title>
            <span>
              该会话已命中人工升级规则，当前状态为
              {{ getStatus(activeConversation.status).label }}，自动回复已暂停。
            </span>
          </template>
        </el-alert>

        <div v-loading="messageLoading" class="message-list">
          <el-empty v-if="messages.length === 0" description="暂无消息" />
          <div
            v-for="item in messages"
            :key="item.id"
            class="message-row"
            :class="{ outbound: item.direction === 2, pending: item.sendStatus === 1 }"
          >
            <div class="message-meta">
              <el-tag :type="getSenderType(item).type" effect="plain" size="small">
                {{ getSenderType(item).label }}
              </el-tag>
              <span v-if="item.senderDisplayName" class="message-sender">{{ item.senderDisplayName }}</span>
              <span>{{ item.messageTime ? formatDate(item.messageTime) : '' }}</span>
            </div>
            <div class="message-bubble">
              <template v-if="isImageMessage(item)">
                <el-image
                  v-if="displayMediaUrl(item)"
                  class="message-image"
                  :preview-src-list="getMediaPreviewList(item)"
                  :src="displayMediaUrl(item)"
                  fit="contain"
                  lazy
                />
                <el-button
                  v-else
                  :loading="mediaLoadingMap[item.id]"
                  plain
                  size="small"
                  type="primary"
                  @click="loadGeweMedia(item)"
                >
                  加载图片
                </el-button>
                <div v-if="mediaErrorMap[item.id]" class="message-media-caption voice-error">
                  {{ mediaErrorMap[item.id] }}
                </div>
                <div v-if="getMediaCaption(item)" class="message-media-caption">{{ getMediaCaption(item) }}</div>
              </template>
              <template v-else-if="isVideoMessage(item)">
                <video
                  v-if="displayMediaUrl(item)"
                  class="message-video"
                  :poster="displayThumbUrl(item)"
                  :src="displayMediaUrl(item)"
                  controls
                  preload="metadata"
                ></video>
                <el-button
                  v-else
                  :loading="mediaLoadingMap[item.id]"
                  plain
                  size="small"
                  type="primary"
                  @click="loadGeweMedia(item)"
                >
                  加载视频
                </el-button>
                <div v-if="getMediaCaption(item)" class="message-media-caption">{{ getMediaCaption(item) }}</div>
              </template>
              <template v-else-if="isEmojiMessage(item)">
                <img
                  v-if="displayMediaUrl(item)"
                  class="message-emoji"
                  :alt="getMediaCaption(item) || 'emoji'"
                  :src="displayMediaUrl(item)"
                  decoding="async"
                  loading="lazy"
                  referrerpolicy="no-referrer"
                />
                <el-button
                  v-else
                  :loading="mediaLoadingMap[item.id]"
                  plain
                  size="small"
                  type="primary"
                  @click="loadGeweMedia(item)"
                >
                  加载表情
                </el-button>
                <div v-if="mediaErrorMap[item.id]" class="message-media-caption voice-error">
                  {{ mediaErrorMap[item.id] }}
                </div>
                <div v-if="getMediaCaption(item)" class="message-media-caption">{{ getMediaCaption(item) }}</div>
              </template>
              <template v-else-if="isVoiceMessage(item)">
                <div class="message-voice">
                  <button
                    class="voice-load-button"
                    :disabled="voiceLoadingMap[item.id]"
                    type="button"
                    @click="loadVoiceMedia(item)"
                  >
                    <Icon :icon="voiceObjectUrls[item.id] ? 'ep:video-play' : 'ep:microphone'" />
                    <span>{{ voiceObjectUrls[item.id] ? '重新加载语音' : '加载语音' }}</span>
                    <span v-if="formatVoiceDuration(item)" class="voice-duration">
                      {{ formatVoiceDuration(item) }}
                    </span>
                  </button>
                  <audio
                    v-if="voiceObjectUrls[item.id] && voicePlayableMap[item.id]"
                    class="message-audio"
                    :src="voiceObjectUrls[item.id]"
                    controls
                    preload="metadata"
                    @error="handleVoicePlayError(item)"
                  ></audio>
                  <a
                    v-if="voiceObjectUrls[item.id]"
                    class="message-media-link"
                    :download="getVoiceDownloadName(item)"
                    :href="voiceObjectUrls[item.id]"
                  >
                    下载原语音
                  </a>
                  <div v-if="voiceErrorMap[item.id]" class="message-media-caption voice-error">
                    {{ voiceErrorMap[item.id] }}
                  </div>
                </div>
              </template>
              <template v-else-if="isFileMessage(item)">
                <div class="message-file">
                  <Icon icon="ep:document" />
                  <span class="message-file-name">{{ getMediaCaption(item) || formatMessageContent(item) }}</span>
                  <el-button
                    :loading="mediaLoadingMap[item.id]"
                    link
                    type="primary"
                    @click="downloadGeweFile(item)"
                  >
                    下载
                  </el-button>
                  <div v-if="mediaErrorMap[item.id]" class="message-media-caption voice-error">
                    {{ mediaErrorMap[item.id] }}
                  </div>
                </div>
              </template>
              <template v-else-if="item.mediaUrl">
                <a class="message-media-link" :href="displayMediaUrl(item)" rel="noreferrer" target="_blank">
                  {{ getMediaCaption(item) || formatMessageContent(item) }}
                </a>
              </template>
              <template v-else>
                {{ formatMessageContent(item) }}
              </template>
            </div>
            <div class="message-extra">
              <el-tag v-if="item.direction === 2" :type="getSendStatus(item.sendStatus).type" size="small" effect="plain">
                {{ getSendStatus(item.sendStatus).label }}
              </el-tag>
              <span v-if="item.matchedPolicy">命中：{{ item.matchedPolicy }}</span>
              <span v-if="item.auditNote">{{ item.auditNote }}</span>
            </div>
            <div v-if="item.sendStatus === 1" class="message-actions">
              <el-button size="small" type="primary" @click="approveMessage(item.id)">通过并发送</el-button>
              <el-button size="small" @click="startEditSuggestion(item)">编辑</el-button>
              <el-button size="small" @click="rejectMessage(item.id)">驳回</el-button>
            </div>
          </div>
        </div>

        <div class="reply-box">
          <el-input v-model="manualReplyContent" :rows="2" placeholder="输入人工回复" type="textarea" />
          <div class="reply-actions">
            <el-button v-if="editingMessageId" @click="cancelEditSuggestion">取消编辑</el-button>
            <el-button :disabled="!manualReplyContent.trim()" type="primary" @click="sendManualMessage">
              {{ editingMessageId ? '发送编辑稿' : '人工发送' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-else class="empty-panel" description="请选择一个好友" />
    </main>

    <aside class="context-panel">
      <div class="context-section">
        <div class="section-title section-title-with-action">
          <span>客户信息</span>
          <el-tooltip content="编辑客户设置" placement="top">
            <el-button
              :disabled="!activeConversation"
              circle
              size="small"
              @click="openContactSettingDialog"
            >
              <Icon icon="ep:edit" />
            </el-button>
          </el-tooltip>
        </div>
        <div class="context-row">
          <span>客户</span>
          <strong>{{ activeConversation ? getContactName(activeConversation.contactId) : '-' }}</strong>
        </div>
        <div class="context-row">
          <span>客户编号</span>
          <strong>{{ activeConversation?.contactId || '-' }}</strong>
        </div>
        <div class="context-row">
          <span>微信账号</span>
          <strong>{{ activeConversation ? getWechatAccountName(activeConversation.wechatAccountId) : '-' }}</strong>
        </div>
        <div class="context-row">
          <span>客户等级</span>
          <el-tag v-if="activeContact" :type="getCustomerLevelMeta(activeContact.customerLevel).type" effect="plain">
            {{ getCustomerLevelMeta(activeContact.customerLevel).label }}
          </el-tag>
        </div>
      </div>
      <div class="context-section">
        <div class="section-title">回复策略</div>
        <div class="context-row">
          <span>回复模式</span>
          <strong>{{ getEffectiveReplyModeLabel(activeConversation) }}</strong>
        </div>
        <div class="context-row">
          <span>静默秒数</span>
          <strong>{{ getEffectiveQuietSeconds(activeConversation) }} 秒</strong>
        </div>
        <div class="context-row">
          <span>营业时间</span>
          <strong>{{ getEffectiveBusinessHoursText(activeConversation) }}</strong>
        </div>
        <div class="context-row">
          <span>策略来源</span>
          <strong>{{ getPolicySourceLabel(activeReplyPolicy?.source) }}</strong>
        </div>
      </div>
      <div class="context-section">
        <div class="section-title">销售洞察</div>
        <div class="context-row">
          <span>购买意愿</span>
          <el-tag v-if="activeContact" :type="getPurchaseIntentionMeta(activeContact.purchaseIntention).type" effect="plain">
            {{ getPurchaseIntentionMeta(activeContact.purchaseIntention).label }}
          </el-tag>
        </div>
        <div class="context-row">
          <span>销售阶段</span>
          <strong>{{ getSalesStageLabel(activeContact?.salesStage) }}</strong>
        </div>
        <div class="context-row">
          <span>客户情绪</span>
          <el-tag v-if="activeContact" :type="getSentimentMeta(activeContact.customerSentiment).type" effect="plain">
            {{ getSentimentMeta(activeContact.customerSentiment).label }}
          </el-tag>
        </div>
        <div class="context-row">
          <span>跟进优先级</span>
          <el-tag v-if="activeContact" :type="getFollowUpPriorityMeta(activeContact.followUpPriority).type" effect="plain">
            {{ getFollowUpPriorityMeta(activeContact.followUpPriority).label }}
          </el-tag>
        </div>
        <div class="tag-list">
          <el-tag
            v-for="tag in activeContactTags"
            :key="tag.id"
            effect="plain"
            size="small"
            :style="getReadableTagStyle(tag.color)"
          >
            {{ tag.name }}
          </el-tag>
          <span v-if="activeContactTags.length === 0" class="empty-tags">暂无客户标签</span>
        </div>
      </div>
    </aside>
  </div>

  <Dialog
    v-model="contactSettingVisible"
    title="客户工作台设置"
    width="720"
    scroll
    max-height="calc(100vh - 180px)"
  >
    <el-form
      ref="contactSettingFormRef"
      v-loading="contactSettingLoading"
      :model="contactSettingForm"
      label-width="96px"
    >
      <el-divider content-position="left">客户</el-divider>
      <el-form-item label="客户等级">
        <el-radio-group v-model="contactSettingForm.customerLevel">
          <el-radio-button
            v-for="item in customerLevelOptions"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-divider content-position="left">策略</el-divider>
      <el-form-item label="回复策略">
        <div class="policy-control">
          <el-checkbox v-model="contactSettingForm.replyModeInherited">继承微信号</el-checkbox>
          <el-select
            v-model="contactSettingForm.replyMode"
            class="!w-260px"
            :disabled="contactSettingForm.replyModeInherited"
          >
            <el-option
              v-for="item in replyModeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
      </el-form-item>
      <el-form-item label="静默秒数">
        <div class="policy-control">
          <el-checkbox v-model="contactSettingForm.quietSecondsInherited">继承微信号</el-checkbox>
          <el-input-number
            v-model="contactSettingForm.quietSeconds"
            :disabled="contactSettingForm.quietSecondsInherited"
            :min="1"
            :step="5"
            class="!w-180px"
          />
          <div class="field-help">
            收到客户最后一条消息后等待指定秒数，再完整分析这段对话并回复。
          </div>
        </div>
      </el-form-item>
      <el-form-item label="营业时间">
        <div class="policy-control">
          <el-checkbox v-model="contactSettingForm.businessHoursInherited">继承微信号</el-checkbox>
          <div class="business-hours-editor">
            <el-time-picker
              v-model="contactSettingForm.businessHoursStart"
              :disabled="contactSettingForm.businessHoursInherited"
              format="HH:mm"
              placeholder="开始"
              value-format="HH:mm"
            />
            <span>至</span>
            <el-time-picker
              v-model="contactSettingForm.businessHoursEnd"
              :disabled="contactSettingForm.businessHoursInherited"
              format="HH:mm"
              placeholder="结束"
              value-format="HH:mm"
            />
          </div>
        </div>
      </el-form-item>

      <el-divider content-position="left">洞察</el-divider>
      <div class="setting-field-grid">
        <el-form-item label="购买意愿">
          <el-select v-model="contactSettingForm.purchaseIntention">
            <el-option
              v-for="item in purchaseIntentionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="销售阶段">
          <el-select v-model="contactSettingForm.salesStage">
            <el-option
              v-for="item in salesStageOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="客户情绪">
          <el-select v-model="contactSettingForm.customerSentiment">
            <el-option
              v-for="item in customerSentimentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进优先级">
          <el-select v-model="contactSettingForm.followUpPriority">
            <el-option
              v-for="item in followUpPriorityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </div>

      <el-divider content-position="left">标签</el-divider>
      <el-form-item label="人工标签">
        <el-select
          v-model="contactSettingForm.tagIds"
          class="!w-420px"
          collapse-tags
          collapse-tags-tooltip
          filterable
          multiple
          placeholder="请选择客户标签"
        >
          <el-option
            v-for="tag in allTags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id!"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="contactSettingVisible = false">取 消</el-button>
      <el-button :disabled="contactSettingLoading" type="primary" @click="submitContactSettings">
        保 存
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import { getReadableTagStyle } from '@/utils/color'
import { config } from '@/config/axios/config'
import { shouldProxyResourceUrl } from '@/utils/resourceUrl'
import * as ConversationApi from '@/api/agent/conversation'
import * as ContactApi from '@/api/agent/contact'
import * as ReplyPolicyApi from '@/api/agent/replyPolicy'
import * as TagApi from '@/api/agent/tag'
import * as WechatAccountApi from '@/api/agent/wechatAccount'

defineOptions({ name: 'AgentConversation' })

const loading = ref(true)
const message = useMessage()
const router = useRouter()
const currentRoute = useRoute()
const total = ref(0)
const list = ref<ConversationApi.AgentConversationContactVO[]>([])
const syncLoading = ref(false)
const queryFormRef = ref()
const parseRouteNumber = (value: unknown) => {
  const raw = Array.isArray(value) ? value[0] : value
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  wechatAccountId: undefined as number | undefined,
  contactId: parseRouteNumber(currentRoute.query.contactId),
  queueType: typeof currentRoute.query.queueType === 'string' ? currentRoute.query.queueType : ''
})

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const statusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '正常跟进', type: 'success' },
  1: { label: '自动回复', type: 'primary' },
  2: { label: '待确认', type: 'warning' },
  3: { label: '人工接管', type: 'danger' },
  4: { label: '已关闭', type: 'info' }
}

const sendStatusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '已接收', type: 'info' },
  1: { label: '待确认', type: 'warning' },
  2: { label: '已发送', type: 'success' },
  3: { label: '发送失败', type: 'danger' },
  4: { label: '已驳回', type: 'info' }
}

const wechatAccountList = ref<WechatAccountApi.AgentWechatAccountVO[]>([])
const contactList = ref<ContactApi.AgentWechatContactVO[]>([])
const contactCache = ref<Record<number, ContactApi.AgentWechatContactVO>>({})
const contactLoading = ref(false)
const contactKeyword = ref('')
const allTags = ref<TagApi.AgentContactTagVO[]>([])
const activeContactTagIds = ref<number[]>([])
const messages = ref<ConversationApi.AgentMessageVO[]>([])
const mediaObjectUrls = ref<Record<number, string>>({})
const mediaLoadingMap = ref<Record<number, boolean>>({})
const mediaErrorMap = ref<Record<number, string>>({})
const voiceObjectUrls = ref<Record<number, string>>({})
const voiceLoadingMap = ref<Record<number, boolean>>({})
const voiceErrorMap = ref<Record<number, string>>({})
const voicePlayableMap = ref<Record<number, boolean>>({})
const voiceContentTypeMap = ref<Record<number, string>>({})
const activeConversation = ref<ConversationApi.AgentConversationContactVO>()
const activeReplyPolicy = ref<ReplyPolicyApi.AgentReplyPolicyVO>()
const messageLoading = ref(false)
const manualReplyContent = ref('')
const editingMessageId = ref<number>()
const restorePolicyLoading = ref(false)
const contactSettingVisible = ref(false)
const contactSettingLoading = ref(false)
const contactSettingFormRef = ref()
const contactSettingForm = reactive({
  customerLevel: 0,
  replyModeInherited: true,
  replyMode: 'MANUAL_CONFIRM',
  quietSecondsInherited: true,
  quietSeconds: 90 as number | null | undefined,
  businessHoursInherited: true,
  businessHoursStart: '08:00',
  businessHoursEnd: '22:00',
  purchaseIntention: 'MEDIUM',
  salesStage: 'NEW_LEAD',
  customerSentiment: 'NEUTRAL',
  followUpPriority: 'NORMAL',
  tagIds: [] as number[]
})
const activeContact = computed(() => {
  if (!activeConversation.value) return undefined
  return activeConversation.value as unknown as ContactApi.AgentWechatContactVO
})
const activeContactTags = computed(() =>
  allTags.value.filter((tag) => tag.id && activeContactTagIds.value.includes(tag.id))
)
const isHumanTakeover = computed(() =>
  activeConversation.value?.status === 3
)
const riskAlert = reactive({
  total: 0,
  preview: [] as ConversationApi.AgentConversationContactVO[]
})
let riskPollTimer: number | undefined
let lastRiskTotal = 0
let riskAlertReady = false

const customerLevelOptions = [
  { label: '普通', value: 0, type: 'info' as TagType },
  { label: '目标', value: 1, type: 'warning' as TagType },
  { label: '重要', value: 2, type: 'danger' as TagType }
]

const replyModeOptions = [
  { label: '自动回复', value: 'AUTO_REPLY' },
  { label: '人工确认', value: 'MANUAL_CONFIRM' },
  { label: '仅记录', value: 'RECORD_ONLY' }
]

const purchaseIntentionOptions = [
  { label: '低意愿', value: 'LOW', type: 'info' as TagType },
  { label: '中意愿', value: 'MEDIUM', type: 'primary' as TagType },
  { label: '高意愿', value: 'HIGH', type: 'warning' as TagType },
  { label: '强烈意愿', value: 'STRONG', type: 'danger' as TagType }
]

const salesStageOptions = [
  { label: '新线索', value: 'NEW_LEAD' },
  { label: '需求确认', value: 'NEEDS_CONFIRMED' },
  { label: '产品介绍', value: 'PRODUCT_INTRO' },
  { label: '报价议价', value: 'QUOTE_NEGOTIATION' },
  { label: '成交推进', value: 'DEAL_PROGRESS' },
  { label: '售后咨询', value: 'AFTER_SALES' }
]

const customerSentimentOptions = [
  { label: '正向', value: 'POSITIVE', type: 'success' as TagType },
  { label: '中性', value: 'NEUTRAL', type: 'info' as TagType },
  { label: '负向', value: 'NEGATIVE', type: 'warning' as TagType }
]

const followUpPriorityOptions = [
  { label: '普通跟进', value: 'NORMAL', type: 'info' as TagType },
  { label: '重点跟进', value: 'FOCUS', type: 'warning' as TagType },
  { label: '紧急跟进', value: 'URGENT', type: 'danger' as TagType }
]

const loadReferences = async () => {
  const [accounts, tags] = await Promise.all([
    WechatAccountApi.getWechatAccountPage({ pageNo: 1, pageSize: 100 }),
    TagApi.getSimpleTagList()
  ])
  wechatAccountList.value = accounts.list
  allTags.value = tags
  void loadContacts()
}

const cacheContacts = (contacts: ContactApi.AgentWechatContactVO[]) => {
  contacts.forEach((contact) => {
    contactCache.value[contact.id] = contact
  })
}

const loadContacts = async (keyword?: string) => {
  contactLoading.value = true
  try {
    const data = await ContactApi.getWechatContactPage({
      pageNo: 1,
      pageSize: 80,
      wechatAccountId: queryParams.wechatAccountId,
      keyword
    })
    contactList.value = data.list
    cacheContacts(data.list)
  } finally {
    contactLoading.value = false
  }
}

const remoteContactSearch = (keyword: string) => {
  contactKeyword.value = keyword
  loadContacts(keyword)
}

const handleContactChange = () => {
  contactKeyword.value = ''
}

const handleContactClear = () => {
  queryParams.contactId = undefined
  contactKeyword.value = ''
  loadContacts()
}

const handleContactSelectorVisible = (visible: boolean) => {
  if (visible) loadContacts()
}

const handleWechatAccountChange = async () => {
  queryParams.contactId = undefined
  contactKeyword.value = ''
  await loadContacts()
}

const buildConversationQueryParams = () => ({
  ...queryParams,
  keyword: queryParams.contactId ? undefined : contactKeyword.value.trim() || undefined
})

const getList = async () => {
  loading.value = true
  let autoOpenConversation: ConversationApi.AgentConversationContactVO | undefined
  try {
    const data = await ConversationApi.getConversationContactPage(buildConversationQueryParams())
    list.value = data.list
    cacheContacts(data.list as ContactApi.AgentWechatContactVO[])
    total.value = data.total
    if (activeConversation.value) {
      const latest = list.value.find((item) => item.contactId === activeConversation.value?.contactId)
      if (latest) {
        activeConversation.value = latest
      }
    }
    if (activeConversation.value && !list.value.some((item) => item.contactId === activeConversation.value?.contactId)) {
      activeConversation.value = undefined
      messages.value = []
      activeContactTagIds.value = []
    }
    if (!activeConversation.value && list.value[0]) {
      autoOpenConversation = list.value[0]
    }
  } finally {
    loading.value = false
  }
  if (autoOpenConversation) {
    void openConversation(autoOpenConversation)
  }
}

const loadRiskAlert = async () => {
  const data = await ConversationApi.getConversationContactPage({
    pageNo: 1,
    pageSize: 5,
    queueType: 'RISK'
  })
  riskAlert.total = data.total
  riskAlert.preview = data.list
  cacheContacts(data.list as ContactApi.AgentWechatContactVO[])
  lastRiskTotal = data.total
  riskAlertReady = true
}

const openRiskQueue = async (queueType: 'RISK' | 'PENDING_REVIEW' | 'TAKEOVER') => {
  queryParams.queueType = queueType
  queryParams.pageNo = 1
  activeConversation.value = undefined
  messages.value = []
  await router.replace({ query: { ...currentRoute.query, queueType } })
  await getList()
}

const handleQuery = async () => {
  queryParams.pageNo = 1
  activeConversation.value = undefined
  messages.value = []
  await getList()
}

const handleSyncContacts = async () => {
  syncLoading.value = true
  try {
    const result = await ContactApi.syncWechatContacts(queryParams.wechatAccountId)
    message.success(`通讯录同步完成：新增 ${result.createdCount || 0} 个，更新 ${result.updatedCount || 0} 个`)
    await loadContacts()
    await handleQuery()
  } finally {
    syncLoading.value = false
  }
}

const handleQueueChange = () => {
  router.replace({
    query: {
      ...currentRoute.query,
      queueType: queryParams.queueType || undefined
    }
  })
  void handleQuery()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  queryParams.queueType = ''
  contactKeyword.value = ''
  router.replace({ query: { ...currentRoute.query, queueType: undefined } })
  void handleQuery()
}

const openConversation = async (row: ConversationApi.AgentConversationContactVO) => {
  activeConversation.value = row
  activeReplyPolicy.value = undefined
  manualReplyContent.value = ''
  editingMessageId.value = undefined
  const [tagResult, policyResult] = await Promise.allSettled([
    ContactApi.getWechatContactTags(row.contactId),
    ReplyPolicyApi.resolveReplyPolicy({ conversationId: row.conversationId }),
    loadMessages(row.conversationId)
  ])
  activeContactTagIds.value = tagResult.status === 'fulfilled' ? tagResult.value : []
  activeReplyPolicy.value = policyResult.status === 'fulfilled' ? policyResult.value : undefined
  await acknowledgePendingReminder(row)
}

const loadMessages = async (conversationId: number) => {
  messageLoading.value = true
  clearMediaObjectUrls()
  try {
    messages.value = await ConversationApi.getConversationMessages(conversationId)
  } finally {
    messageLoading.value = false
  }
}

const clearMediaObjectUrls = () => {
  Object.values(mediaObjectUrls.value).forEach((url) => URL.revokeObjectURL(url))
  Object.values(voiceObjectUrls.value).forEach((url) => URL.revokeObjectURL(url))
  mediaObjectUrls.value = {}
  mediaLoadingMap.value = {}
  mediaErrorMap.value = {}
  voiceObjectUrls.value = {}
  voiceLoadingMap.value = {}
  voiceErrorMap.value = {}
  voicePlayableMap.value = {}
  voiceContentTypeMap.value = {}
}

const loadGeweMedia = async (item: ConversationApi.AgentMessageVO) => {
  mediaLoadingMap.value = { ...mediaLoadingMap.value, [item.id]: true }
  mediaErrorMap.value = { ...mediaErrorMap.value, [item.id]: '' }
  try {
    const oldUrl = mediaObjectUrls.value[item.id]
    if (oldUrl) URL.revokeObjectURL(oldUrl)
    const blob = await ConversationApi.downloadGeweMedia(item.id)
    const objectUrl = URL.createObjectURL(blob)
    mediaObjectUrls.value = { ...mediaObjectUrls.value, [item.id]: objectUrl }
    return objectUrl
  } catch (error) {
    mediaErrorMap.value = {
      ...mediaErrorMap.value,
      [item.id]: '媒体加载失败，请稍后重试'
    }
    return ''
  } finally {
    mediaLoadingMap.value = { ...mediaLoadingMap.value, [item.id]: false }
  }
}

const downloadGeweFile = async (item: ConversationApi.AgentMessageVO) => {
  const url = mediaObjectUrls.value[item.id] || await loadGeweMedia(item)
  if (!url) return
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = getMediaCaption(item) || `wechat-file-${item.id}`
  anchor.click()
}

const loadVoiceMedia = async (item: ConversationApi.AgentMessageVO) => {
  voiceLoadingMap.value = { ...voiceLoadingMap.value, [item.id]: true }
  voiceErrorMap.value = { ...voiceErrorMap.value, [item.id]: '' }
  try {
    const blob = await ConversationApi.downloadVoiceMedia(item.id)
    const playable = isPlayableVoiceBlob(blob)
    const oldUrl = voiceObjectUrls.value[item.id]
    const objectUrl = URL.createObjectURL(blob)
    if (oldUrl) URL.revokeObjectURL(oldUrl)
    voiceObjectUrls.value = { ...voiceObjectUrls.value, [item.id]: objectUrl }
    voicePlayableMap.value = { ...voicePlayableMap.value, [item.id]: playable }
    voiceContentTypeMap.value = { ...voiceContentTypeMap.value, [item.id]: blob.type || '' }
    if (!playable) {
      voiceErrorMap.value = {
        ...voiceErrorMap.value,
        [item.id]: '已获取原语音，但微信语音是 silk/amr 等浏览器不支持的格式，请先下载原语音。若要在线播放，需要服务器安装语音转码工具。'
      }
    }
  } catch (error) {
    voiceErrorMap.value = {
      ...voiceErrorMap.value,
      [item.id]: '语音加载失败，可能是 Gewe 暂未返回文件或原文件已过期，请稍后重试。'
    }
  } finally {
    voiceLoadingMap.value = { ...voiceLoadingMap.value, [item.id]: false }
  }
}

const isPlayableVoiceBlob = (blob: Blob) => {
  const type = (blob.type || '').toLowerCase()
  if (!type || type.includes('octet-stream') || type.includes('silk') || type.includes('amr')) {
    return false
  }
  return type.includes('mpeg')
    || type.includes('mp3')
    || type.includes('wav')
    || type.includes('ogg')
    || type.includes('webm')
    || type.includes('mp4')
    || type.includes('aac')
}

const getVoiceDownloadName = (item: ConversationApi.AgentMessageVO) => {
  const type = (voiceContentTypeMap.value[item.id] || '').toLowerCase()
  if (type.includes('mpeg') || type.includes('mp3')) return `wechat-voice-${item.id}.mp3`
  if (type.includes('wav')) return `wechat-voice-${item.id}.wav`
  if (type.includes('ogg')) return `wechat-voice-${item.id}.ogg`
  if (type.includes('webm')) return `wechat-voice-${item.id}.webm`
  if (type.includes('mp4')) return `wechat-voice-${item.id}.m4a`
  if (type.includes('amr')) return `wechat-voice-${item.id}.amr`
  return `wechat-voice-${item.id}.silk`
}

const handleVoicePlayError = (item: ConversationApi.AgentMessageVO) => {
  voiceErrorMap.value = {
    ...voiceErrorMap.value,
    [item.id]: '该语音格式当前浏览器不能直接播放，请先下载原语音。若要在线播放，需要服务器安装语音转码工具。'
  }
}

const acknowledgePendingReminder = async (row: ConversationApi.AgentConversationContactVO) => {
  if (row.status !== 2) return
  const acknowledged = await ConversationApi.acknowledgePending(row.conversationId)
  if (!acknowledged) return
  const patch = {
    status: 0,
    lastConversationStatus: 0,
    riskLevel: 0,
    continuousAutoReplyCount: 0
  }
  patchActiveContact(patch)
  const shouldRemoveFromCurrentQueue = ['PENDING_REVIEW', 'RISK'].includes(String(queryParams.queueType || ''))
  if (shouldRemoveFromCurrentQueue) {
    list.value = list.value.filter((item) => item.contactId !== row.contactId)
    total.value = Math.max(0, total.value - 1)
  }
  await loadRiskAlert()
}

const sendManualMessage = async () => {
  if (!activeConversation.value || !manualReplyContent.value.trim()) return
  if (editingMessageId.value) {
    await ConversationApi.approveMessage(editingMessageId.value, manualReplyContent.value.trim())
    message.success('编辑稿已发送')
  } else {
    await ConversationApi.sendMessage(activeConversation.value.conversationId, manualReplyContent.value.trim())
    message.success('人工消息已发送')
  }
  manualReplyContent.value = ''
  editingMessageId.value = undefined
  await loadMessages(activeConversation.value.conversationId)
  await getList()
  await loadRiskAlert()
}

const approveMessage = async (messageId: number) => {
  await ConversationApi.approveMessage(messageId)
  message.success('AI 建议已发送')
  if (activeConversation.value) await loadMessages(activeConversation.value.conversationId)
  await getList()
  await loadRiskAlert()
}

const startEditSuggestion = (item: ConversationApi.AgentMessageVO) => {
  editingMessageId.value = item.id
  manualReplyContent.value = item.content || ''
}

const cancelEditSuggestion = () => {
  editingMessageId.value = undefined
  manualReplyContent.value = ''
}

const rejectMessage = async (messageId: number) => {
  await ConversationApi.rejectMessage(messageId)
  message.success('AI 建议已驳回')
  if (activeConversation.value) await loadMessages(activeConversation.value.conversationId)
}

const restoreOriginalPolicy = async () => {
  if (!activeConversation.value) return
  restorePolicyLoading.value = true
  try {
    await ConversationApi.restoreOriginalPolicy(activeConversation.value.conversationId)
    patchActiveContact({
      status: 0,
      lastConversationStatus: 0,
      riskLevel: 0,
      continuousAutoReplyCount: 0
    })
    message.success('已恢复原策略')
    await getList()
    await loadRiskAlert()
  } finally {
    restorePolicyLoading.value = false
  }
}

const openContactSettingDialog = () => {
  if (!activeContact.value) return
  const hours = activeContact.value.businessHours || {}
  contactSettingForm.customerLevel = activeContact.value.customerLevel ?? 0
  contactSettingForm.replyModeInherited = !activeContact.value.replyMode
  contactSettingForm.replyMode = activeContact.value.replyMode || 'MANUAL_CONFIRM'
  contactSettingForm.quietSecondsInherited = activeContact.value.quietSeconds == null
  contactSettingForm.quietSeconds = activeContact.value.quietSeconds ?? 90
  contactSettingForm.businessHoursInherited = !(typeof hours.start === 'string' && typeof hours.end === 'string')
  contactSettingForm.businessHoursStart = typeof hours.start === 'string' ? hours.start : '08:00'
  contactSettingForm.businessHoursEnd = typeof hours.end === 'string' ? hours.end : '22:00'
  contactSettingForm.purchaseIntention = activeContact.value.purchaseIntention || 'MEDIUM'
  contactSettingForm.salesStage = activeContact.value.salesStage || 'NEW_LEAD'
  contactSettingForm.customerSentiment = activeContact.value.customerSentiment || 'NEUTRAL'
  contactSettingForm.followUpPriority = activeContact.value.followUpPriority || 'NORMAL'
  contactSettingForm.tagIds = [...activeContactTagIds.value]
  contactSettingVisible.value = true
}

const submitContactSettings = async () => {
  if (!activeConversation.value) return
  if (!validateContactPolicySettings()) return
  const contactId = activeConversation.value.contactId
  const policyReq = {
    conversationId: activeConversation.value.conversationId,
    contactId,
    replyMode: contactSettingForm.replyModeInherited ? null : contactSettingForm.replyMode,
    quietSeconds: contactSettingForm.quietSecondsInherited ? null : contactSettingForm.quietSeconds,
    businessHours: buildBusinessHours()
  }
  contactSettingLoading.value = true
  try {
    const result = await ConversationApi.saveContactSettings({
      ...policyReq,
      customerLevel: contactSettingForm.customerLevel,
      purchaseIntention: contactSettingForm.purchaseIntention,
      salesStage: contactSettingForm.salesStage,
      customerSentiment: contactSettingForm.customerSentiment,
      followUpPriority: contactSettingForm.followUpPriority,
      tagIds: contactSettingForm.tagIds
    })
    activeReplyPolicy.value = result.policy as ReplyPolicyApi.AgentReplyPolicyVO
    activeContactTagIds.value = [...(result.tagIds || [])]
    patchActiveContact(result.contact)
    contactSettingVisible.value = false
    message.success('好友设置已保存')
    await getList()
  } finally {
    contactSettingLoading.value = false
  }
}

const buildBusinessHours = () => {
  if (contactSettingForm.businessHoursInherited) {
    return null
  }
  return {
    start: contactSettingForm.businessHoursStart,
    end: contactSettingForm.businessHoursEnd
  }
}

const validateContactPolicySettings = () => {
  if (!contactSettingForm.replyModeInherited && !contactSettingForm.replyMode) {
    message.warning('请选择好友回复策略')
    return false
  }
  if (!contactSettingForm.quietSecondsInherited
    && (!contactSettingForm.quietSeconds || contactSettingForm.quietSeconds < 1)) {
    message.warning('静默秒数不能为空，且至少为 1 秒')
    return false
  }
  if (!contactSettingForm.businessHoursInherited
    && (!contactSettingForm.businessHoursStart || !contactSettingForm.businessHoursEnd)) {
    message.warning('营业时间不能为空')
    return false
  }
  return true
}

const patchActiveContact = (
  patch: Partial<ConversationApi.AgentConversationContactVO & ContactApi.AgentWechatContactVO>
) => {
  if (!activeConversation.value) return
  activeConversation.value = {
    ...activeConversation.value,
    ...patch
  } as ConversationApi.AgentConversationContactVO
  const cached = contactCache.value[activeConversation.value.contactId]
  if (cached) {
    contactCache.value[activeConversation.value.contactId] = {
      ...cached,
      ...patch
    }
  }
  list.value = list.value.map((item) =>
    item.contactId === activeConversation.value?.contactId ? { ...item, ...patch } : item
  )
}

const getMessageTypeLabel = (messageType: number) => {
  const labels: Record<number, string> = {
    1: '[文本消息]',
    3: '[图片消息]',
    34: '[语音消息]',
    43: '[视频消息]',
    47: '[表情]',
    49: '[文件或链接]'
  }
  return labels[messageType] || '[未知消息]'
}

const getStatus = (status?: number) => statusMap[status ?? -1] || { label: '未知', type: 'info' as TagType }
const getSendStatus = (sendStatus?: number) => sendStatusMap[sendStatus ?? 0] || sendStatusMap[0]

const isRiskConversation = (contact?: ConversationApi.AgentConversationContactVO) => {
  if (!contact) return false
  return (contact.riskLevel ?? 0) > 0 || [2, 3].includes(contact.status ?? -1)
}

const getSenderType = (item: ConversationApi.AgentMessageVO): { label: string; type: TagType } => {
  if (item.senderType === 1) return { label: '客户', type: 'info' }
  if (item.senderType === 3) return { label: '人工', type: 'success' }
  if (item.sendStatus === 1) return { label: 'AI 建议', type: 'warning' }
  if (item.senderType === 2) return { label: 'AI 自动', type: 'primary' }
  return { label: '系统', type: 'info' }
}

const cleanWechatText = (value?: unknown) => {
  if (value == null) return ''
  let text = String(value).trim()
  for (let i = 0; i < 3; i++) {
    const match = text.match(/^\{(?:string|str|text|value)=([\s\S]*)\}$/)
    if (match) {
      text = match[1].trim()
      continue
    }
    try {
      const parsed = JSON.parse(text)
      const scalar = parsed?.string ?? parsed?.str ?? parsed?.text ?? parsed?.value
      if (scalar == null) break
      text = String(scalar).trim()
    } catch {
      break
    }
  }
  return text
}

const firstWechatText = (...values: unknown[]) => {
  for (const value of values) {
    const text = cleanWechatText(value)
    if (text && !isRawWechatIdentifier(text)) return text
  }
  return ''
}

const isRawWechatIdentifier = (text: string) => {
  return (
    text === 'weixin' ||
    text.startsWith('wxid_') ||
    text.startsWith('gh_') ||
    text.endsWith('@chatroom')
  )
}

const isChatroomConversation = (contact?: Pick<ConversationApi.AgentConversationContactVO, 'externalUserId'>) => {
  return Boolean(contact?.externalUserId?.endsWith('@chatroom'))
}

const getConversationKindMeta = (
  contact?: Pick<ConversationApi.AgentConversationContactVO, 'externalUserId'>
) => {
  return isChatroomConversation(contact)
    ? { label: '群聊', type: 'warning' as const }
    : { label: '私聊', type: 'info' as const }
}

const formatMessageContent = (item: ConversationApi.AgentMessageVO) => {
  const content = cleanWechatText(item.content)
  if (!content) return getMessageTypeLabel(item.messageType)
  if (item.messageType !== 1 && content.startsWith('<')) {
    return getMessageTypeLabel(item.messageType)
  }
  return content
}

const isImageMessage = (item: ConversationApi.AgentMessageVO) => item.messageType === 3
const isVoiceMessage = (item: ConversationApi.AgentMessageVO) => item.messageType === 34
const isVideoMessage = (item: ConversationApi.AgentMessageVO) => item.messageType === 43
const isEmojiMessage = (item: ConversationApi.AgentMessageVO) => item.messageType === 47
const isFileMessage = (item: ConversationApi.AgentMessageVO) => item.messageType === 49

const formatVoiceDuration = (item: ConversationApi.AgentMessageVO) => {
  if (!item.mediaDurationMillis || item.mediaDurationMillis <= 0) return ''
  const seconds = Math.max(1, Math.round(item.mediaDurationMillis / 1000))
  return `${seconds}s`
}

const renderableMediaUrl = (item: ConversationApi.AgentMessageVO) => {
  return proxiedWechatMediaUrl(item.mediaUrl, item.mediaAesKey)
    || proxiedWechatMediaUrl(item.thumbUrl, item.mediaAesKey)
    || ''
}

const displayMediaUrl = (item: ConversationApi.AgentMessageVO) => {
  return mediaObjectUrls.value[item.id] || renderableMediaUrl(item)
}

const displayThumbUrl = (item: ConversationApi.AgentMessageVO) => {
  return proxiedWechatMediaUrl(item.thumbUrl, item.mediaAesKey)
}

const getMediaPreviewList = (item: ConversationApi.AgentMessageVO) => {
  return [
    mediaObjectUrls.value[item.id],
    proxiedWechatMediaUrl(item.mediaUrl, item.mediaAesKey),
    proxiedWechatMediaUrl(item.thumbUrl, item.mediaAesKey)
  ].filter(Boolean) as string[]
}

const proxiedWechatMediaUrl = (url?: string, aesKey?: string) => {
  if (!url) return ''
  if (!shouldProxyWechatMediaUrl(url)) return url
  const params = new URLSearchParams({ url })
  if (aesKey) params.set('aesKey', aesKey)
  return `${config.base_url}/agent/conversation/media-proxy?${params.toString()}`
}

const shouldProxyWechatMediaUrl = (url: string) => {
  try {
    const parsed = new URL(url)
    const host = parsed.hostname.toLowerCase()
    return ['http:', 'https:'].includes(parsed.protocol)
      && (host === 'qq.com'
        || host.endsWith('.qq.com')
        || host === 'qpic.cn'
        || host.endsWith('.qpic.cn')
        || host === 'qlogo.cn'
        || host.endsWith('.qlogo.cn')
        || host === 'weixin.qq.com'
        || host.endsWith('.weixin.qq.com')
        || host === 'wechat.com'
        || host.endsWith('.wechat.com')
        || shouldProxyResourceUrl(url))
  } catch {
    return false
  }
}

const getMediaCaption = (item: ConversationApi.AgentMessageVO) => {
  if (item.mediaName) return item.mediaName
  const content = formatMessageContent(item)
  return content && !content.startsWith('[') ? content : ''
}

const formatWechatAccount = (account: WechatAccountApi.AgentWechatAccountVO) => {
  return firstWechatText(account.nickname, account.wechatId, account.geweAppId) || `微信账号 #${account.id}`
}

const formatContact = (contact: ContactApi.AgentWechatContactVO) => {
  return firstWechatText(contact.displayName, contact.remark, contact.nickname, contact.wechatId, contact.externalUserId)
    || `客户 #${contact.id}`
}

const formatQueueContact = (contact: ConversationApi.AgentConversationContactVO) => {
  return firstWechatText(contact.displayName, contact.remark, contact.nickname, contact.wechatId, contact.externalUserId)
    || `客户 #${contact.contactId}`
}

const getReplyModeLabel = (replyMode?: string | null) => {
  const labels: Record<string, string> = {
    MANUAL_CONFIRM: '人工确认',
    AUTO_REPLY: '自动回复',
    RECORD_ONLY: '仅记录'
  }
  return labels[replyMode || ''] || '继承微信号'
}

const getWechatAccount = (id?: number) => {
  return wechatAccountList.value.find((item) => item.id === id)
}

const getEffectiveReplyModeLabel = (contact?: ConversationApi.AgentConversationContactVO) => {
  if (activeReplyPolicy.value && activeReplyPolicy.value.conversationId === contact?.conversationId) {
    return getReplyModeLabel(activeReplyPolicy.value.replyMode)
  }
  const account = getWechatAccount(contact?.wechatAccountId)
  return getReplyModeLabel(contact?.replyMode || account?.replyMode || 'MANUAL_CONFIRM')
}

const getEffectiveQuietSeconds = (contact?: ConversationApi.AgentConversationContactVO) => {
  if (activeReplyPolicy.value && activeReplyPolicy.value.conversationId === contact?.conversationId) {
    return activeReplyPolicy.value.quietSeconds || 90
  }
  if (contact?.quietSeconds && contact.quietSeconds > 0) {
    return contact.quietSeconds
  }
  const account = getWechatAccount(contact?.wechatAccountId)
  if (account?.quietSeconds && account.quietSeconds > 0) {
    return account.quietSeconds
  }
  return 90
}

const getEffectiveBusinessHours = (contact?: ConversationApi.AgentConversationContactVO) => {
  if (activeReplyPolicy.value && activeReplyPolicy.value.conversationId === contact?.conversationId) {
    return activeReplyPolicy.value.businessHours || { start: '08:00', end: '22:00' }
  }
  if (contact?.businessHours?.start && contact.businessHours.end) {
    return contact.businessHours
  }
  const account = getWechatAccount(contact?.wechatAccountId)
  if (account?.businessHours?.start && account.businessHours.end) {
    return account.businessHours
  }
  return { start: '08:00', end: '22:00' }
}

const getCustomerLevelMeta = (value?: number) => {
  return customerLevelOptions.find((option) => option.value === value) || customerLevelOptions[0]
}

const getBusinessHoursText = (businessHours?: { start?: string; end?: string } | null) => {
  if (!businessHours?.start || !businessHours?.end) return '继承'
  return `${businessHours.start}-${businessHours.end}`
}

const getEffectiveBusinessHoursText = (contact?: ConversationApi.AgentConversationContactVO) => {
  const businessHours = getEffectiveBusinessHours(contact)
  return getBusinessHoursText(businessHours)
}

const getPolicySourceLabel = (source?: string) => {
  const labels: Record<string, string> = {
    CONTACT: '客户覆盖',
    ACCOUNT: '微信号默认'
  }
  return labels[source || ''] || '-'
}

const getPurchaseIntentionMeta = (value?: string) => {
  return purchaseIntentionOptions.find((option) => option.value === value) || purchaseIntentionOptions[1]
}

const getSalesStageLabel = (value?: string) => {
  return salesStageOptions.find((option) => option.value === value)?.label || salesStageOptions[0].label
}

const getSentimentMeta = (value?: string) => {
  return customerSentimentOptions.find((option) => option.value === value) || customerSentimentOptions[1]
}

const getFollowUpPriorityMeta = (value?: string) => {
  return followUpPriorityOptions.find((option) => option.value === value) || followUpPriorityOptions[0]
}

const getWechatAccountName = (id?: number) => {
  const account = getWechatAccount(id)
  return account ? formatWechatAccount(account) : `微信账号 #${id || '-'}`
}

const getContactName = (id?: number) => {
  const contact = id ? contactCache.value[id] : undefined
  return contact ? formatContact(contact) : `客户 #${id || '-'}`
}

onMounted(async () => {
  void loadReferences()
  void loadRiskAlert()
  await getList()
  riskPollTimer = window.setInterval(loadRiskAlert, 30000)
})

onBeforeUnmount(() => {
  clearMediaObjectUrls()
  if (riskPollTimer) {
    window.clearInterval(riskPollTimer)
  }
})
</script>

<style scoped>
.conversation-workbench {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) 260px;
  gap: 12px;
  height: calc(100dvh - 204px);
  min-width: 1080px;
  min-height: 0;
  overflow: hidden;
}

.risk-workbench-alert {
  margin-bottom: 10px;
}

.risk-alert-title,
.risk-alert-actions,
.risk-alert-preview {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-alert-title {
  justify-content: space-between;
  width: 100%;
}

.risk-alert-preview {
  flex-wrap: wrap;
  margin-top: 6px;
}

.risk-preview-item {
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
  font-size: 12px;
}

.compact-filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.compact-filter-form :deep(.el-form-item) {
  margin-right: 8px;
  margin-bottom: 6px;
}

.compact-filter-form :deep(.el-form-item__label) {
  height: 24px;
  line-height: 24px;
}

.compact-filter-form :deep(.el-input__wrapper),
.compact-filter-form :deep(.el-select__wrapper) {
  min-height: 24px;
}

.compact-filter-form :deep(.el-radio-button__inner) {
  padding: 5px 9px;
}

.queue-form-item :deep(.el-form-item__content) {
  line-height: 24px;
}

.contact-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.contact-option-title {
  overflow: hidden;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-option-meta {
  flex: none;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.conversation-list,
.message-panel,
.context-panel {
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.conversation-list,
.context-panel {
  display: flex;
  flex-direction: column;
}

.list-header,
.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.message-header > :first-child {
  min-width: 0;
}

.list-header {
  font-weight: 600;
}

.list-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  width: 100%;
  margin-bottom: 8px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.conversation-item:hover,
.conversation-item.active {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.conversation-item.needs-human {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.conversation-item.needs-human.takeover {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.conversation-risk-alert {
  margin: 10px 12px 0;
}

.item-main,
.item-meta,
.item-foot,
.header-actions,
.header-tags,
.message-meta,
.message-extra,
.message-actions,
.reply-actions,
.context-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  flex: none;
  justify-content: flex-end;
}

.item-main,
.item-meta,
.item-foot,
.context-row {
  justify-content: space-between;
}

.item-title,
.message-title {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.item-main .item-title {
  flex: 1;
}

.item-title,
.message-title,
.message-subtitle {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-main :deep(.el-tag),
.item-meta :deep(.el-tag),
.item-foot :deep(.el-tag),
.header-tags :deep(.el-tag) {
  flex: none;
}

.item-meta,
.item-foot,
.message-subtitle,
.message-extra {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  background: var(--el-fill-color-extra-light);
}

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 16px;
}

.message-row.outbound {
  align-items: flex-end;
}

.message-bubble {
  max-width: 78%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-sender {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.message-image,
.message-video {
  display: block;
  width: min(320px, 100%);
  max-height: 360px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.message-video {
  object-fit: contain;
}

.message-emoji {
  display: block;
  max-width: 120px;
  max-height: 120px;
}

.message-voice {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}

.voice-load-button {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 180px;
  padding: 7px 10px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 14px;
}

.voice-load-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.voice-duration {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message-audio {
  width: min(280px, 100%);
  height: 32px;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 220px;
}

.message-file-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-media-caption {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.voice-error {
  color: var(--el-color-warning);
}

.message-media-link {
  color: var(--el-color-primary);
  text-decoration: none;
  word-break: break-all;
}

.message-row.outbound .message-bubble {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.message-row.pending .message-bubble {
  border-style: dashed;
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.reply-box {
  position: sticky;
  bottom: 0;
  z-index: 2;
  flex: none;
  padding: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  box-shadow: 0 -8px 18px rgb(0 0 0 / 3%);
}

.reply-actions {
  justify-content: flex-end;
  margin-top: 8px;
}

.context-panel {
  padding: 14px;
  gap: 16px;
  box-sizing: border-box;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.context-section {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.section-title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.section-title-with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.context-row {
  min-height: 28px;
  color: var(--el-text-color-secondary);
}

.context-row strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--el-text-color-primary);
  font-weight: 500;
  text-align: right;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding-top: 4px;
}

.empty-tags {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.empty-panel {
  height: 100%;
}

.business-hours-editor {
  display: flex;
  align-items: center;
  gap: 8px;
}

.business-hours-editor :deep(.el-date-editor.el-input) {
  width: 130px;
}

.policy-control {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}

.field-help {
  flex-basis: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.setting-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.setting-field-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 1280px) {
  .conversation-workbench {
    grid-template-columns: 280px minmax(0, 1fr) 250px;
  }
}

@media (max-width: 960px) {
  .setting-field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
