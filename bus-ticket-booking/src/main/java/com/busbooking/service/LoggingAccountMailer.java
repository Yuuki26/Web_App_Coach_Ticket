package com.busbooking.service;

import com.busbooking.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development stand-in for email: writes the verification link to the application log so a
 * new account can be verified locally. Replaced by the SMTP mailer when email is added.
 */
@Component
public class LoggingAccountMailer implements AccountMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingAccountMailer.class);

    @Override
    public void sendVerificationLink(User user, String link) {
        log.info("[dev mail] Verification link for user {}: {}", user.getId(), link);
    }
}
