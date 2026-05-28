package cn.ai.sales.module.agent.service.review;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationMessageReviewReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewApproveReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewPageReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRejectReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRespVO;

public interface AgentReplyReviewService {

    PageResult<AgentReplyReviewRespVO> getReviewPage(AgentReplyReviewPageReqVO pageReqVO);

    Long approve(AgentReplyReviewApproveReqVO approveReqVO);

    void reject(AgentReplyReviewRejectReqVO rejectReqVO);

    Long approveByMessage(AgentConversationMessageReviewReqVO reviewReqVO);

    void rejectByMessage(AgentConversationMessageReviewReqVO reviewReqVO);

}
