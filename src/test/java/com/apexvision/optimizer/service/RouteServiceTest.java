package com.apexvision.optimizer.service;

import com.apexvision.optimizer.dtos.LocationDto;
import com.apexvision.optimizer.dtos.RouteRequest;
import com.apexvision.optimizer.dtos.RouteResponse;
import com.apexvision.optimizer.service.graphhopper.GraphHopperService;
import com.apexvision.optimizer.strategy.OptimizationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private OptimizationStrategy strategy;

    @Mock
    private GraphHopperService graphHopperService;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(strategy, graphHopperService);
    }

    @Test
    void shouldOptimizeRoute_WhenJspritSucceeds() {
        // GIVEN: Valid request wit 3 locations
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, null);
        LocationDto loc2 = new LocationDto(2L, 4.602, -74.072, null);
        LocationDto loc3 = new LocationDto(3L, 4.610, -74.080, null);
        List<LocationDto> locations = Arrays.asList(loc1, loc2, loc3);
        RouteRequest request = new RouteRequest("fleet-001", locations);

        // Strategy returns optimized order (e.g., 1 -> 3 -> 2)
        LocationDto optLoc1 = new LocationDto(1L, 4.598, -74.076, 0);
        LocationDto optLoc3 = new LocationDto(3L, 4.610, -74.080, 1);
        LocationDto optLoc2 = new LocationDto(2L, 4.602, -74.072, 2);
        List<LocationDto> optimized = Arrays.asList(optLoc1, optLoc3, optLoc2);

        when(strategy.calculateOptimalRoute(locations)).thenReturn(optimized);
        when(graphHopperService.getRoadDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0); // 5 km per segment

        // WHEN
        RouteResponse response = routeService.optimizeRoute(request);

        // THEN
        assertNotNull(response);
        assertEquals(3, response.getOptimizedOrder().size());
        assertEquals(10.0, response.getTotalDistanceKm()); // 5km + 5km = 10km
        assertEquals(0, response.getOptimizedOrder().get(0).getSequenceNumber());
        assertEquals(1, response.getOptimizedOrder().get(1).getSequenceNumber());
    }

    @Test
    void shouldFallbackToOriginalOrder_WhenJspritFails() {
        // GIVEN: Strategy throws exception (simulating Jsprit failure)
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, null);
        LocationDto loc2 = new LocationDto(2L, 4.602, -74.072, null);
        List<LocationDto> locations = Arrays.asList(loc1, loc2);
        RouteRequest request = new RouteRequest("fleet-002", locations);

        when(strategy.calculateOptimalRoute(locations))
                .thenThrow(new RuntimeException("Jsprit optimization failed"));
        when(graphHopperService.getRoadDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(3.0);

        // WHEN
        RouteResponse response = routeService.optimizeRoute(request);

        // THEN: Should return original order as fallback
        assertNotNull(response);
        assertEquals(2, response.getOptimizedOrder().size());
        assertEquals(1L, response.getOptimizedOrder().get(0).getId());
        assertEquals(2L, response.getOptimizedOrder().get(1).getId());
        assertEquals(0, response.getOptimizedOrder().get(0).getSequenceNumber());
        assertEquals(1, response.getOptimizedOrder().get(1).getSequenceNumber());
        assertEquals(3.0, response.getTotalDistanceKm());
    }

    @Test
    void shouldUseHaversineFallback_WhenGraphHopperFails() {
        // GIVEN: GraphHopper fails, should fall back to Haversine
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, 0);
        LocationDto loc2 = new LocationDto(2L, 4.602, -74.072, 1);
        List<LocationDto> locations = Arrays.asList(loc1, loc2);
        RouteRequest request = new RouteRequest("fleet-003", locations);

        when(strategy.calculateOptimalRoute(locations)).thenReturn(locations);
        when(graphHopperService.getRoadDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("GraphHopper server unavailable"));

        // WHEN
        RouteResponse response = routeService.optimizeRoute(request);

        // THEN: Should use Haversine formula as fallback
        assertNotNull(response);
        assertEquals(2, response.getOptimizedOrder().size());
        assertTrue(response.getTotalDistanceKm() > 0, "Distance should be calculated using Haversine");
        // Haversine distance between these coords is approximately 0.5 km
        assertTrue(response.getTotalDistanceKm() < 1.0, "Haversine distance should be less than 1km");
    }

    @Test
    void shouldReturnZeroDistance_WhenSingleLocation() {
        // GIVEN: Only one location
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, null);
        List<LocationDto> locations = Collections.singletonList(loc1);
        RouteRequest request = new RouteRequest("fleet-004", locations);

        when(strategy.calculateOptimalRoute(locations)).thenReturn(locations);

        // WHEN
        RouteResponse response = routeService.optimizeRoute(request);

        // THEN
        assertNotNull(response);
        assertEquals(1, response.getOptimizedOrder().size());
        assertEquals(0.0, response.getTotalDistanceKm());
    }

    @Test
    void shouldHandleEmptyList_Gracefully() {
        // GIVEN: Empty location list
        List<LocationDto> locations = Collections.emptyList();
        RouteRequest request = new RouteRequest("fleet-005", locations);

        when(strategy.calculateOptimalRoute(locations)).thenReturn(locations);

        // WHEN
        RouteResponse response = routeService.optimizeRoute(request);

        // THEN
        assertNotNull(response);
        assertTrue(response.getOptimizedOrder().isEmpty());
        assertEquals(0.0, response.getTotalDistanceKm());
    }
}