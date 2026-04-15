package com.ran.aiagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {
    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools(){
//        TerminateTool terminateTool = new TerminateTool();
        PolicyNewsTool policyNewsTool = new PolicyNewsTool(searchApiKey);
        return ToolCallbacks.from(
                policyNewsTool
//                terminateTool
        );
    }
}
