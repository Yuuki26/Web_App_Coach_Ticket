package com.busbooking.config;

import com.busbooking.model.Role;
import com.busbooking.model.User;
import com.busbooking.model.UserStatus;
import com.busbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates one verified account per role, using the test credentials the project brief asks to
 * hand to the graders. These are public demo logins, not secrets. Accounts that already exist are
 * left alone. Turn off with {@code app.seed.demo-users=false} on a real production system.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed.demo-users", havingValue = "true", matchIfMissing = true)
public class DemoUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private record Demo(String email, String password, String fullName, String phone, Role role) {}

    private static final List<Demo> ACCOUNTS = List.of(
            new Demo("admin@test.com", "Admin123!", "Alex Morgan", "+1 555 0101", Role.ADMIN),
            new Demo("manager@test.com", "Manager123!", "Taylor Brooks", "+1 555 0102", Role.MANAGER),
            new Demo("driver@test.com", "Driver123!", "Chris Walker", "+1 555 0103", Role.DRIVER),
            new Demo("customer@test.com", "Customer123!", "Jordan Lee", "+1 555 0104", Role.CUSTOMER));

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int added = 0;
        for (Demo d : ACCOUNTS) {
            if (users.existsByEmail(d.email())) continue;
            User user = new User(d.email(), passwordEncoder.encode(d.password()), d.fullName(), d.phone(),
                    d.role(), UserStatus.ACTIVE);
            user.markEmailVerified();
            users.save(user);
            added++;
        }
        if (added > 0) log.info("Added {} demo account(s)", added);
    }
}
