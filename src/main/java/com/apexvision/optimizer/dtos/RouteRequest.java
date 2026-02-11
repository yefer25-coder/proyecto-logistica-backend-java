package com.apexvision.optimizer.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteRequest {
    private String fleetId;

    @NotEmpty(message = "The list of locations cannot be empty")
    @jakarta.validation.Valid
    private List<LocationDto> locations;
}