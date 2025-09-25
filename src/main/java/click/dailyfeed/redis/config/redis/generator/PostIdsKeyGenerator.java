package click.dailyfeed.redis.config.redis.generator;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@Component("postIdsKeyGenerator")
public class PostIdsKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length > 0 && params[0] instanceof PostDto.PostsBulkRequest) {
            PostDto.PostsBulkRequest request = (PostDto.PostsBulkRequest) params[0];
            Set<Long> ids = request.getIds();

            // Set이 null이거나 비어있는 경우 빈 List 반환
            if (ids == null || ids.isEmpty()) {
                return "empty_ids";  // 또는 특별한 키 값 사용
            }

            // 정상적인 경우 정렬된 ID들을 문자열로 조합
            return ids.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining("_"));
        }
        return "unknown";
    }
}
