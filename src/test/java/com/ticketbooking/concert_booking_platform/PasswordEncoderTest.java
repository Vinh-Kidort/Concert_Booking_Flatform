package com.ticketbooking.concert_booking_platform;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderTest {

    @Test
    void generateBcryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String rawPassword = "Password123!";

        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("=========================================");
        System.out.println("RAW PASSWORD : " + rawPassword);
        System.out.println("BCRYPT HASH  : " + encodedPassword);
        System.out.println("=========================================");

        // Kiểm tra tính hợp lệ
        boolean isMatch = encoder.matches(rawPassword, encodedPassword);
        System.out.println("VERIFY MATCH : " + isMatch);
    }
}