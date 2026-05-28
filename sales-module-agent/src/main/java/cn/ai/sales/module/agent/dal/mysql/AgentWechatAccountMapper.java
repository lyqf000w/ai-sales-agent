package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentWechatAccountMapper extends BaseMapperX<AgentWechatAccountDO> {

    default PageResult<AgentWechatAccountDO> selectPage(AgentWechatAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentWechatAccountDO>()
                .likeIfPresent(AgentWechatAccountDO::getNickname, reqVO.getKeyword())
                .eqIfPresent(AgentWechatAccountDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AgentWechatAccountDO::getLoginStatus, reqVO.getLoginStatus())
                .betweenIfPresent(AgentWechatAccountDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AgentWechatAccountDO::getId));
    }

    default AgentWechatAccountDO selectByCallbackToken(String callbackToken) {
        return selectOne(AgentWechatAccountDO::getCallbackToken, callbackToken);
    }

    default AgentWechatAccountDO selectByGeweAppId(String geweAppId) {
        return selectOne(AgentWechatAccountDO::getGeweAppId, geweAppId);
    }

    default AgentWechatAccountDO selectByCredentialAndAppId(Long credentialId, String appId) {
        return selectOne(AgentWechatAccountDO::getGeweCredentialId, credentialId,
                AgentWechatAccountDO::getGeweAppId, appId);
    }

    default AgentWechatAccountDO selectByCredentialAndWxid(Long credentialId, String wxid) {
        return selectOne(AgentWechatAccountDO::getGeweCredentialId, credentialId,
                AgentWechatAccountDO::getWechatId, wxid);
    }

    default AgentWechatAccountDO selectByWxid(String wxid) {
        return selectOne(AgentWechatAccountDO::getWechatId, wxid);
    }

    default Long selectOnlineCount() {
        return selectCount(new LambdaQueryWrapperX<AgentWechatAccountDO>()
                .eq(AgentWechatAccountDO::getLoginStatus, AgentConstants.LOGIN_STATUS_ONLINE));
    }

    default List<AgentWechatAccountDO> selectSyncableList() {
        return selectList(new LambdaQueryWrapperX<AgentWechatAccountDO>()
                .eq(AgentWechatAccountDO::getStatus, AgentConstants.STATUS_ENABLE)
                .eq(AgentWechatAccountDO::getLoginStatus, AgentConstants.LOGIN_STATUS_ONLINE)
                .orderByDesc(AgentWechatAccountDO::getId));
    }

}
