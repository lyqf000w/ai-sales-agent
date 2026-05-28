package cn.ai.sales.module.agent.service.tag;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateTagsReqVO;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagPageReqVO;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import jakarta.validation.Valid;

import java.util.List;

public interface AgentContactTagService {

    Long createTag(@Valid AgentContactTagSaveReqVO createReqVO);

    void updateTag(@Valid AgentContactTagSaveReqVO updateReqVO);

    void deleteTag(Long id);

    AgentContactTagDO getTag(Long id);

    PageResult<AgentContactTagDO> getTagPage(AgentContactTagPageReqVO pageReqVO);

    List<AgentContactTagDO> getEnabledTags();

    List<Long> getContactTagIds(Long contactId);

    void updateContactTags(@Valid AgentWechatContactUpdateTagsReqVO updateReqVO);

}
