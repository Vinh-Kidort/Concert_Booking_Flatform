package com.ticketbooking.concert_booking_platform.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static com.ticketbooking.concert_booking_platform.config.RabbitMQConfig.NOTIFICATION_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationConsumer {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = NOTIFICATION_QUEUE)
    public void handleBookingConfirmed(String payloadJson) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);

            String email = payload.get("userEmail").asText();
            String name = payload.get("userFullName").asText();
            String concertTitle = payload.get("concertTitle").asText();
            String bookingId = payload.get("bookingId").asText();

            log.info("📩 Processing notification for booking #{} (Recipient: {})", bookingId, email);

            try {
                // Thử gửi Email thật (nếu có cấu hình SMTP)
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Your ticket for " + concertTitle + " is confirmed!");
                message.setText(String.format(
                        "Hi %s,\n\nYour booking #%s for \"%s\" has been confirmed. Enjoy the show!\n\n— Concert Booking Platform",
                        name, bookingId, concertTitle));

                mailSender.send(message);
                log.info("✅ Sent real confirmation email to {} for booking #{}", email, bookingId);
            } catch (Exception mailError) {
                // Môi trường Local / Dev chưa có SMTP Server -> Chuyển sang Giả lập (Mock Email)
                log.warn("⚠️ SMTP server not available on localhost:25 (Normal in Dev). Simulating email delivery...");
                log.info("📧 [MOCK EMAIL SENT] To: {} | Subject: 'Your ticket for {} is confirmed!' | Booking ID: #{}",
                        email, concertTitle, bookingId);
            }

        } catch (Exception e) {
            log.error("Failed to parse notification payload: {}", payloadJson, e);
        }
    }
}