package com.busbooking.service;

import com.busbooking.model.User;

/** Sends account emails. Swapped for a real SMTP implementation in the email feature (phase 4). */
public interface AccountMailer {

    void sendVerificationLink(User user, String link);
}
