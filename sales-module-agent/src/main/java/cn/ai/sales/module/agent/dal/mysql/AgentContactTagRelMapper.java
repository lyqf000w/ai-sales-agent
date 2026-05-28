package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagRelDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentContactTagRelMapper extends BaseMapperX<AgentContactTagRelDO> {

    default List<AgentContactTagRelDO> selectListByContactId(Long contactId) {
        return selectList(new LambdaQueryWrapper<AgentContactTagRelDO>()
                .eq(AgentContactTagRelDO::getContactId, contactId)
                .orderByAsc(AgentContactTagRelDO::getId));
    }

    default void deleteByContactId(Long contactId) {
        delete(new LambdaQueryWrapper<AgentContactTagRelDO>()
                .eq(AgentContactTagRelDO::getContactId, contactId));
    }

}
