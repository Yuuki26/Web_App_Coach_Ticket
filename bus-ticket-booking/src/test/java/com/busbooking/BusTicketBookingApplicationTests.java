package com.busbooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2") // tests use the in-memory database, not MySQL
class BusTicketBookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
