package com.apexvision.optimizer.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {

    @NotNull(message = "ID is mandatory")
    private Long id;

    @NotNull(message = "Latitude is mandatory")
    @Min(value = -90, message = "The latitude cannot be less than -90.")
    @Max(value = 90, message = "The latitude cannot be greater than 90.")
    private double latitude;

    @NotNull(message = "Length is required")
    @Min(value = -180, message = "The length cannot be less than -180.")
    @Max(value = 180, message = "The length cannot exceed 180.")
    private double longitude;

    private Integer sequenceNumber;
}