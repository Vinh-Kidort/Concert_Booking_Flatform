package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.response.WaitingRoomStatusResponse;
import com.ticketbooking.concert_booking_platform.security.QueueTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(
        classes = {
                WaitingRoomService.class,
                QueueTokenService.class,
                RedisAutoConfiguration.class
        },
        properties = {
                "app.waiting-room.queue-token-ttl-minutes=30",
                "app.waiting-room.admission-token-ttl-minutes=10",
                "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250655368566D5971"
        }
)
class WaitingRoomServiceTest {

    // 👉 CHỈ CẦN DUY NHẤT REDIS CONTAINER CHO TEST NÀY:
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private WaitingRoomService waitingRoomService;

    @Test
    void joinQueue_thenStatus_returnsCorrectPosition() {
        waitingRoomService.joinQueue(1L, 100L);
        waitingRoomService.joinQueue(2L, 100L);
        waitingRoomService.joinQueue(3L, 100L);

        WaitingRoomStatusResponse status = waitingRoomService.getStatus(3L, 100L);

        assertThat(status.isAdmitted()).isFalse();
        assertThat(status.getPosition()).isEqualTo(3);
        assertThat(status.getTotalInQueue()).isEqualTo(3);
    }

    @Test
    void admitNextBatch_movesUsersFromQueueToAdmittedSet() {
        waitingRoomService.joinQueue(10L, 200L);
        waitingRoomService.joinQueue(11L, 200L);
        waitingRoomService.joinQueue(12L, 200L);

        waitingRoomService.admitNextBatch(200L, 2);

        WaitingRoomStatusResponse admitted = waitingRoomService.getStatus(10L, 200L);
        WaitingRoomStatusResponse stillWaiting = waitingRoomService.getStatus(12L, 200L);

        assertThat(admitted.isAdmitted()).isTrue();
        assertThat(stillWaiting.isAdmitted()).isFalse();
        assertThat(stillWaiting.getPosition()).isEqualTo(1);
    }

    @Test
    void issueAdmissionTokenIfEligible_notYetAdmitted_throwsIllegalState() {
        waitingRoomService.joinQueue(20L, 300L);

        assertThatThrownBy(() -> waitingRoomService.issueAdmissionTokenIfEligible(20L, 300L))
                .isInstanceOf(IllegalStateException.class);
    }
}