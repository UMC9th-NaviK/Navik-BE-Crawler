package navik.growth.pipeline;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import navik.ai.client.EmbeddingClient;
import navik.growth.analysis.dto.AnalysisDraft;
import navik.growth.analysis.dto.AnalysisRequest.GrowthAnalysisRequest;
import navik.growth.analysis.dto.AnalysisResponse.GrowthAnalysisResponse;
import navik.growth.analysis.service.GrowthAnalysisService;
import navik.growth.analysis.service.GrowthEmbeddingService;

@EnabledIfEnvironmentVariable(named = "PIPELINE_TEST_JDBC_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PIPELINE_TEST_REDIS_PORT", matches = "[0-9]+")
class GrowthStageProcessorIntegrationTest {
    @Test
    void persistedAnalysisAndPartialEmbeddingResumeWithoutRepeatingLlm() throws Exception {
        String url = System.getenv("PIPELINE_TEST_JDBC_URL");
        var admin = new DriverManagerDataSource(url, "pipeline_test", "");
        String schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(admin).execute("CREATE SCHEMA " + schema);
        var dataSource = new DriverManagerDataSource(url + "?currentSchema=" + schema, "pipeline_test", "");
        new ResourceDatabasePopulator(new ClassPathResource("growth-pipeline.sql")).execute(dataSource);
        var jdbc = new JdbcTemplate(dataSource);
        var store = new GrowthJobStore(jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)), Duration.ofSeconds(90));
        var config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:" + System.getenv("PIPELINE_TEST_REDIS_PORT"));
        var client = Redisson.create(config);
        var heartbeats = Executors.newSingleThreadScheduledExecutor();
        try {
            var redis = new StringRedisTemplate(new RedissonConnectionFactory(client));
            var mapper = new ObjectMapper().findAndRegisterModules();
            var analysis = mock(GrowthAnalysisService.class);
            when(analysis.analyzeDraft(any())).thenReturn(new AnalysisDraft("title", "content", List.of(), List.of("first", "second")));
            var embedding = mock(EmbeddingClient.class);
            when(embedding.embed("first")).thenReturn(new float[1536]);
            when(embedding.embed("second")).thenThrow(new RuntimeException("transient")).thenReturn(new float[1536]);
            var processor = new GrowthStageProcessor(store, analysis, new GrowthEmbeddingService(embedding), mapper,
                redis, heartbeats, 3, 10000, Duration.ofSeconds(300), "test-model");
            String id = UUID.randomUUID().toString();
            var input = new GrowthAnalysisRequest(1L,1L,1,
                new GrowthAnalysisRequest.Context("resume", List.of(), List.of(), "new content"));
            jdbc.update("INSERT INTO growth_analysis_job(id,user_id,growth_log_id,processing_token,input_json) VALUES (?,1,2,?,?)",
                id, UUID.randomUUID().toString(), mapper.writeValueAsString(input));
            assertThat(processor.handle(id, "ANALYZE")).isTrue();
            assertThat(processor.handle(id, "EMBED")).isTrue(); // retry is durably scheduled, so message may ACK
            assertThat(store.get(id).orElseThrow().state()).isEqualTo("READY");
            assertThat(mapper.readValue(store.get(id).orElseThrow().result(), GrowthAnalysisResponse.class).abilities()).hasSize(1);
            jdbc.update("UPDATE growth_analysis_job SET next_attempt_at=CURRENT_TIMESTAMP WHERE id=?", id);
            assertThat(processor.handle(id, "EMBED")).isTrue();
            assertThat(store.get(id).orElseThrow().stage()).isEqualTo("APPLY");
            assertThat(mapper.readValue(store.get(id).orElseThrow().result(), GrowthAnalysisResponse.class).abilities()).hasSize(2);
            assertThat(processor.handle(id, "ANALYZE")).isTrue(); // duplicate old stage is ignored
            verify(analysis, times(1)).analyzeDraft(any());
            verify(embedding, times(1)).embed("first");
            verify(embedding, times(2)).embed("second");
        } finally {
            heartbeats.shutdownNow(); client.shutdown();
        }
    }
}
