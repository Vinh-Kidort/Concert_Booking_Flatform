package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.response.WaitingRoomStatusResponse;
import com.ticketbooking.concert_booking_platform.security.QueueTokenService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingRoomService {

    private final StringRedisTemplate redisTemplate;
    private final QueueTokenService queueTokenService;

    @Value("${app.waiting-room.queue-token-ttl-minutes}")
    private long queueTokenTtlMinutes;

    @Value("${app.waiting-room.admission-token-ttl-minutes}")
    private long admissionTokenTtlMinutes;

    private String queueKey(Long concertId) {
        return "waiting_room:queue:" + concertId;
    }

    private String admittedSetKey(Long concertId) {
        return "waiting_room:admitted:" + concertId; // dùng để check nhanh "đã từng được thả chưa" tránh double-admit
    }

    /**
     * Adds the user to the Redis sorted set with score = current timestamp,
     * so ZRANK naturally orders by join time (FIFO). Re-joining simply
     * updates the score to now (moves to the back), which is the desired
     * behavior if a user's earlier queue token expired.
     */
    public String joinQueue(Long userId, Long concertId) {
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey(concertId), String.valueOf(userId), score);
        log.info("User {} joined waiting room for concert {}", userId, concertId);
        return queueTokenService.generateQueueToken(userId, concertId, queueTokenTtlMinutes);
    }

    public WaitingRoomStatusResponse getStatus(Long userId, Long concertId) {
        // Already admitted? Check the admitted set first — avoids telling an
        // already-admitted user they're still "position 5" due to Redis
        // eventual state during the transition window.
        Boolean alreadyAdmitted = redisTemplate.opsForSet().isMember(admittedSetKey(concertId), String.valueOf(userId));
        if (Boolean.TRUE.equals(alreadyAdmitted)) {
            return WaitingRoomStatusResponse.builder()
                    .admitted(true)
                    .position(0)
                    .totalInQueue(0)
                    .build();
        }

        Long rank = redisTemplate.opsForZSet().rank(queueKey(concertId), String.valueOf(userId));
        Long total = redisTemplate.opsForZSet().zCard(queueKey(concertId));

        if (rank == null) {
            // Not in queue (never joined, or expired and was removed) — must join again.
            return WaitingRoomStatusResponse.builder()
                    .admitted(false)
                    .position(-1)
                    .totalInQueue(total != null ? total.intValue() : 0)
                    .build();
        }

        return WaitingRoomStatusResponse.builder()
                .admitted(false)
                .position(rank.intValue() + 1) // 1-indexed for display
                .totalInQueue(total != null ? total.intValue() : 0)
                .build();
    }

    /**
     * Called by the scheduler. Pops the front N users off the sorted set
     * (lowest score = earliest joiners = FIFO) and issues each an admission
     * token. Uses ZPOPMIN which is atomic in Redis — safe even if multiple
     * scheduler instances somehow ran concurrently (not expected here with
     * @Scheduled on a single app instance, but the atomicity is free and
     * correct regardless).
     */
    public void admitNextBatch(Long concertId, int batchSize) {
        Set<ZSetOperations.TypedTuple<String>> popped =
                redisTemplate.opsForZSet().popMin(queueKey(concertId), batchSize);

        redisTemplate.expire(admittedSetKey(concertId), java.time.Duration.ofHours(6));

        if (popped == null || popped.isEmpty()) {
            return;
        }

        for (ZSetOperations.TypedTuple<String> entry : popped) {
            Long userId = Long.valueOf(entry.getValue());
            redisTemplate.opsForSet().add(admittedSetKey(concertId), String.valueOf(userId));
            log.info("Admitted user {} for concert {}", userId, concertId);
        }
    }

    public String issueAdmissionTokenIfEligible(Long userId, Long concertId) {
        Boolean admitted = redisTemplate.opsForSet().isMember(admittedSetKey(concertId), String.valueOf(userId));
        if (!Boolean.TRUE.equals(admitted)) {
            throw new IllegalStateException("User has not been admitted from the waiting room yet");
        }
        return queueTokenService.generateAdmissionToken(userId, concertId, admissionTokenTtlMinutes);
    }
}