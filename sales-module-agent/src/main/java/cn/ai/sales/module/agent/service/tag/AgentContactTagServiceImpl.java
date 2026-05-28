package cn.ai.sales.module.agent.service.tag;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateTagsReqVO;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagPageReqVO;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagRelDO;
import cn.ai.sales.module.agent.dal.mysql.AgentContactTagMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentContactTagRelMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONTACT_TAG_NAME_DUPLICATE;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONTACT_TAG_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_CONTACT_NOT_EXISTS;

@Service
@Validated
public class AgentContactTagServiceImpl implements AgentContactTagService {

    @Resource
    private AgentContactTagMapper tagMapper;
    @Resource
    private AgentContactTagRelMapper tagRelMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;

    @Override
    public Long createTag(AgentContactTagSaveReqVO createReqVO) {
        validateTagNameDuplicate(createReqVO.getName(), null);
        AgentContactTagDO tag = BeanUtils.toBean(createReqVO, AgentContactTagDO.class);
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public void updateTag(AgentContactTagSaveReqVO updateReqVO) {
        validateTagExists(updateReqVO.getId());
        validateTagNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        tagMapper.updateById(BeanUtils.toBean(updateReqVO, AgentContactTagDO.class));
    }

    @Override
    public void deleteTag(Long id) {
        validateTagExists(id);
        tagMapper.deleteById(id);
    }

    @Override
    public AgentContactTagDO getTag(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    public PageResult<AgentContactTagDO> getTagPage(AgentContactTagPageReqVO pageReqVO) {
        return tagMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AgentContactTagDO> getEnabledTags() {
        return tagMapper.selectEnabledList();
    }

    @Override
    public List<Long> getContactTagIds(Long contactId) {
        validateContactExists(contactId);
        return tagRelMapper.selectListByContactId(contactId).stream()
                .map(AgentContactTagRelDO::getTagId)
                .toList();
    }

    @Override
    public void updateContactTags(AgentWechatContactUpdateTagsReqVO updateReqVO) {
        validateContactExists(updateReqVO.getContactId());
        List<Long> tagIds = updateReqVO.getTagIds() == null ? Collections.emptyList()
                : updateReqVO.getTagIds().stream().distinct().toList();
        for (Long tagId : tagIds) {
            validateTagExists(tagId);
        }
        tagRelMapper.deleteByContactId(updateReqVO.getContactId());
        for (Long tagId : tagIds) {
            AgentContactTagRelDO rel = new AgentContactTagRelDO();
            rel.setContactId(updateReqVO.getContactId());
            rel.setTagId(tagId);
            tagRelMapper.insert(rel);
        }
    }

    private void validateContactExists(Long contactId) {
        if (contactMapper.selectById(contactId) == null) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }
    }

    private void validateTagExists(Long id) {
        if (tagMapper.selectById(id) == null) {
            throw exception(CONTACT_TAG_NOT_EXISTS);
        }
    }

    private void validateTagNameDuplicate(String name, Long id) {
        AgentContactTagDO tag = tagMapper.selectByName(name);
        if (tag == null) {
            return;
        }
        if (id == null || !Objects.equals(tag.getId(), id)) {
            throw exception(CONTACT_TAG_NAME_DUPLICATE);
        }
    }

}
