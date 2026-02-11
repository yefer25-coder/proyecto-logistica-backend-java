package com.apexvision.optimizer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200_WhenValidRequest() throws Exception {
        // GIVEN: Valid request with 3 locations in Colombia
        String request = """
                {
                  "fleetId": "test-fleet-001",
                  "locations": [
                    {"id": 1, "latitude": 4.598120, "longitude": -74.076037},
                    {"id": 2, "latitude": 4.601968, "longitude": -74.072089},
                    {"id": 3, "latitude": 4.610350, "longitude": -74.080900}
                  ]
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceKm").exists())
                .andExpect(jsonPath("$.totalDistanceKm").isNumber())
                .andExpect(jsonPath("$.optimizedOrder").isArray())
                .andExpect(jsonPath("$.optimizedOrder.length()").value(3))
                .andExpect(jsonPath("$.optimizedOrder[0].sequenceNumber").value(0))
                .andExpect(jsonPath("$.optimizedOrder[1].sequenceNumber").value(1))
                .andExpect(jsonPath("$.optimizedOrder[2].sequenceNumber").value(2));
    }

    @Test
    void shouldReturn400_WhenInvalidLatitude() throws Exception {
        // GIVEN: Invalid latitude (outside -90 to 90 range)
        String request = """
                {
                  "fleetId": "test-fleet-002",
                  "locations": [
                    {"id": 1, "latitude": 200.0, "longitude": -74.076037}
                  ]
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void shouldReturn400_WhenInvalidLongitude() throws Exception {
        // GIVEN: Invalid longitude (outside -180 to 180 range)
        String request = """
                {
                  "fleetId": "test-fleet-003",
                  "locations": [
                    {"id": 1, "latitude": 4.598, "longitude": -200.0}
                  ]
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void shouldReturn400_WhenEmptyLocationList() throws Exception {
        // GIVEN: Empty locations array
        String request = """
                {
                  "fleetId": "test-fleet-004",
                  "locations": []
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void shouldReturn400_WhenMalformedJSON() throws Exception {
        // GIVEN: Malformed JSON (missing closing brace)
        String request = """
                {
                  "fleetId": "test-fleet-005",
                  "locations": [
                    {"id": 1, "latitude": 4.598, "longitude": -74.076
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_WhenNullLocationId() throws Exception {
        // GIVEN: Location with null ID
        String request = """
                {
                  "fleetId": "test-fleet-006",
                  "locations": [
                    {"id": null, "latitude": 4.598, "longitude": -74.076}
                  ]
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleTwoLocations_Successfully() throws Exception {
        // GIVEN: Minimum viable request (2 locations)
        String request = """
                {
                  "fleetId": "test-fleet-007",
                  "locations": [
                    {"id": 1, "latitude": 4.598, "longitude": -74.076},
                    {"id": 2, "latitude": 4.602, "longitude": -74.072}
                  ]
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceKm").exists())
                .andExpect(jsonPath("$.optimizedOrder.length()").value(2));
    }
}
