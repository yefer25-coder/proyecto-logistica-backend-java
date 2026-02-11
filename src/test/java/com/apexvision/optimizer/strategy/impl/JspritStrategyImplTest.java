package com.apexvision.optimizer.strategy.impl;

import com.apexvision.optimizer.dtos.LocationDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JspritStrategyImpl.
 * Note: Tests that require GraphHopper routing are in integration tests.
 */
class JspritStrategyImplTest {

    @Test
    void shouldReturnSameLocation_WhenSingleLocation() {
        // GIVEN: Only one location (no routing needed)
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, null);
        List<LocationDto> locations = Collections.singletonList(loc1);

        // Note: GraphHopper not needed for single location
        JspritStrategyImpl strategy = new JspritStrategyImpl(null);

        // WHEN
        List<LocationDto> result = strategy.calculateOptimalRoute(locations);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void shouldReturnEmptyList_WhenEmptyInput() {
        // GIVEN: Empty list
        List<LocationDto> locations = Collections.emptyList();

        JspritStrategyImpl strategy = new JspritStrategyImpl(null);

        // WHEN
        List<LocationDto> result = strategy.calculateOptimalRoute(locations);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Note: Tests for sequence number assignment and multiple locations
    // require GraphHopper initialization. These are covered in integration tests.
}
