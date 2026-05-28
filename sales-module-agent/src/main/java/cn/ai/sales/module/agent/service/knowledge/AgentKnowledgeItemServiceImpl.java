package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemPageReqVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.dal.mysql.AgentKnowledgeBaseMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentKnowledgeItemMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.KNOWLEDGE_BASE_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.KNOWLEDGE_ITEM_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.KNOWLEDGE_ITEM_TITLE_DUPLICATE;

@Service
@Validated
public class AgentKnowledgeItemServiceImpl implements AgentKnowledgeItemService {

    @Resource
    private AgentKnowledgeItemMapper knowledgeItemMapper;
    @Resource
    private AgentKnowledgeBaseMapper knowledgeBaseMapper;
    @Resource
    private AgentKnowledgeIndexService knowledgeIndexService;

    @Override
    public Long createKnowledgeItem(AgentKnowledgeItemSaveReqVO createReqVO) {
        validateKnowledgeBaseExists(createReqVO.getKnowledgeBaseId());
        validateTitleDuplicate(createReqVO.getKnowledgeBaseId(), createReqVO.getTitle(), null);
        AgentKnowledgeItemDO item = BeanUtils.toBean(createReqVO, AgentKnowledgeItemDO.class);
        item.setEmbedding(null);
        item.setEmbeddingStatus(AgentConstants.EMBEDDING_STATUS_PENDING);
        knowledgeItemMapper.insert(item);
        knowledgeIndexService.rebuildItemIndex(item);
        return item.getId();
    }

    @Override
    public void updateKnowledgeItem(AgentKnowledgeItemSaveReqVO updateReqVO) {
        validateKnowledgeItemExists(updateReqVO.getId());
        validateKnowledgeBaseExists(updateReqVO.getKnowledgeBaseId());
        validateTitleDuplicate(updateReqVO.getKnowledgeBaseId(), updateReqVO.getTitle(), updateReqVO.getId());
        AgentKnowledgeItemDO item = BeanUtils.toBean(updateReqVO, AgentKnowledgeItemDO.class);
        item.setEmbedding(null);
        item.setEmbeddingStatus(AgentConstants.EMBEDDING_STATUS_PENDING);
        knowledgeItemMapper.updateById(item);
        knowledgeIndexService.rebuildItemIndex(item);
    }

    @Override
    public void deleteKnowledgeItem(Long id) {
        validateKnowledgeItemExists(id);
        knowledgeIndexService.deleteItemIndex(id);
        knowledgeItemMapper.deleteById(id);
    }

    @Override
    public AgentKnowledgeItemDO getKnowledgeItem(Long id) {
        return knowledgeItemMapper.selectById(id);
    }

    @Override
    public PageResult<AgentKnowledgeItemDO> getKnowledgeItemPage(AgentKnowledgeItemPageReqVO pageReqVO) {
        return knowledgeItemMapper.selectPage(pageReqVO);
    }

    @Override
    public void rebuildKnowledgeItemIndex(Long id) {
        AgentKnowledgeItemDO item = knowledgeItemMapper.selectById(id);
        if (item == null) {
            throw exception(KNOWLEDGE_ITEM_NOT_EXISTS);
        }
        knowledgeIndexService.rebuildItemIndex(item);
    }

    private void validateKnowledgeItemExists(Long id) {
        if (knowledgeItemMapper.selectById(id) == null) {
            throw exception(KNOWLEDGE_ITEM_NOT_EXISTS);
        }
    }

    private void validateKnowledgeBaseExists(Long id) {
        if (knowledgeBaseMapper.selectById(id) == null) {
            throw exception(KNOWLEDGE_BASE_NOT_EXISTS);
        }
    }

    private void validateTitleDuplicate(Long knowledgeBaseId, String title, Long id) {
        AgentKnowledgeItemDO item = knowledgeItemMapper.selectByTitle(knowledgeBaseId, title);
        if (item == null) {
            return;
        }
        if (id == null || !Objects.equals(item.getId(), id)) {
            throw exception(KNOWLEDGE_ITEM_TITLE_DUPLICATE);
        }
    }

}
