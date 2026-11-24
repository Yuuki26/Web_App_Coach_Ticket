package com.busbooking.controller;

import com.busbooking.dto.MessageResponse;
import com.busbooking.dto.auth.AuthRequests;
import com.busbooking.dto.auth.AuthResponse;
import com.busbooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints for getting and ending a login session. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Creates a customer account and emails a verification link. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody AuthRequests.Register req) {
        authService.register(req);
        return new MessageResponse("Account created. Check your email for a link to activate it");
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody AuthRequests.VerifyEmail req) {
        authService.verifyEmail(req.token());
        return new MessageResponse("Email verified. You can now log in");
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerification(@Valid @RequestBody AuthRequests.ResendVerification req) {
        authService.resendVerification(req.email());
        return new MessageResponse("If that account is waiting for verification, a new link has been sent");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequests.Login req) {
        return authService.login(req);
    }

    /** Swaps a refresh token for a new access token and a new refresh token. */
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody AuthRequests.Refresh req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthRequests.Logout req) {
        authService.logout(req.refreshToken());
    }
}
