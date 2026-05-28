package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBasePageReqVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBaseSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeBaseDO;
import cn.ai.sales.module.agent.dal.mysql.AgentKnowledgeBaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.KNOWLEDGE_BASE_NAME_DUPLICATE;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.KNOWLEDGE_BASE_NOT_EXISTS;

@Service
@Validated
public class AgentKnowledgeBaseServiceImpl implements AgentKnowledgeBaseService {

    @Resource
    private AgentKnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public Long createKnowledgeBase(AgentKnowledgeBaseSaveReqVO createReqVO) {
        validateNameDuplicate(createReqVO.getName(), null);
        AgentKnowledgeBaseDO base = BeanUtils.toBean(createReqVO, AgentKnowledgeBaseDO.class);
        knowledgeBaseMapper.insert(base);
        return base.getId();
    }

    @Override
    public void updateKnowledgeBase(AgentKnowledgeBaseSaveReqVO updateReqVO) {
        validateKnowledgeBaseExists(updateReqVO.getId());
        validateNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        knowledgeBaseMapper.updateById(BeanUtils.toBean(updateReqVO, AgentKnowledgeBaseDO.class));
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        validateKnowledgeBaseExists(id);
        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    public AgentKnowledgeBaseDO getKnowledgeBase(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public PageResult<AgentKnowledgeBaseDO> getKnowledgeBasePage(AgentKnowledgeBasePageReqVO pageReqVO) {
        return knowledgeBaseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AgentKnowledgeBaseDO> getEnabledKnowledgeBaseList() {
        return knowledgeBaseMapper.selectEnabledList();
    }

    private void validateKnowledgeBaseExists(Long id) {
        if (knowledgeBaseMapper.selectById(id) == null) {
            throw exception(KNOWLEDGE_BASE_NOT_EXISTS);
        }
    }

    private void validateNameDuplicate(String name, Long id) {
        AgentKnowledgeBaseDO base = knowledgeBaseMapper.selectByName(name);
        if (base == null) {
            return;
        }
        if (id == null || !Objects.equals(base.getId(), id)) {
            throw exception(KNOWLEDGE_BASE_NAME_DUPLICATE);
        }
    }

}
