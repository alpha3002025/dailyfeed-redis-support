package click.dailyfeed.redis.config.redis;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.cache.RedisKeyConstant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:26379}")
    private Integer redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean // 1m
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("jsonRedisSerializer") GenericJackson2JsonRedisSerializer jsonRedisSerializer
    ) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
//                .prefixCacheNameWith("myapp:")  // 키 접두사
                .disableCachingNullValues()     // null 값 캐싱 비활성화
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonRedisSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        /// post service
        RedisKeyConstant.PostService.INTERNAL_QUERY_LIST_BY_IDS_IN_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(5))));
        RedisKeyConstant.PostService.GET_ITEM_BY_ID_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(3))));
        RedisKeyConstant.PostService.GET_PAGE_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(20))));
        RedisKeyConstant.PostService.STATISTICS_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(30))));
        RedisKeyConstant.PostService.SEARCH_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(1))));
        /// comment service
        RedisKeyConstant.CommentService.GET_ITEM_BY_ID_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(3))));
        RedisKeyConstant.CommentService.GET_PAGE_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(20))));
        /// member redis service
        RedisKeyConstant.MemberRedisService.INTERNAL_QUERY_LIST_BY_IDS_IN_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(5))));
        RedisKeyConstant.MemberRedisService.GET_ITEM_BY_ID_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(10))));
        /// follow redis service
        RedisKeyConstant.FollowRedisService.INTERNAL_QUERY_LIST_BY_IDS_IN_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(5))));
        RedisKeyConstant.FollowRedisService.GET_PAGE_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(20))));
        RedisKeyConstant.FollowRedisService.GET_ITEM_BY_ID_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(3))));
        /// timeline pull service
        RedisKeyConstant.TimelinePullService.GET_PAGE_KEYS.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofSeconds(90))));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .transactionAware()  // 트랜잭션 지원
                .build();
    }

    @Bean
    public GenericJackson2JsonRedisSerializer jsonRedisSerializer(
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper
    ) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    @Bean
    RedisTemplate<String, MemberDto.Member> memberDtoRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper
    ){
        RedisTemplate<String, MemberDto.Member> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<MemberDto.Member> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        redisObjectMapper,
                        MemberDto.Member.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, PostDto.Post>  postDtoPostRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper postActivityEventObjectMapper
    ){
        RedisTemplate<String, PostDto.Post> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<PostDto.Post> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        postActivityEventObjectMapper,
                        PostDto.Post.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, CommentDto.CommentActivityEvent>  commentActivityEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisCommonObjectMapper
    ){
        RedisTemplate<String, CommentDto.CommentActivityEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<PostDto.PostActivityEvent> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        redisCommonObjectMapper,
                        PostDto.PostActivityEvent.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, TimelineDto.TimelinePostActivity> timelinePostActivityRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper commonObjectMapper
    ){
        RedisTemplate<String, TimelineDto.TimelinePostActivity> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<TimelineDto.TimelinePostActivity> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        commonObjectMapper,
                        TimelineDto.TimelinePostActivity.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, PostDto.LikeActivityEvent>  postLikeActivityEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisCommonObjectMapper
    ){
        RedisTemplate<String, PostDto.LikeActivityEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<PostDto.LikeActivityEvent> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        redisCommonObjectMapper,
                        PostDto.LikeActivityEvent.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, CommentDto.LikeActivityEvent>  commentLikeActivityEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisCommonObjectMapper
    ){
        RedisTemplate<String, CommentDto.LikeActivityEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<CommentDto.LikeActivityEvent> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        redisCommonObjectMapper,
                        CommentDto.LikeActivityEvent.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, PostDto.PostActivityEvent>  postActivityEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper postActivityEventObjectMapper
    ){
        RedisTemplate<String, PostDto.PostActivityEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<PostDto.PostActivityEvent> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        postActivityEventObjectMapper,
                        PostDto.PostActivityEvent.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
