package com.ticketbooking.concert_booking_platform.integration;

import com.ticketbooking.concert_booking_platform.dto.request.BookingItemRequest;
import com.ticketbooking.concert_booking_platform.dto.request.CreateBookingRequest;
import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.enums.UserRole;
import com.ticketbooking.concert_booking_platform.exception.InsufficientTicketException;
import com.ticketbooking.concert_booking_platform.repository.*;
import com.ticketbooking.concert_booking_platform.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class BookingConcurrencyIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private ConcertRepository concertRepository;
    @Autowired private TicketCategoryRepository ticketCategoryRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingItemRepository bookingItemRepository;

    private Long concertId;
    private Long ticketCategoryId;
    private static final int INITIAL_STOCK = 10;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("concert_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @BeforeEach
    void setUp() {
        Concert concert = concertRepository.save(Concert.builder()
                .title("Flash Sale Concert Test " + UUID.randomUUID())
                .venue("National Stadium")
                .eventDate(OffsetDateTime.now().plusDays(30))
                .status(ConcertStatus.ON_SALE)
                .build());
        concertId = concert.getId();

        TicketCategory category = ticketCategoryRepository.save(TicketCategory.builder()
                .concert(concert)
                .name("VIP Test")
                .price(new BigDecimal("500.00"))
                .totalQuantity(INITIAL_STOCK)
                .availableQuantity(INITIAL_STOCK)
                .build());
        ticketCategoryId = category.getId();
    }

    @AfterEach
    void tearDown() {
        bookingItemRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void concurrentBookingRequests_neverOversell_evenWhenDemandExceedsStock() throws InterruptedException {
        int numberOfRequests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfRequests; i++) {
            Long userId = userRepository.save(User.builder()
                    .email("testuser" + UUID.randomUUID() + "@test.com")
                    .fullName("User " + i)
                    .passwordHash("hash")
                    .role(UserRole.CUSTOMER)
                    .build()).getId();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateBookingRequest request = new CreateBookingRequest();
                    request.setConcertId(concertId);
                    request.setIdempotencyKey(UUID.randomUUID().toString());
                    BookingItemRequest item = new BookingItemRequest();
                    item.setTicketCategoryId(ticketCategoryId);
                    item.setQuantity(1);
                    request.setItems(List.of(item));

                    bookingService.createBooking(userId, request);
                    successCount.incrementAndGet();
                } catch (InsufficientTicketException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        TicketCategory finalCategory = ticketCategoryRepository.findById(ticketCategoryId).orElseThrow();

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(failureCount.get()).isEqualTo(numberOfRequests - INITIAL_STOCK);
        assertThat(finalCategory.getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    void duplicateIdempotencyKey_concurrentRetries_onlyCreateOneBooking() throws InterruptedException {
        Long userId = userRepository.save(User.builder()
                .email("retry-user-" + UUID.randomUUID() + "@test.com")
                .fullName("Retry User")
                .passwordHash("hash")
                .role(UserRole.CUSTOMER).build()).getId();

        String sharedIdempotencyKey = UUID.randomUUID().toString();
        int numberOfRetries = 10;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfRetries);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < numberOfRetries; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateBookingRequest request = new CreateBookingRequest();
                    request.setConcertId(concertId);
                    request.setIdempotencyKey(sharedIdempotencyKey);
                    BookingItemRequest item = new BookingItemRequest();
                    item.setTicketCategoryId(ticketCategoryId);
                    item.setQuantity(1);
                    request.setItems(List.of(item));

                    bookingService.createBooking(userId, request);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.err.println(
                            "Thread " + Thread.currentThread().getName()
                                    + " failed: "
                                    + e.getClass().getName()
                                    + " - "
                                    + e.getMessage()
                    );
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long bookingCount = bookingRepository.findAll().stream()
                .filter(b -> sharedIdempotencyKey.equals(b.getIdempotencyKey()))
                .count();

        assertThat(bookingCount).isEqualTo(1);
        assertThat(errors.get()).isZero();

        TicketCategory finalCategory = ticketCategoryRepository.findById(ticketCategoryId).orElseThrow();
        assertThat(finalCategory.getAvailableQuantity()).isEqualTo(INITIAL_STOCK - 1);
    }
}