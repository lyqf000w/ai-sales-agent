package cn.ai.sales.module.agent.controller.open;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.personal-sales-assistant.skill")
@Data
public class PersonalSalesAssistantSkillProperties {

    private String apiKey;
    private Long defaultTenantId = 1L;
    private Integer defaultLimit = 20;

}
