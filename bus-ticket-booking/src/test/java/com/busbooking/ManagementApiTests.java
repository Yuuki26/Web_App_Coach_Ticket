package com.busbooking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Manager and admin endpoints: roles, paging, business rules, cascades and the audit trail. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ManagementApiTests {

    private static final AtomicInteger SEQ = new AtomicInteger(100);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired MockMvc mvc;

    private String manager;
    private String admin;

    @BeforeEach
    void logIn() throws Exception {
        manager = token("manager@test.com", "Manager123!");
        admin = token("admin@test.com", "Admin123!");
    }

    // ---------- helpers ----------

    private String token(String email, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.accessToken");
    }

    private ResultActions call(HttpMethod method, String url, String auth, String body) throws Exception {
        var req = request(method, url).header("Authorization", auth);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req);
    }

    private String body(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** A unique, valid plate like 59Z-123.45. */
    private static String plate() {
        int n = SEQ.incrementAndGet();
        return "59Z-%03d.%02d".formatted(n % 1000, n % 100);
    }

    private long newCoach(int rows) throws Exception {
        String json = body(call(HttpMethod.POST, "/api/v1/manage/coaches", manager,
                "{\"plateNumber\":\"" + plate() + "\",\"operatorId\":1,\"type\":\"PREMIUM\",\"seatRows\":" + rows
                        + ",\"manufactureYear\":2024}").andExpect(status().isCreated()));
        return id(json);
    }

    private long newRoute() throws Exception {
        int n = SEQ.incrementAndGet();
        String json = body(call(HttpMethod.POST, "/api/v1/manage/routes", manager, """
                {"origin":"TP. Hồ Chí Minh","destination":"Thử nghiệm %d","originStation":"Bến xe Miền Tây",
                 "destinationStation":"Bến xe Thử nghiệm","distanceKm":120,"durationMinutes":180,
                 "fromPrice":150000,"popular":false}""".formatted(n)).andExpect(status().isCreated()));
        return id(json);
    }

    private ResultActions createTrip(long routeId, long coachId, LocalDateTime dep, LocalDateTime arr) throws Exception {
        return call(HttpMethod.POST, "/api/v1/manage/trips", manager,
                "{\"routeId\":" + routeId + ",\"coachId\":" + coachId + ",\"departureTime\":\"" + dep.format(ISO)
                        + "\",\"arrivalTime\":\"" + arr.format(ISO) + "\",\"price\":200000}");
    }

    private String book(long tripId, String seat, String promo) throws Exception {
        return body(mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(
                "{\"tripId\":" + tripId + ",\"passengerName\":\"Lê Văn Test\",\"email\":\"rider@example.com\","
                        + "\"seats\":[\"" + seat + "\"]" + (promo == null ? "" : ",\"promoCode\":\"" + promo + "\"") + "}")));
    }

    // ---------- roles and paging ----------

    @Test
    void onlyManagersAndAdminsCanManage() throws Exception {
        call(HttpMethod.GET, "/api/v1/manage/routes", token("customer@test.com", "Customer123!"), null)
                .andExpect(status().isForbidden());
        call(HttpMethod.GET, "/api/v1/manage/routes", token("driver@test.com", "Driver123!"), null)
                .andExpect(status().isForbidden());
        call(HttpMethod.GET, "/api/v1/manage/routes", manager, null).andExpect(status().isOk());
        call(HttpMethod.GET, "/api/v1/manage/routes", admin, null).andExpect(status().isOk());
        // User management is admin-only.
        call(HttpMethod.GET, "/api/v1/admin/users", manager, null).andExpect(status().isForbidden());
    }

    @Test
    void listsArePagedSortedAndFiltered() throws Exception {
        String page2 = body(call(HttpMethod.GET,
                "/api/v1/manage/routes?origin=TP. Hồ Chí Minh&page=2&size=10&sort=fromPrice,desc&sort=destination", manager, null)
                .andExpect(status().isOk()));
        int total = JsonPath.read(page2, "$.totalElements");
        assertTrue(total >= 12);
        assertEquals(2, (int) JsonPath.read(page2, "$.page"));
        assertEquals(11, ((Number) JsonPath.read(page2, "$.from")).intValue());
        assertEquals(List.of("fromPrice,desc", "destination,asc", "id,asc"), JsonPath.read(page2, "$.sort"));

        String expensive = body(call(HttpMethod.GET, "/api/v1/manage/routes?minPrice=280000&active=true&size=50", manager, null));
        List<Integer> prices = JsonPath.read(expensive, "$.content[*].fromPrice");
        assertTrue(prices.stream().allMatch(p -> p >= 280000));

        call(HttpMethod.GET, "/api/v1/manage/routes?size=7", manager, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        call(HttpMethod.GET, "/api/v1/manage/routes?sort=secret", manager, null)
                .andExpect(status().isBadRequest());
    }

    // ---------- routes ----------

    @Test
    void routeRules() throws Exception {
        call(HttpMethod.POST, "/api/v1/manage/routes", manager, """
                {"origin":"Đà Lạt","destination":"đà lạt","originStation":"A","destinationStation":"B",
                 "distanceKm":10,"durationMinutes":30,"fromPrice":50000}""")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        call(HttpMethod.POST, "/api/v1/manage/routes", manager, """
                {"origin":"TP. Hồ Chí Minh","destination":"Đà Lạt","originStation":"A","destinationStation":"B",
                 "distanceKm":300,"durationMinutes":420,"fromPrice":280000}""")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE"));

        // Route 1 (HCMC → Đà Lạt) has upcoming trips, so it can't be removed.
        call(HttpMethod.DELETE, "/api/v1/manage/routes/1", manager, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));

        long route = newRoute();
        call(HttpMethod.GET, "/api/v1/manage/routes/" + route, manager, null)
                .andExpect(jsonPath("$.audit.createdBy").value("manager@test.com"));
        call(HttpMethod.DELETE, "/api/v1/manage/routes/" + route, manager, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        call(HttpMethod.POST, "/api/v1/manage/routes/" + route + "/restore", manager, null)
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---------- coaches ----------

    @Test
    void coachRules() throws Exception {
        call(HttpMethod.POST, "/api/v1/manage/coaches", manager,
                "{\"plateNumber\":\"ABC\",\"operatorId\":1,\"type\":\"STANDARD\",\"seatRows\":20,\"manufactureYear\":2024}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'plateNumber')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'seatRows')]").exists());

        // Coach 1 runs upcoming sample trips.
        call(HttpMethod.DELETE, "/api/v1/manage/coaches/1", manager, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));

        long coach = newCoach(6);
        call(HttpMethod.DELETE, "/api/v1/manage/coaches/" + coach, manager, null)
                .andExpect(jsonPath("$.status").value("RETIRED"));
        call(HttpMethod.POST, "/api/v1/manage/coaches/" + coach + "/restore", manager, null)
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // An operator with coaches in service can't be removed.
        call(HttpMethod.DELETE, "/api/v1/manage/operators/1", manager, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));
    }

    // ---------- trips ----------

    @Test
    void aCoachCannotBeOnTwoTripsAtOnce() throws Exception {
        long route = newRoute();
        long coach = newCoach(6);
        LocalDateTime dep = LocalDateTime.now().plusDays(20).withHour(8).withMinute(0).withSecond(0).withNano(0);

        createTrip(route, coach, dep, dep.plusHours(3)).andExpect(status().isCreated());
        createTrip(route, coach, dep.plusHours(2), dep.plusHours(5))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
        createTrip(route, coach, dep.plusHours(3), dep.plusHours(6)).andExpect(status().isCreated()); // back-to-back is fine

        createTrip(route, coach, dep.plusDays(1), dep.plusDays(1).minusHours(1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        createTrip(route, coach, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soldSeatsMustFitANewCoachAndCancellingCascadesToBookings() throws Exception {
        long route = newRoute();
        long bigCoach = newCoach(9);
        long smallCoach = newCoach(4);
        LocalDateTime dep = LocalDateTime.now().plusDays(21).withHour(9).withMinute(0).withSecond(0).withNano(0);
        long trip = id(body(createTrip(route, bigCoach, dep, dep.plusHours(3)).andExpect(status().isCreated())));

        String booking = book(trip, "8A", null);
        String code = JsonPath.read(booking, "$.bookingCode");

        // The small coach has only 4 rows, so seat 8A would disappear.
        call(HttpMethod.PUT, "/api/v1/manage/trips/" + trip, manager,
                "{\"routeId\":" + route + ",\"coachId\":" + smallCoach + ",\"departureTime\":\"" + dep.format(ISO)
                        + "\",\"arrivalTime\":\"" + dep.plusHours(3).format(ISO) + "\",\"price\":200000}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));

        call(HttpMethod.DELETE, "/api/v1/manage/trips/" + trip + "?reason=Bão", manager, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBookings").value(1))
                .andExpect(jsonPath("$.trip.status").value("CANCELLED"));

        mvc.perform(post("/api/v1/bookings/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingCode\":\"" + code + "\",\"email\":\"rider@example.com\"}"))
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Nobody can book a cancelled trip.
        mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"tripId\":" + trip + ",\"passengerName\":\"A\",\"email\":\"a@example.com\",\"seats\":[\"1A\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_CANCELLED"));

        call(HttpMethod.GET, "/api/v1/admin/audit-logs?entityType=Trip&entityId=" + trip + "&action=CANCEL", admin, null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actor").value("manager@test.com"))
                .andExpect(jsonPath("$.content[0].details").value("1 booking(s) cancelled. Reason: Bão"));
    }

    // ---------- promo codes ----------

    @Test
    void promoCodeUsageLimitIsEnforced() throws Exception {
        String code = "ONCE" + SEQ.incrementAndGet();
        long promo = id(body(call(HttpMethod.POST, "/api/v1/manage/promo-codes", manager,
                "{\"code\":\"" + code.toLowerCase() + "\",\"description\":\"Single use\",\"discountPercent\":50,\"usageLimit\":1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))));

        long route = newRoute();
        long coach = newCoach(9);
        LocalDateTime dep = LocalDateTime.now().plusDays(22).withHour(10).withMinute(0).withSecond(0).withNano(0);
        long trip = id(body(createTrip(route, coach, dep, dep.plusHours(3))));

        String first = book(trip, "5A", code);
        assertEquals(100000, ((Number) JsonPath.read(first, "$.discount")).intValue()); // 50% of 200.000 ₫

        String second = book(trip, "5B", code);
        assertEquals(0, ((Number) JsonPath.read(second, "$.discount")).intValue());

        call(HttpMethod.GET, "/api/v1/manage/promo-codes/" + promo, manager, null)
                .andExpect(jsonPath("$.usedCount").value(1));
        call(HttpMethod.PUT, "/api/v1/manage/promo-codes/" + promo, manager,
                "{\"code\":\"RENAMED1\",\"description\":\"x\",\"discountPercent\":50,\"usageLimit\":1}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));

        call(HttpMethod.POST, "/api/v1/manage/promo-codes", manager,
                "{\"code\":\"BADDATES\",\"description\":\"x\",\"discountPercent\":10,\"validFrom\":\"2027-02-01\",\"validTo\":\"2027-01-01\"}")
                .andExpect(status().isBadRequest());
    }

    // ---------- admin: users ----------

    @Test
    void adminManagesUsersWithGuardRails() throws Exception {
        String email = "driver" + SEQ.incrementAndGet() + "@hcoach.example";
        long user = ((Number) JsonPath.read(body(call(HttpMethod.POST, "/api/v1/admin/users", admin,
                "{\"email\":\"" + email + "\",\"password\":\"Driver#123\",\"fullName\":\"Trần Văn Lái\",\"role\":\"DRIVER\"}")
                .andExpect(status().isCreated())), "$.user.id")).longValue();

        token(email, "Driver#123"); // created active and verified

        call(HttpMethod.PATCH, "/api/v1/admin/users/" + user + "/role", admin, "{\"role\":\"MANAGER\"}")
                .andExpect(jsonPath("$.user.role").value("MANAGER"));
        call(HttpMethod.POST, "/api/v1/admin/users/" + user + "/deactivate", admin, null)
                .andExpect(jsonPath("$.user.status").value("DEACTIVATED"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Driver#123\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        call(HttpMethod.POST, "/api/v1/admin/users/" + user + "/reactivate", admin, null)
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

        // The admin can't lock themselves out.
        String me = body(call(HttpMethod.GET, "/api/v1/users/me", admin, null));
        long adminId = ((Number) JsonPath.read(me, "$.id")).longValue();
        call(HttpMethod.POST, "/api/v1/admin/users/" + adminId + "/deactivate", admin, null)
                .andExpect(status().isConflict());
        call(HttpMethod.PATCH, "/api/v1/admin/users/" + adminId + "/role", admin, "{\"role\":\"CUSTOMER\"}")
                .andExpect(status().isConflict());

        call(HttpMethod.GET, "/api/v1/admin/users?role=MANAGER&q=" + email, admin, null)
                .andExpect(jsonPath("$.totalElements").value(1));
        call(HttpMethod.GET, "/api/v1/admin/audit-logs?entityType=User&entityId=" + user, admin, null)
                .andExpect(jsonPath("$.totalElements").value(4)); // create, role change, deactivate, reactivate
    }
}
