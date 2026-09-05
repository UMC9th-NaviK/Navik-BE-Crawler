package navik.growth.pipeline;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import navik.growth.analysis.service.GrowthAnalysisService;
import navik.growth.analysis.service.GrowthEmbeddingService;
import navik.pipeline.PipelineStreamConsumer;

@Configuration
@ConditionalOnProperty(name = "navik.growth-pipeline.enabled", havingValue = "true")
public class GrowthPipelineConfiguration {
    @Bean(destroyMethod = "close")
    public HikariDataSource pipelineDataSource(@Value("${navik.growth-pipeline.jdbc-url}") String url,
        @Value("${navik.growth-pipeline.username}") String username,
        @Value("${navik.growth-pipeline.password}") String password) {
        var config = new HikariConfig();
        config.setJdbcUrl(url); config.setUsername(username); config.setPassword(password);
        config.setMaximumPoolSize(8); config.setConnectionTimeout(5000);
        config.setPoolName("growth-pipeline");
        return new HikariDataSource(config);
    }

    @Bean
    public GrowthJobStore growthJobStore(@Qualifier("pipelineDataSource") DataSource dataSource) {
        return new GrowthJobStore(new JdbcTemplate(dataSource),
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)), Duration.ofSeconds(90));
    }

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService pipelineHeartbeats() { return Executors.newScheduledThreadPool(2); }

    @Bean
    public GrowthStageProcessor growthStageProcessor(GrowthJobStore store, GrowthAnalysisService analysis,
        GrowthEmbeddingService embeddings, ObjectMapper mapper, StringRedisTemplate redis,
        ScheduledExecutorService pipelineHeartbeats,
        @Value("${navik.growth-pipeline.max-attempts:3}") int maxAttempts,
        @Value("${navik.growth-pipeline.starts-per-minute:30}") int startsPerMinute,
        @Value("${navik.growth-pipeline.max-runtime-seconds:300}") long maxRuntime,
        @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}") String embeddingModel) {
        if (maxAttempts < 1 || startsPerMinute < 1 || maxRuntime < 30) throw new IllegalArgumentException("Invalid pipeline limits");
        return new GrowthStageProcessor(store, analysis, embeddings, mapper, redis, pipelineHeartbeats,
            maxAttempts, startsPerMinute, Duration.ofSeconds(maxRuntime), embeddingModel);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public PipelineStreamConsumer growthAnalysisConsumer(StringRedisTemplate redis, GrowthStageProcessor processor,
        @Value("${navik.growth-pipeline.analysis-concurrency:2}") int concurrency) {
        return new PipelineStreamConsumer(redis, "{growth-v2}:analyze", "growth-analysis-v2", concurrency,
            Duration.ofSeconds(120), id -> processor.handle(id, "ANALYZE"));
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public PipelineStreamConsumer growthEmbeddingConsumer(StringRedisTemplate redis, GrowthStageProcessor processor,
        @Value("${navik.growth-pipeline.embedding-concurrency:2}") int concurrency) {
        return new PipelineStreamConsumer(redis, "{growth-v2}:embed", "growth-embedding-v2", concurrency,
            Duration.ofSeconds(120), id -> processor.handle(id, "EMBED"));
    }
}
