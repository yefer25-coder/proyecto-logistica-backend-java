package com.apexvision.optimizer.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouteRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation_WhenValidRequest() {
        // GIVEN: Valid request
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, null);
        LocationDto loc2 = new LocationDto(2L, 4.602, -74.072, null);
        RouteRequest request = new RouteRequest("fleet-001", Arrays.asList(loc1, loc2));

        // WHEN
        Set<ConstraintViolation<RouteRequest>> violations = validator.validate(request);

        // THEN
        assertTrue(violations.isEmpty(), "Valid request should not have violations");
    }

    @Test
    void shouldFailValidation_WhenLocationListIsEmpty() {
        // GIVEN: Empty location list
        RouteRequest request = new RouteRequest("fleet-002", Collections.emptyList());

        // WHEN
        Set<ConstraintViolation<RouteRequest>> violations = validator.validate(request);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("locations")));
    }

    @Test
    void shouldFailValidation_WhenLocationHasInvalidCoordinates() {
        // GIVEN: Location with invalid latitude
        LocationDto invalidLoc = new LocationDto(1L, 200.0, -74.076, null);
        RouteRequest request = new RouteRequest("fleet-003", Collections.singletonList(invalidLoc));

        // WHEN: Cascading validation should catch invalid location
        Set<ConstraintViolation<RouteRequest>> violations = validator.validate(request);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().contains("locations")));
    }

    @Test
    void shouldAllowNullFleetId() {
        // GIVEN: Request with null fleetId (optional field)
        LocationDto loc1 = new LocationDto(1L, 4.598, -74.076, null);
        RouteRequest request = new RouteRequest(null, Collections.singletonList(loc1));

        // WHEN
        Set<ConstraintViolation<RouteRequest>> violations = validator.validate(request);

        // THEN: FleetId is not required, only locations are validated
        assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("fleetId")),
                "FleetId should be optional");
    }
}
