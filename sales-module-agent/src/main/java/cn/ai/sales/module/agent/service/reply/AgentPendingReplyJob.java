package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.framework.tenant.core.job.TenantJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class AgentPendingReplyJob {

    @Resource
    private AgentAutoReplyService autoReplyService;

    @Scheduled(fixedDelay = 5000L, initialDelay = 5000L)
    @TenantJob
    public void processPendingReplies() {
        autoReplyService.processDuePendingReplies(LocalDateTime.now());
    }

}
