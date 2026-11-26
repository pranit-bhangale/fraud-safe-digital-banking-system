package com.banking.helpdesk.config;

import com.banking.helpdesk.constant.AiConstant;
import com.banking.helpdesk.guardrail.advisor.StrictGuardrailAdvisor;
import com.banking.helpdesk.guardrail.properties.GuardrailProperties;
import com.banking.helpdesk.guardrail.service.GuardrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.WindowCompactionStrategy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * Spring AI configuration registering strict input/output guardrails, conversation memory,
 * and the pre-configured {@link ChatClient} bean with domain-centric tools.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({GuardrailProperties.class})
public class AgentConfiguration {

    public static final int DEFAULT_MAX_MEMORY_MESSAGES = 20;

    /**
     * Outermost: runs before chat memory, RAG and tools.
     */
    @Bean
    public StrictGuardrailAdvisor inputGuardrailAdvisor(GuardrailService service, GuardrailProperties properties) {
        return new StrictGuardrailAdvisor(service, properties, StrictGuardrailAdvisor.Stage.INPUT, Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * Innermost: runs right after the model, BEFORE the memory advisor persists the answer.
     * Its order number must be larger than every other advisor's (memory, RAG, tool-call).
     */
    @Bean
    public StrictGuardrailAdvisor outputGuardrailAdvisor(GuardrailService service, GuardrailProperties properties) {
        return new StrictGuardrailAdvisor(service, properties, StrictGuardrailAdvisor.Stage.OUTPUT, Ordered.LOWEST_PRECEDENCE - 100);
    }

    /**
     * Makes silent misconfiguration (wrong prefix, missing yml) visible at startup.
     */
    @Bean
    public InitializingBean guardrailSelfCheck(GuardrailProperties p) {
        return () -> {
            if (!p.enabled()) {
                log.warn("[GUARDRAIL] [DISABLED] Guardrails are OFF. Do not run like this in production.");
                return;
            }
            log.info("[GUARDRAIL] [READY] strict: {}, maxInputLength: {}, keywords: {}, inputPatterns: {}, outputPatterns: {}, scope: {}",
                    p.strictMode(), p.maxInputLength(), p.blockedWords().size(),
                    p.inputPatterns().size(), p.outputPatterns().size(), p.scope().enabled());
            if (p.blockedWords().isEmpty() && p.inputPatterns().isEmpty() && p.outputPatterns().isEmpty()) {
                log.warn("[GUARDRAIL] [NO-RULES] Enabled but no keywords or patterns are configured.");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionService sessionService() {
        return DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
    }

    @Bean
    public List<Advisor> sessionAdvisors(SessionService sessionService) {
        return SessionAdvisors.builder(sessionService)
                .compaction(new TurnCountTrigger(20),
                        WindowCompactionStrategy.builder().lastTurns(10).build())
                .build();
    }


    @Bean("chatClient")
    public ChatClient chatClient(
            ChatClient.Builder builder,
            @Qualifier("inputGuardrailAdvisor") StrictGuardrailAdvisor inputGuardrailAdvisor,
            @Qualifier("outputGuardrailAdvisor") StrictGuardrailAdvisor outputGuardrailAdvisor,
            @Qualifier("sessionAdvisors") List<Advisor> sessionAdvisors
    ) {
        return builder
                .defaultSystem(AiConstant.SYSTEM_PROMPT)
                .defaultAdvisors(inputGuardrailAdvisor)
                .defaultAdvisors(sessionAdvisors)
                .defaultAdvisors(outputGuardrailAdvisor)
                .build();
    }
}
