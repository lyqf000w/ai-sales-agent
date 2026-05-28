package cn.ai.sales.module.agent.service.statistics;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessagePageReqVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessageRespVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsSummaryRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.service.contact.AgentWechatContactDisplayService;
import cn.ai.sales.module.agent.service.conversation.AgentWechatDisplayFormatter;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class AgentStatisticsServiceImpl implements AgentStatisticsService {

    private static final String SCOPE_TODAY_AUTO_REPLY = "TODAY_AUTO_REPLY";

    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentReplyDecisionMapper replyDecisionMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactDisplayService contactDisplayService;

    @Override
    public AgentStatisticsSummaryRespVO getSummary() {
        LocalDateTime beginTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = beginTime.plusDays(1);

        AgentStatisticsSummaryRespVO respVO = new AgentStatisticsSummaryRespVO();
        respVO.setTodayMessageCount(messageMapper.selectCountByMessageTimeBetween(beginTime, endTime));
        respVO.setTodayAutoReplyCount(messageMapper.selectAutoReplyCountByMessageTimeBetween(beginTime, endTime));
        respVO.setPendingReviewCount(replyDecisionMapper.selectPendingReviewCount());
        respVO.setRiskConversationCount(conversationMapper.selectRiskCount());
        respVO.setPurchaseIntentionStats(contactMapper.selectPurchaseIntentionStats());
        respVO.setSalesStageStats(contactMapper.selectSalesStageStats());
        respVO.setCustomerSentimentStats(contactMapper.selectCustomerSentimentStats());
        respVO.setFollowUpPriorityStats(contactMapper.selectFollowUpPriorityStats());
        return respVO;
    }

    @Override
    public PageResult<AgentStatisticsMessageRespVO> getMessagePage(AgentStatisticsMessagePageReqVO pageReqVO) {
        LocalDateTime beginTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = beginTime.plusDays(1);
        boolean autoReplyOnly = StrUtil.equals(pageReqVO.getScope(), SCOPE_TODAY_AUTO_REPLY);
        PageResult<AgentMessageDO> pageResult = messageMapper.selectStatisticsMessagePage(pageReqVO, beginTime,
                endTime, autoReplyOnly);
        List<AgentStatisticsMessageRespVO> list = pageResult.getList().stream()
                .map(this::messageResp)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    private AgentStatisticsMessageRespVO messageResp(AgentMessageDO message) {
        AgentStatisticsMessageRespVO respVO = BeanUtils.toBean(message, AgentStatisticsMessageRespVO.class);
        AgentWechatContactDO contact = message.getContactId() == null ? null : contactMapper.selectById(message.getContactId());
        AgentWechatAccountDO account = message.getWechatAccountId() == null
                ? null : accountMapper.selectById(message.getWechatAccountId());
        respVO.setContactName(contactDisplayService.resolveDisplayName(contact));
        respVO.setAccountName(accountName(account));
        respVO.setContent(AgentWechatDisplayFormatter.formatMessageContent(message.getMessageType(),
                message.getContent()));
        return respVO;
    }

    private String accountName(AgentWechatAccountDO account) {
        if (account == null) {
            return "";
        }
        return StrUtil.firstNonBlank(AgentWechatDisplayFormatter.cleanScalar(account.getNickname()),
                AgentWechatDisplayFormatter.cleanScalar(account.getWechatId()),
                AgentWechatDisplayFormatter.cleanScalar(account.getGeweAppId()));
    }

}
