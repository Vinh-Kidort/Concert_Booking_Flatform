package com.ticketbooking.concert_booking_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ConcertBookingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConcertBookingPlatformApplication.class, args);
	}

}
