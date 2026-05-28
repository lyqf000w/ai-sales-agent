package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatBindSessionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentWechatBindSessionMapper extends BaseMapperX<AgentWechatBindSessionDO> {
}
