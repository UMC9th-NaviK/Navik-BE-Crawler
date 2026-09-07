package navik.growth.pipeline.e2e;

import java.util.function.Function;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.*;
import navik.ai.config.ChatClientConfig;
import navik.ai.client.EmbeddingClient;
import navik.ai.util.PromptLoader;
import navik.growth.analysis.controller.GrowthAnalysisController;
import navik.growth.analysis.service.GrowthAnalysisService;
import navik.growth.analysis.service.GrowthEmbeddingService;
import navik.growth.analysis.service.parser.ResponseParser;
import navik.growth.analysis.service.prompt.PromptBuilder;
import navik.growth.analysis.service.util.ContentTypeHelper;
import navik.growth.analysis.strategy.PersonaPromptLoader;
import navik.growth.tool.dto.ToolRequests.LevelCriteriaRequest;
import navik.growth.tool.service.LevelCriteriaService;
import navik.growth.pipeline.GrowthPipelineConfiguration;
import navik.redis.config.RedisConfig;

/** Real growth/AI/Redisson components; external OpenAI HTTP is redirected to the fixture server. */
@org.springframework.boot.test.context.TestConfiguration
@EnableAutoConfiguration(exclude=DataSourceAutoConfiguration.class)
@Import({GrowthPipelineConfiguration.class, RedisConfig.class, ChatClientConfig.class, EmbeddingClient.class,
    PromptLoader.class, GrowthAnalysisController.class, GrowthAnalysisService.class, GrowthEmbeddingService.class,
    ResponseParser.class, PromptBuilder.class, ContentTypeHelper.class, PersonaPromptLoader.class, LevelCriteriaService.class})
public class GrowthCrawlerHarness {
    public static void main(String[] args) {
        if (!"true".equals(System.getenv("PIPELINE_E2E_ENABLED")))
            throw new IllegalStateException("Explicit isolated E2E opt-in required");
        SpringApplication.run(GrowthCrawlerHarness.class, args);
    }
    @Bean @Description("Load the level criteria for the isolated growth experiment")
    Function<LevelCriteriaRequest, String> retrieveLevelCriteria(LevelCriteriaService criteria) {
        return r -> criteria.findCriteria(r.jobId(), r.levelValue());
    }
}
