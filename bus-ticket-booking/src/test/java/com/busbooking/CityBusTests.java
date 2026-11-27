package com.busbooking;

import com.busbooking.citybus.CityBusSnapshot;
import com.busbooking.citybus.Geo;
import com.busbooking.citybus.OsmBusParser;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HCMC city buses: the OpenStreetMap parser, and the API on the bundled snapshot. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CityBusTests {

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    /**
     * Line 99: one direction made of two road pieces (the second mapped backwards) and three stops,
     * listed out of order (A, C, B) as sometimes happens in OSM.
     */
    private static final String OVERPASS = """
            {"elements":[
              {"type":"relation","id":10,"tags":{"type":"route","route":"bus","ref":"99","network":"HCMC",
                 "name":"A - C","from":"A","to":"C","operator":"Op","opening_hours":"Mo-Su 05:00-21:00",
                 "interval":"00:10-00:15","charge":"6.000 VND","colour":"#FF0000"},
               "members":[{"type":"node","ref":1,"role":"platform"},{"type":"node","ref":3,"role":"platform"},
                          {"type":"way","ref":100,"role":""},{"type":"way","ref":101,"role":""},
                          {"type":"node","ref":2,"role":"platform_entry_only"}]},
              {"type":"relation","id":11,"tags":{"type":"route","route":"bus","ref":"7","network":"Elsewhere"},
               "members":[{"type":"way","ref":100,"role":""}]},
              {"type":"relation","id":20,"tags":{"type":"route_master","ref":"99"},
               "members":[{"type":"relation","ref":10,"role":""}]},
              {"type":"node","id":1,"lat":10.7700,"lon":106.7000,"tags":{"name":"Stop A"}},
              {"type":"node","id":2,"lat":10.7710,"lon":106.7010,"tags":{"name":"Stop B"}},
              {"type":"node","id":3,"lat":10.7730,"lon":106.7030,"tags":{"name":"Stop C"}},
              {"type":"way","id":100,"geometry":[{"lat":10.7700,"lon":106.7000},{"lat":10.7710,"lon":106.7010}]},
              {"type":"way","id":101,"geometry":[{"lat":10.7730,"lon":106.7030},{"lat":10.7720,"lon":106.7020},
                                                  {"lat":10.7710,"lon":106.7010}]}
            ]}""";

    @Test
    void parserStitchesRoadPiecesAndKeepsStopsInOrder() {
        CityBusSnapshot snap = new OsmBusParser(Set.of("HCMC")).parse(json.readTree(OVERPASS), "now");

        assertEquals(1, snap.lines().size(), "other networks are left out");
        CityBusSnapshot.Line line = snap.lines().get(0);
        assertEquals("99", line.ref());
        assertEquals(20L, line.osmMasterId());
        assertEquals("6.000 VND", line.fare());
        CityBusSnapshot.Direction d = line.directions().get(0);
        assertEquals(List.of(1L, 2L, 3L), d.stops(), "stops in order along the road");
        // A → B → C: the backwards piece was turned round, so the path ends at C.
        List<double[]> path = decode(d.polyline());
        assertEquals(10.7700, path.get(0)[0], 1e-5);
        assertEquals(10.7730, path.get(path.size() - 1)[0], 1e-5);
        int expected = (int) Math.round(Geo.meters(10.77, 106.70, 10.771, 106.701) + Geo.meters(10.771, 106.701, 10.773, 106.703));
        assertEquals(expected, d.lengthMeters(), 1);
    }

    @Test
    void listIsInLineNumberOrderAndSearchMatchesStopsWithoutAccents() throws Exception {
        mvc.perform(get("/api/v1/city-buses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(150)))
                .andExpect(jsonPath("$[0].ref").value("01"))
                .andExpect(jsonPath("$[0].openingHours").isString());

        // "ben thanh" (no accents) finds lines passing stops named "Bến Thành".
        mvc.perform(get("/api/v1/city-buses").param("q", "ben thanh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(5)));

        // An exact line number comes first.
        mvc.perform(get("/api/v1/city-buses").param("q", "19"))
                .andExpect(jsonPath("$[0].ref").value("19"));
    }

    @Test
    void lineDetailHasBothDirectionsWithPathsAndStops() throws Exception {
        String list = mvc.perform(get("/api/v1/city-buses").param("q", "01"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(list, "$[0].id");

        mvc.perform(get("/api/v1/city-buses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line.ref").value("01"))
                .andExpect(jsonPath("$.directions.length()").value(2))
                .andExpect(jsonPath("$.directions[0].polyline").isString())
                .andExpect(jsonPath("$.directions[0].stops.length()").value(greaterThan(10)))
                .andExpect(jsonPath("$.attribution").value("Bus data © OpenStreetMap contributors (ODbL)"));

        mvc.perform(get("/api/v1/city-buses/{id}", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    void stopsNearACoachStationListTheirLines() throws Exception {
        // Bến xe Miền Đông mới
        mvc.perform(get("/api/v1/city-buses/stops/near").param("lat", "10.8790").param("lng", "106.8155"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bến xe Miền Đông mới"))
                .andExpect(jsonPath("$[0].distanceMeters").value(org.hamcrest.Matchers.lessThan(500)))
                .andExpect(jsonPath("$[0].lines[*].ref", hasItem("93")));
    }

    @Test
    void onlyAdminsCanImport() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"manager@test.com\",\"password\":\"Manager123!\"}"))
                .andReturn().getResponse().getContentAsString();
        String manager = "Bearer " + JsonPath.read(login, "$.accessToken");
        mvc.perform(post("/api/v1/admin/city-buses/import").header("Authorization", manager))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/city-buses/snapshot")).andExpect(status().isUnauthorized());
    }

    private static List<double[]> decode(String s) {
        List<double[]> pts = new java.util.ArrayList<>();
        int i = 0, lat = 0, lng = 0;
        while (i < s.length()) {
            int[] vals = new int[2];
            for (int axis = 0; axis < 2; axis++) {
                int shift = 0, result = 0, b;
                do {
                    b = s.charAt(i++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                vals[axis] = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            }
            lat += vals[0];
            lng += vals[1];
            pts.add(new double[]{lat / 1e5, lng / 1e5});
        }
        assertTrue(pts.size() >= 2);
        return pts;
    }
}
