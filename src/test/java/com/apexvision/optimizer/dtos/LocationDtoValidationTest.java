package com.apexvision.optimizer.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LocationDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation_WhenValidLocation() {
        // GIVEN: Valid location
        LocationDto location = new LocationDto(1L, 4.598, -74.076, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations = validator.validate(location);

        // THEN
        assertTrue(violations.isEmpty(), "Valid location should not have violations");
    }

    @Test
    void shouldFailValidation_WhenLatitudeTooHigh() {
        // GIVEN: Latitude > 90
        LocationDto location = new LocationDto(1L, 100.0, -74.076, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations = validator.validate(location);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }

    @Test
    void shouldFailValidation_WhenLatitudeTooLow() {
        // GIVEN: Latitude < -90
        LocationDto location = new LocationDto(1L, -100.0, -74.076, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations = validator.validate(location);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }

    @Test
    void shouldFailValidation_WhenLongitudeTooHigh() {
        // GIVEN: Longitude > 180
        LocationDto location = new LocationDto(1L, 4.598, 200.0, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations = validator.validate(location);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }

    @Test
    void shouldFailValidation_WhenLongitudeTooLow() {
        // GIVEN: Longitude < -180
        LocationDto location = new LocationDto(1L, 4.598, -200.0, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations = validator.validate(location);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }

    @Test
    void shouldFailValidation_WhenIdIsNull() {
        // GIVEN: Null ID
        LocationDto location = new LocationDto(null, 4.598, -74.076, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations = validator.validate(location);

        // THEN
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }

    @Test
    void shouldAllowBoundaryValues_ForLatitudeLongitude() {
        // GIVEN: Boundary values (exactly -90, 90, -180, 180)
        LocationDto loc1 = new LocationDto(1L, 90.0, 180.0, null);
        LocationDto loc2 = new LocationDto(2L, -90.0, -180.0, null);

        // WHEN
        Set<ConstraintViolation<LocationDto>> violations1 = validator.validate(loc1);
        Set<ConstraintViolation<LocationDto>> violations2 = validator.validate(loc2);

        // THEN
        assertTrue(violations1.isEmpty(), "Max boundary values should be valid");
        assertTrue(violations2.isEmpty(), "Min boundary values should be valid");
    }
}
