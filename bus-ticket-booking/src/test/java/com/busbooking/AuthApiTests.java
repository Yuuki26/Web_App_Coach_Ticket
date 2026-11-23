package com.busbooking;

import com.busbooking.config.DataSeeder;
import com.busbooking.dto.TripResponse;
import com.busbooking.model.User;
import com.busbooking.model.UserStatus;
import com.busbooking.repository.UserRepository;
import com.busbooking.service.AccountMailer;
import com.busbooking.service.SeatLayout;
import com.busbooking.service.TripService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Accounts, tokens and role checks, tested through the real HTTP API and security filters. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2") // tests use the in-memory database, not MySQL
class AuthApiTests {

    private static final String PASSWORD = "Secret#123";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripService tripService;
    @MockitoSpyBean AccountMailer mailer;

    // ---------- helpers ----------

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String uniqueEmail() {
        return "rider-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private void register(String email, String password) throws Exception {
        postJson("/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"fullName\":\"Riley Park\"}")
                .andExpect(status().isCreated());
    }

    /** Registers and clicks the emailed link. */
    private void registerVerified(String email) throws Exception {
        register(email, PASSWORD);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer, atLeastOnce()).sendVerificationLink(any(), link.capture());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        postJson("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}").andExpect(status().isOk());
    }

    /** Returns the raw login response JSON. */
    private String login(String email, String password) throws Exception {
        return postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String field(String json, String path) {
        return JsonPath.read(json, path);
    }

    private static String bearer(String loginJson) {
        return "Bearer " + field(loginJson, "$.accessToken");
    }

    // ---------- registration & verification ----------

    @Test
    void newAccountMustVerifyEmailBeforeLoggingIn() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer, atLeastOnce()).sendVerificationLink(any(), link.capture());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);

        postJson("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}").andExpect(status().isOk());
        postJson("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        String session = login(email.toUpperCase(), PASSWORD); // email is case-insensitive
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        postJson("/api/v1/auth/register",
                "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"Copy\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void weakPasswordAndBadEmailAreReportedPerField() throws Exception {
        postJson("/api/v1/auth/register", "{\"email\":\"not-an-email\",\"password\":\"password\",\"fullName\":\"A\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
    }

    // ---------- login ----------

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        postJson("/api/v1/auth/login", "{\"email\":\"customer@test.com\",\"password\":\"Nope#1234\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        postJson("/api/v1/auth/login", "{\"email\":\"nobody@example.com\",\"password\":\"Nope#1234\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void deactivatedAccountCannotLogIn() throws Exception {
        String email = uniqueEmail();
        registerVerified(email);
        User user = users.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.DEACTIVATED);
        users.save(user);

        postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void everyDemoRoleCanLogIn() throws Exception {
        assertEquals("ADMIN", field(login("admin@test.com", "Admin123!"), "$.user.role"));
        assertEquals("MANAGER", field(login("manager@test.com", "Manager123!"), "$.user.role"));
        assertEquals("DRIVER", field(login("driver@test.com", "Driver123!"), "$.user.role"));
        assertEquals("CUSTOMER", field(login("customer@test.com", "Customer123!"), "$.user.role"));
    }

    // ---------- tokens ----------

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/cities")).andExpect(status().isOk()); // public stays public
    }

    @Test
    void refreshTokensAreSingleUseAndReuseEndsAllSessions() throws Exception {
        String first = login("customer@test.com", "Customer123!");
        String oldRefresh = field(first, "$.refreshToken");

        String second = postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + oldRefresh + "\"}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newRefresh = field(second, "$.refreshToken");

        // Someone replays the old token: refused, and the new one is revoked too.
        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + oldRefresh + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + newRefresh + "\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        String refresh = field(login("customer@test.com", "Customer123!"), "$.refreshToken");
        postJson("/api/v1/auth/logout", "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isNoContent());
        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isUnauthorized());
    }

    // ---------- profile ----------

    @Test
    void profileCanBeEditedAndPasswordChanged() throws Exception {
        String email = uniqueEmail();
        registerVerified(email);
        String session = login(email, PASSWORD);

        mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Riley Park-Lee\",\"phone\":\"+1 555 0199\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Riley Park-Lee"))
                .andExpect(jsonPath("$.phone").value("+1 555 0199"));

        mvc.perform(put("/api/v1/users/me/password").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"Wrong#123\",\"newPassword\":\"Another#456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));

        mvc.perform(put("/api/v1/users/me/password").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Another#456\"}"))
                .andExpect(status().isNoContent());

        // Old sessions are logged out; the new password works.
        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + field(session, "$.refreshToken") + "\"}")
                .andExpect(status().isUnauthorized());
        login(email, "Another#456");
    }

    // ---------- roles ----------

    @Test
    void onlyAdminsCanListUsers() throws Exception {
        mvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(login("customer@test.com", "Customer123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(login("manager@test.com", "Manager123!"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(login("admin@test.com", "Admin123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.user.email == 'admin@test.com')]").exists());
    }

    // ---------- bookings on the account ----------

    @Test
    void bookingWhileLoggedInShowsUpInMyBookings() throws Exception {
        String email = uniqueEmail();
        registerVerified(email);
        String auth = bearer(login(email, PASSWORD));

        TripResponse trip = tripService.search(DataSeeder.HCMC, "Cần Thơ", LocalDate.now().plusDays(4)).get(0);
        var map = tripService.seatMap(trip.id());
        List<String> booked = map.bookedSeats();
        List<String> free = SeatLayout.allSeats(map.rows()).stream().filter(s -> !booked.contains(s)).limit(2).toList();

        String body = "{\"tripId\":" + trip.id() + ",\"passengerName\":\"Riley Park\",\"email\":\"" + email
                + "\",\"seats\":[\"%s\"]}";
        String code = JsonPath.read(mvc.perform(post("/api/v1/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body.formatted(free.get(0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.bookingCode");

        // A guest booking with the same email is not attached to the account.
        postJson("/api/v1/bookings", body.formatted(free.get(1))).andExpect(status().isCreated());

        String mine = mvc.perform(get("/api/v1/bookings/mine").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> codes = JsonPath.read(mine, "$[*].bookingCode");
        assertEquals(List.of(code), codes);

        mvc.perform(get("/api/v1/bookings/mine")).andExpect(status().isUnauthorized());
        assertTrue(code.startsWith("HC-"));
    }
}
