package click.dailyfeed.redis.global.deadletter.kafka;

import click.dailyfeed.code.domain.activity.transport.MemberActivityTransportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static click.dailyfeed.code.global.cache.RedisKeyPrefix.MEMBER_ACTIVITY_KAFKA_LISTENER;
import static click.dailyfeed.code.global.cache.RedisKeyPrefix.MEMBER_ACTIVITY_KAFKA_DLQ;

@RequiredArgsConstructor
@Service
public class MemberActivityEventRedisService {
    private final static String redisKey = MEMBER_ACTIVITY_KAFKA_LISTENER.getKeyPrefix().substring(0, MEMBER_ACTIVITY_KAFKA_LISTENER.getKeyPrefix().length() - 1);
    private final static String redisDLQKey = MEMBER_ACTIVITY_KAFKA_DLQ.getKeyPrefix().substring(0, MEMBER_ACTIVITY_KAFKA_DLQ.getKeyPrefix().length() - 1);

    @Value("${infrastructure.redis.event-queue.member-activity-event.dead-letter-list-key}")
    private String deadLetterKey;

    @Value("${infrastructure.redis.event-queue.member-activity-event.batch-size}")
    private Integer batchSize;

    @Qualifier("memberActivityTransportDtoRedisTemplate")
    private final RedisTemplate<String, MemberActivityTransportDto.MemberActivityMessage> redisTemplate;

    public void rPushEvent(MemberActivityTransportDto.MemberActivityMessage message) {
        redisTemplate.opsForList().rightPush(redisKey, message);
    }

    public List<MemberActivityTransportDto.MemberActivityMessage> lPopList() {
        List<MemberActivityTransportDto.MemberActivityMessage> result = redisTemplate.opsForList().leftPop(redisKey, batchSize);
        return result != null? result : List.of();
    }

    public List<MemberActivityTransportDto.MemberActivityMessage> lPopTopN(int size) {
        List<MemberActivityTransportDto.MemberActivityMessage> result = redisTemplate.opsForList().leftPop(redisKey, size);
        return result != null? result : List.of();
    }

    public void rPushDeadLetterEvent(List<MemberActivityTransportDto.MemberActivityMessage> postActivityEvent) {
        redisTemplate.opsForList().rightPushAll(deadLetterKey, postActivityEvent);
    }

    public List<MemberActivityTransportDto.MemberActivityMessage> lPopDeadLetterList() {
        List<MemberActivityTransportDto.MemberActivityMessage> result = redisTemplate.opsForList().rightPop(deadLetterKey, batchSize);
        return result != null? result : List.of();
    }

    public void rPushDeadletter(MemberActivityTransportDto.MemberActivityMessage message) {
        redisTemplate.opsForList().rightPush(redisDLQKey, message);
    }

    public List<MemberActivityTransportDto.MemberActivityMessage> lPopTopNDeadLetter(int size) {
        List<MemberActivityTransportDto.MemberActivityMessage> result = redisTemplate.opsForList().leftPop(redisDLQKey, size);
        return result != null? result : List.of();
    }
}
